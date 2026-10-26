package com.zzx.flowerfieldfrienddisk.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zzx.flowerfieldfrienddisk.model.entity.UserField;
import com.zzx.flowerfieldfrienddisk.model.entity.UserFieldScore;
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

import static com.zzx.flowerfieldfrienddisk.constant.RedisConstants.*;

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
     * 每日凌晨 2 点执行：检查昨日访问网站的用户，对其未签到的社区进行扣分
     * 连续未签规则（per-user + per-field）：
     * - 连续 7 天未签某社区：对该社区评分扣 -1.0
     * - 连续 N 个 7 天未签：扣 -1.0 * N（6 分封顶）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void applyWeeklyInactivityPenalty() {
        log.info("开始执行连续未签惩罚任务");
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        int yDay = yesterday.getDayOfMonth();
        String yMonthKey = yesterday.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        
        // Step 1: 获取所有昨天访问过网站的用户 ID
        // 由于 bitmap key 是 visit:website:{userId}:{yyyyMM}，无法直接扫描所有用户
        // 所以改为遍历有评分记录的用户，检查其昨天是否访问网站
        List<UserFieldScore> allScores = userFieldScoreService.list();
        if (allScores == null || allScores.isEmpty()) {
            log.info("无评分记录，跳过惩罚任务");
            return;
        }
        
        Set<Long> userIds = allScores.stream()
                .map(UserFieldScore::getUserId)
                .collect(Collectors.toSet());
        
        int penalizedUserCount = 0;
        int penalizedFieldCount = 0;
        
        for (Long userId : userIds) {
            try {
                // Step 2: 检查用户昨天是否访问了网站
                String websiteKey = VISIT_WEBSITE_KEY + userId + yMonthKey;
                Boolean visitedYesterday = redisTemplate.opsForValue().getBit(websiteKey, yDay - 1);
                
                if (visitedYesterday == null || !visitedYesterday) {
                    // 用户昨天没访问网站，跳过
                    continue;
                }
                
                // Step 3: 用户昨天访问了网站，获取其加入的所有花田
                QueryWrapper<UserField> ufQuery = new QueryWrapper<>();
                ufQuery.eq("userId", userId);
                List<UserField> userFields = userFieldService.list(ufQuery);
                
                if (userFields.isEmpty()) {
                    continue;
                }
                
                // Step 4: 对每个花田检查连续未签情况
                for (UserField uf : userFields) {
                    Long fieldId = uf.getFieldId();
                    
                    // 检查该花田昨天是否签到（通过 VISIT_FIELD_KEY bitmap）
                    String fieldVisitKey = VISIT_FIELD_KEY + userId + ":" + fieldId + yMonthKey;
                    Boolean fieldVisitedYesterday = redisTemplate.opsForValue().getBit(fieldVisitKey, yDay - 1);
                    
                    if (fieldVisitedYesterday != null && fieldVisitedYesterday) {
                        // 昨天签到了该花田，重置连续未签计数
                        String inactiveKey = INACTIVE_WEEKS_KEY + userId + ":" + fieldId;
                        redisTemplate.delete(inactiveKey);
                        continue;
                    }
                    
                    // 昨天未签该花田，递增连续未签天数
                    String inactiveKey = INACTIVE_WEEKS_KEY + userId + ":" + fieldId;
                    Object daysObj = redisTemplate.opsForValue().get(inactiveKey);
                    int consecutiveInactiveDays = daysObj != null ? Integer.parseInt(daysObj.toString()) : 0;
                    consecutiveInactiveDays++;
                    
                    // 每满 7 天扣 1 分（固定扣 -1.0，不累积）
                    if (consecutiveInactiveDays % 7 == 0) {
                        // 只对该花田扣 -1.0 分
                        userFieldScoreService.addScore(userId, fieldId, -1.0);
                        
                        penalizedFieldCount++;
                        log.debug("用户 {} 对花田 {} 连续 {} 天未签，扣 -1.0",
                                userId, fieldId, consecutiveInactiveDays);
                    }
                    
                    // 更新连续未签天数
                    redisTemplate.opsForValue().set(inactiveKey, consecutiveInactiveDays, 30, TimeUnit.DAYS);
                }
                
                penalizedUserCount++;
                
            } catch (Exception e) {
                log.error("处理用户 {} 的未签惩罚时出错", userId, e);
            }
        }
        
        log.info("连续未签惩罚任务完成，检查 {} 个用户，惩罚 {} 个花田",
                penalizedUserCount, penalizedFieldCount);
    }
}
