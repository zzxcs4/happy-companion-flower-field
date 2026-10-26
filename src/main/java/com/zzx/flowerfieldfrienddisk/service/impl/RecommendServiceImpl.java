package com.zzx.flowerfieldfrienddisk.service.impl;


import com.zzx.flowerfieldfrienddisk.constant.RedisConstants;
import com.zzx.flowerfieldfrienddisk.mapper.SpaceMapper;
import com.zzx.flowerfieldfrienddisk.model.entity.Space;
import com.zzx.flowerfieldfrienddisk.model.entity.UserFieldScore;
import com.zzx.flowerfieldfrienddisk.service.RecommendService;
import com.zzx.flowerfieldfrienddisk.service.SpaceService;
import com.zzx.flowerfieldfrienddisk.service.UserFieldScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基于用户基协同过滤（皮尔森相关系数）的社区空间推荐
 *
 * <p>算法步骤：
 * <ol>
 *   <li>构造 用户 → {社区空间id → 评分} 数据集</li>
 *   <li>计算目标用户与其他所有用户之间的皮尔森相关系数</li>
 *   <li>取相关性最高的用户作为"最近邻"</li>
 *   <li>推荐最近邻评分过但目标用户从未涉及的社区空间</li>
 * </ol>
 */
@Service
public class RecommendServiceImpl implements RecommendService, RedisConstants {

    private static final Logger log = LoggerFactory.getLogger(RecommendServiceImpl.class);

    @Resource
    private UserFieldScoreService userFieldScoreService;
    @Resource
    private SpaceService spaceService;
    @Autowired
    private SpaceMapper spaceMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * L1 本地缓存（Caffeine）：存储推荐社区空间 ID 列表
     */
//    private final Cache<String, List<Long>> localCache = Caffeine.newBuilder()
//            .maximumSize(1000)
//            .expireAfterWrite(10, TimeUnit.MINUTES)
//            .build();

    /** L2 分布式缓存（Redis）TTL：15 分钟 */
    private static final long REDIS_CACHE_TTL_MINUTES = 15;

    // -------------------------------------------------------------------------
    // 公共接口
    // -------------------------------------------------------------------------

    @Override
    public List<Space> recommendFields(Long userId, int topN) {
        String cacheKey = RECOMMEND_FIELDS_KEY + userId + ":" + topN;

        // ===== L1: Caffeine 本地缓存 =====
//        List<Long> cachedIds = localCache.getIfPresent(cacheKey);
//        if (cachedIds != null) {
//            log.debug("[推荐缓存] L1 命中, userId={}", userId);
//            return buildSpacesFromIds(cachedIds);
//        }

        // ===== L2: Redis 分布式缓存 =====
        List<Long> redisIds = getFromRedis(cacheKey);
        if (redisIds != null) {
            log.debug("[推荐缓存] L2 命中, userId={}", userId);
            //localCache.put(cacheKey, redisIds); // 回填 L1
            return buildSpacesFromIds(redisIds);
        }

        // ===== Cache Miss: 执行协同过滤计算 =====
        log.debug("[推荐缓存] Miss, 执行计算, userId={}", userId);
        List<Long> recommendSpaceIds = computeRecommendation(userId, topN);

        if (recommendSpaceIds != null && !recommendSpaceIds.isEmpty()) {
            // 写入双层缓存
            //localCache.put(cacheKey, recommendSpaceIds);
            putToRedis(cacheKey, recommendSpaceIds);
        }

        return buildSpacesFromIds(recommendSpaceIds);
    }

    /**
     * 根据社区空间 ID 列表查询 Space 实体并保持顺序
     */
    private List<Space> buildSpacesFromIds(List<Long> spaceIds) {
        if (spaceIds == null || spaceIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Space> spaces = spaceService.listByIds(spaceIds);
        Map<Long, Space> spaceMap = spaces.stream()
                .collect(Collectors.toMap(Space::getId, s -> s));
        return spaceIds.stream()
                .filter(spaceMap::containsKey)
                .map(spaceMap::get)
                .collect(Collectors.toList());
    }

    /**
     * 核心协同过滤计算（无缓存）
     */
    private List<Long> computeRecommendation(Long userId, int topN) {
        // 1. 构造数据集：所有 (userId, spaceId, score) 记录
        List<UserFieldScore> allScores = userFieldScoreService.list();
        if (allScores == null || allScores.isEmpty()) {
            List<Space> hotSpaces = fallbackHotSpaces(topN);
            return hotSpaces.stream().map(Space::getId).collect(Collectors.toList());
        }

        // 2. 转换为 Map<userId, Map<spaceId, score>>
        Map<Long, Map<Long, Double>> dataset = buildDataset(allScores);

        // 如果目标用户在数据集中没有任何评分记录，返回全局热门社区
        if (!dataset.containsKey(userId)) {
            List<Space> hotSpaces = fallbackHotSpaces(topN);
            return hotSpaces.stream().map(Space::getId).collect(Collectors.toList());
        }

        // 3. 计算皮尔森系数，选出相关性最强的最近邻用户
        Long nearestNeighborId = findNearestNeighbor(userId, dataset);
        if (nearestNeighborId == null) {
            List<Space> hotSpaces = fallbackHotSpaces(topN);
            return hotSpaces.stream().map(Space::getId).collect(Collectors.toList());
        }

        // 4. 取出 "最近邻评分过 但目标用户没涉及过" 的社区空间id
        Set<Long> targetSpaceIds = dataset.get(userId).keySet();
        Map<Long, Double> neighborScores = dataset.get(nearestNeighborId);

        return neighborScores.entrySet().stream()
                .filter(e -> !targetSpaceIds.contains(e.getKey()))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // ===== Redis 缓存操作 =====

    private List<Long> getFromRedis(String key) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null && !json.isEmpty()) {
                // JSON 格式: [1,2,3]
                return Arrays.stream(json.replaceAll("[\\[\\]\\s]", "").split(","))
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("[推荐缓存] Redis 读取失败: {}", e.getMessage());
        }
        return null;
    }

    private void putToRedis(String key, List<Long> spaceIds) {
        try {
            String json = spaceIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",", "[", "]"));
            stringRedisTemplate.opsForValue().set(key, json, REDIS_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("[推荐缓存] Redis 写入失败: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 核心算法：皮尔森相关系数
    // -------------------------------------------------------------------------

    private Long findNearestNeighbor(Long targetUserId,
                                     Map<Long, Map<Long, Double>> dataset) {
        Map<Long, Double> targetRatings = dataset.get(targetUserId);

        TreeMap<Double, Long> similarityMap = new TreeMap<>();

        for (Map.Entry<Long, Map<Long, Double>> entry : dataset.entrySet()) {
            Long otherUserId = entry.getKey();
            if (otherUserId.equals(targetUserId)) {
                continue;
            }
            Map<Long, Double> otherRatings = entry.getValue();

            double pearson = calcPearson(targetRatings, otherRatings);
            similarityMap.put(pearson, otherUserId);
        }

        if (similarityMap.isEmpty()) {
            return null;
        }

        double maxSimilarity = similarityMap.lastKey();
        if (maxSimilarity <= 0) {
            return null;
        }
        return similarityMap.lastEntry().getValue();
    }

    private double calcPearson(Map<Long, Double> ratingsA,
                               Map<Long, Double> ratingsB) {
        Set<Long> common = new HashSet<>(ratingsA.keySet());
        common.retainAll(ratingsB.keySet());

        int n = common.size();
        if (n == 0) {
            return 0.0;
        }

        double avgA = common.stream().mapToDouble(ratingsA::get).average().orElse(0.0);
        double avgB = common.stream().mapToDouble(ratingsB::get).average().orElse(0.0);

        double numerator = 0.0;
        double denomA = 0.0;
        double denomB = 0.0;

        for (Long spaceId : common) {
            double diffA = ratingsA.get(spaceId) - avgA;
            double diffB = ratingsB.get(spaceId) - avgB;
            numerator += diffA * diffB;
            denomA += diffA * diffA;
            denomB += diffB * diffB;
        }

        double denominator = Math.sqrt(denomA) * Math.sqrt(denomB);
        if (denominator == 0) {
            return 0.0;
        }
        return numerator / denominator;
    }

    /** 将 List<UserFieldScore> 转为 Map<userId, Map<spaceId, score>> */
    private Map<Long, Map<Long, Double>> buildDataset(List<UserFieldScore> allScores) {
        Map<Long, Map<Long, Double>> dataset = new HashMap<>();
        for (UserFieldScore s : allScores) {
            dataset
                .computeIfAbsent(s.getUserId(), k -> new HashMap<>())
                .put(s.getSpaceId(), s.getScore());
        }
        return dataset;
    }

    /** 冷启动 / 无邻居时，降级为按帖子数排序的热门社区 */
    private List<Space> fallbackHotSpaces(int topN) {
        System.out.println("无邻居，降级为按帖子数排序的热门社区");
        return spaceMapper.selectTopSpacesByPostCount(topN);
    }
}