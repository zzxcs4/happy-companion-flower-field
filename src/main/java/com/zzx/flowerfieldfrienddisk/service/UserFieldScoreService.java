package com.zzx.flowerfieldfrienddisk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.flowerfieldfrienddisk.model.entity.UserFieldScore;

public interface UserFieldScoreService extends IService<UserFieldScore> {

    /**
     * 增加用户对某社区空间（花田）的评分，上限 10 分
     *
     * @param userId  用户id
     * @param spaceId 社区空间id
     * @param delta   增量（可为负数）
     */
    void addScore(Long userId, Long spaceId, double delta);
    
    /**
     * 增加用户对某社区空间（花田）的评分，带额外上限（用于签到奖励等场景）
     *
     * @param userId  用户id
     * @param spaceId 社区空间id
     * @param delta   增量
     * @param cap     该类型增量的累计上限
     */
    void addScoreCapped(Long userId, Long spaceId, double delta, double cap);
}
