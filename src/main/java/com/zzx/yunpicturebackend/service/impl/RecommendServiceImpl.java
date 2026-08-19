package com.zzx.yunpicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zzx.yunpicturebackend.constant.RedisConstants;
import com.zzx.yunpicturebackend.mapper.FieldMapper;
import com.zzx.yunpicturebackend.model.entity.Field;
import com.zzx.yunpicturebackend.model.entity.UserFieldScore;
import com.zzx.yunpicturebackend.service.FieldService;
import com.zzx.yunpicturebackend.service.RecommendService;
import com.zzx.yunpicturebackend.service.UserFieldScoreService;
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
 * 基于用户基协同过滤（皮尔森相关系数）的花田推荐
 *
 * <p>算法步骤：
 * <ol>
 *   <li>构造 用户 → {花田id → 评分} 数据集</li>
 *   <li>计算目标用户与其他所有用户之间的皮尔森相关系数</li>
 *   <li>取相关性最高的用户作为"最近邻"</li>
 *   <li>推荐最近邻评分过但目标用户从未涉及的花田</li>
 * </ol>
 */
@Service
public class RecommendServiceImpl implements RecommendService, RedisConstants {

    private static final Logger log = LoggerFactory.getLogger(RecommendServiceImpl.class);

    @Resource
    private UserFieldScoreService userFieldScoreService;
    @Resource
    private FieldService fieldService;
    @Autowired
    private FieldMapper fieldMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * L1 本地缓存（Caffeine）：存储推荐花田 ID 列表
     * 策略：仅依赖 TTL 自动过期，不做主动清除
     * - maximumSize: 最多缓存 1000 个用户的推荐结果
     * - expireAfterWrite: 10 分钟自动过期
     */
    private final Cache<String, List<Long>> localCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    /** L2 分布式缓存（Redis）TTL：15 分钟 */
    private static final long REDIS_CACHE_TTL_MINUTES = 15;

    // -------------------------------------------------------------------------
    // 公共接口
    // -------------------------------------------------------------------------

    @Override
    public List<Field> recommendFields(Long userId, int topN) {
        String cacheKey = RECOMMEND_FIELDS_KEY + userId + ":" + topN;

        // ===== L1: Caffeine 本地缓存 =====
        List<Long> cachedIds = localCache.getIfPresent(cacheKey);
        if (cachedIds != null) {
            log.debug("[推荐缓存] L1 命中, userId={}", userId);
            return buildFieldsFromIds(cachedIds);
        }

        // ===== L2: Redis 分布式缓存 =====
        List<Long> redisIds = getFromRedis(cacheKey);
        if (redisIds != null) {
            log.debug("[推荐缓存] L2 命中, userId={}", userId);
            localCache.put(cacheKey, redisIds); // 回填 L1
            return buildFieldsFromIds(redisIds);
        }

        // ===== Cache Miss: 执行协同过滤计算 =====
        log.debug("[推荐缓存] Miss, 执行计算, userId={}", userId);
        List<Long> recommendFieldIds = computeRecommendation(userId, topN);

        if (recommendFieldIds != null && !recommendFieldIds.isEmpty()) {
            // 写入双层缓存
            localCache.put(cacheKey, recommendFieldIds);
            putToRedis(cacheKey, recommendFieldIds);
        }

        return buildFieldsFromIds(recommendFieldIds);
    }

    /**
     * 根据花田 ID 列表查询 Field 实体并保持顺序
     */
    private List<Field> buildFieldsFromIds(List<Long> fieldIds) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Field> fields = fieldService.listByIds(fieldIds);
        Map<Long, Field> fieldMap = fields.stream()
                .collect(Collectors.toMap(Field::getId, f -> f));
        return fieldIds.stream()
                .filter(fieldMap::containsKey)
                .map(fieldMap::get)
                .collect(Collectors.toList());
    }

    /**
     * 核心协同过滤计算（无缓存）
     */
    private List<Long> computeRecommendation(Long userId, int topN) {
        // 1. 构造数据集：所有 (userId, fieldId, score) 记录
        List<UserFieldScore> allScores = userFieldScoreService.list();
        if (allScores == null || allScores.isEmpty()) {
            List<Field> hotFields = fallbackHotFields(topN);
            return hotFields.stream().map(Field::getId).collect(Collectors.toList());
        }

        // 2. 转换为 Map<userId, Map<fieldId, score>>
        Map<Long, Map<Long, Double>> dataset = buildDataset(allScores);

        // 如果目标用户在数据集中没有任何评分记录，返回全局热门花田
        if (!dataset.containsKey(userId)) {
            List<Field> hotFields = fallbackHotFields(topN);
            return hotFields.stream().map(Field::getId).collect(Collectors.toList());
        }

        // 3. 计算皮尔森系数，选出相关性最强的最近邻用户
        Long nearestNeighborId = findNearestNeighbor(userId, dataset);
        if (nearestNeighborId == null) {
            List<Field> hotFields = fallbackHotFields(topN);
            return hotFields.stream().map(Field::getId).collect(Collectors.toList());
        }

        // 4. 取出 "最近邻评分过 但目标用户没涉及过" 的花田id
        Set<Long> targetFieldIds = dataset.get(userId).keySet();
        Map<Long, Double> neighborScores = dataset.get(nearestNeighborId);

        return neighborScores.entrySet().stream()
                .filter(e -> !targetFieldIds.contains(e.getKey()))
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

    private void putToRedis(String key, List<Long> fieldIds) {
        try {
            String json = fieldIds.stream()
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

    /**
     * 找到与目标用户皮尔森系数最高的用户 id。
     *
     * <p>皮尔森相关系数公式：
     * <pre>
     *        Σ[(Ra - avg_a)(Rb - avg_b)]
     * r = ─────────────────────────────────────────────
     *      sqrt(Σ(Ra-avg_a)²) * sqrt(Σ(Rb-avg_b)²)
     * </pre>
     * 只对两人共同评分过的花田计算。
     */
    private Long findNearestNeighbor(Long targetUserId,
                                     Map<Long, Map<Long, Double>> dataset) {
        Map<Long, Double> targetRatings = dataset.get(targetUserId);

        // 用 TreeMap 按皮尔森系数升序排列，最后一个即为最近邻
        TreeMap<Double, Long> similarityMap = new TreeMap<>();

        for (Map.Entry<Long, Map<Long, Double>> entry : dataset.entrySet()) {
            Long otherUserId = entry.getKey();
            if (otherUserId.equals(targetUserId)) {
                continue;
            }
            Map<Long, Double> otherRatings = entry.getValue();

            double pearson = calcPearson(targetRatings, otherRatings);
            // 若系数相同，TreeMap 会覆盖，可接受（概率极低）
            similarityMap.put(pearson, otherUserId);
        }

        if (similarityMap.isEmpty()) {
            return null;
        }

        // 取皮尔森系数最大的用户（正相关最强）
        double maxSimilarity = similarityMap.lastKey();
        // 相关性 <= 0 时说明没有正相关邻居，退化为热门推荐
        if (maxSimilarity <= 0) {
            return null;
        }
        return similarityMap.lastEntry().getValue();
    }

    /**
     * 计算两个用户在共同评分花田上的皮尔森相关系数。
     */
    private double calcPearson(Map<Long, Double> ratingsA,
                               Map<Long, Double> ratingsB) {
        // 取交集（共同评分过的花田）
        Set<Long> common = new HashSet<>(ratingsA.keySet());
        common.retainAll(ratingsB.keySet());

        int n = common.size();
        if (n == 0) {
            return 0.0;
        }

        // 计算各自均值
        double avgA = common.stream().mapToDouble(ratingsA::get).average().orElse(0.0);
        double avgB = common.stream().mapToDouble(ratingsB::get).average().orElse(0.0);

        double numerator = 0.0;
        double denomA = 0.0;
        double denomB = 0.0;

        for (Long fieldId : common) {
            double diffA = ratingsA.get(fieldId) - avgA;
            double diffB = ratingsB.get(fieldId) - avgB;
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

    // -------------------------------------------------------------------------
    // 工具方法
    // -------------------------------------------------------------------------

    /** 将 List<UserFieldScore> 转为 Map<userId, Map<fieldId, score>> */
    private Map<Long, Map<Long, Double>> buildDataset(List<UserFieldScore> allScores) {
        Map<Long, Map<Long, Double>> dataset = new HashMap<>();
        for (UserFieldScore s : allScores) {
            dataset
                .computeIfAbsent(s.getUserId(), k -> new HashMap<>())
                .put(s.getFieldId(), s.getScore());
        }
        return dataset;
    }

    /** 冷启动 / 无邻居时，降级为按 postNum 排序的热门花田 */
    private List<Field> fallbackHotFields(int topN) {
        System.out.println("无邻居，降级为按 postNum 排序的热门花田");
        return fieldMapper.selectTopFieldsByPostCount(topN);
    }
}
