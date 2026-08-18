package com.zzx.yunpicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.yunpicturebackend.model.entity.UserFieldScore;

public interface UserFieldScoreService extends IService<UserFieldScore> {

    /**
     * 增加用户对某花田的评分，上限 10 分
     *
     * @param userId  用户id
     * @param fieldId 花田id
     * @param delta   增量（可为负数）
     */
    void addScore(Long userId, Long fieldId, double delta);
}
