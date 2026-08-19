package com.zzx.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zzx.yunpicturebackend.model.entity.UserField;
import com.zzx.yunpicturebackend.model.entity.UserFieldScore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zzx.yunpicturebackend.constant.RedisConstants.*;

/**
 * 推荐系统评分定时任务服务
 * 负责处理连续未签到的惩罚机制
 */
@Service
@Slf4j
public class FieldScoreTaskService {

    @Resource
    private UserFieldScoreService userFieldScoreService;
    @Resource
    private UserFieldService userFieldService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 每日凌晨 2 点执行：检查昨日活跃用户，对未签到的用户进行扣分
     * 连续未签规则：
     * - 1 周未签：-1.0
     * - 连续 N 周未签：-1.0 * N（6 分封顶）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void applyWeeklyInactivityPenalty() {
        log.info("开始执行连续未签惩罚任务");
        
        // 获取所有有评分记录的用户
        List<UserFieldScore> allScores = userFieldScoreService.list();
        if (allScores == null || allScores.isEmpty()) {
            log.info("无评分记录，跳过惩罚任务");
            return;
        }
        
        // 提取所有唯一的用户 ID
        Set<Long> userIds = allScores.stream()
                .map(UserFieldScore::getUserId)
                .collect(Collectors.toSet());
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        int yDay = yesterday.getDayOfMonth();
        String yMonthKey = yesterday.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        
        int penalizedCount = 0;
        
        for (Long userId : userIds) {
            try {
                // 检查用户昨天是否活跃（访问网站或签到）
                boolean wasActiveYesterday = checkUserActiveOnDate(userId, yesterday);
                
                if (!wasActiveYesterday) {
                    // 用户昨天未活跃，获取其加入的花田
                    QueryWrapper<UserField> ufQuery = new QueryWrapper<>();
                    ufQuery.eq("userId", userId);
                    List<UserField> userFields = userFieldService.list(ufQuery);
                    
                    if (userFields.isEmpty()) {
                        continue;
                    }
                    
                    // 获取或初始化连续未签周数
                    String inactiveWeeksKey = INACTIVE_WEEKS_KEY + userId;
                    Object weeksObj = redisTemplate.opsForValue().get(inactiveWeeksKey);
                    int consecutiveInactiveWeeks = weeksObj != null ? Integer.parseInt(weeksObj.toString()) : 0;
                    
                    // 检查是否是新的不活跃周（每 7 天递增）
                    String lastCheckKey = "inactive:last:check:" + userId;
                    Object lastCheckObj = redisTemplate.opsForValue().get(lastCheckKey);
                    
                    boolean shouldIncrement = false;
                    if (lastCheckObj == null) {
                        // 首次检查
                        shouldIncrement = true;
                    } else {
                        long lastCheckDays = Long.parseLong(lastCheckObj.toString());
                        long daysSinceLastCheck = java.time.temporal.ChronoUnit.DAYS.between(
                                LocalDate.ofEpochDay(lastCheckDays), yesterday);
                        if (daysSinceLastCheck >= 7) {
                            shouldIncrement = true;
                        }
                    }
                    
                    if (shouldIncrement) {
                        consecutiveInactiveWeeks++;
                        redisTemplate.opsForValue().set(inactiveWeeksKey, consecutiveInactiveWeeks, 30, TimeUnit.DAYS);
                        redisTemplate.opsForValue().set(lastCheckKey, yesterday.toEpochDay(), 30, TimeUnit.DAYS);
                        
                        // 计算惩罚分数：-1.0 * N，6 分封顶
                        double penalty = Math.min(1.0 * consecutiveInactiveWeeks, 6.0);
                        
                        // 对用户加入的每个花田应用惩罚
                        for (UserField uf : userFields) {
                            userFieldScoreService.addScore(userId, uf.getFieldId(), -penalty);
                        }
                        
                        penalizedCount++;
                        log.debug("用户 {} 连续 {} 周未签，扣分 {}", userId, consecutiveInactiveWeeks, penalty);
                    }
                } else {
                    // 用户昨天活跃，重置连续未签周数
                    String inactiveWeeksKey = INACTIVE_WEEKS_KEY + userId;
                    redisTemplate.delete(inactiveWeeksKey);
                    String lastCheckKey = "inactive:last:check:" + userId;
                    redisTemplate.delete(lastCheckKey);
                }
            } catch (Exception e) {
                log.error("处理用户 {} 的未签惩罚时出错", userId, e);
            }
        }
        
        log.info("连续未签惩罚任务完成，共惩罚 {} 个用户", penalizedCount);
    }
    
    /**
     * 检查用户在指定日期是否活跃
     */
    private boolean checkUserActiveOnDate(Long userId, LocalDate date) {
        int day = date.getDayOfMonth();
        String monthKey = date.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        
        // 检查网站访问 bitmap
        String websiteKey = VISIT_WEBSITE_KEY + userId + monthKey;
        Boolean websiteVisited = redisTemplate.opsForValue().getBit(websiteKey, day - 1);
        if (websiteVisited != null && websiteVisited) {
            return true;
        }
        
//        // 检查传统签到 bitmap（遍历所有可能的 spaceId）
//        // 由于 spaceId 不固定，这里简化为检查用户是否有任何签到记录
//        // 实际使用时可以通过 UserField 表获取用户加入的花田对应的 spaceId
//        List<UserField> userFields = userFieldService.list(
//                new QueryWrapper<UserField>().eq("userId", userId));
//        for (UserField uf : userFields) {
//            // 这里假设 spaceId 可以通过某种方式获取
//            // 暂时返回 false，实际部署时需要完善
//        }

        return false;
    }
}
