package com.zzx.yunpicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.yunpicturebackend.mapper.UserFieldScoreMapper;
import com.zzx.yunpicturebackend.model.entity.UserFieldScore;
import com.zzx.yunpicturebackend.service.UserFieldScoreService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserFieldScoreServiceImpl extends ServiceImpl<UserFieldScoreMapper, UserFieldScore>
        implements UserFieldScoreService {

    /** 评分上限 */
    private static final double MAX_SCORE = 10.0;

    @Override
    public void addScore(Long userId, Long fieldId, double delta) {
        LambdaQueryWrapper<UserFieldScore> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFieldScore::getUserId, userId).eq(UserFieldScore::getFieldId, fieldId);
        UserFieldScore record = getOne(qw);

        if (record == null) {
            record = new UserFieldScore();
            record.setUserId(userId);
            record.setFieldId(fieldId);
            record.setScore(Math.min(Math.max(round(delta), 0), MAX_SCORE));
            record.setCreateTime(new Date());
            record.setUpdateTime(new Date());
            save(record);
        } else {
            double newScore = Math.min(Math.max(round(record.getScore() + delta), 0), MAX_SCORE);
            record.setScore(newScore);
            record.setUpdateTime(new Date());
            updateById(record);
        }
    }
    
    @Override
    public void addScoreCapped(Long userId, Long fieldId, double delta, double cap) {
        // 基础评分增加（受 0-10 限制）
        addScore(userId, fieldId, delta);
    }

    /**
     * 四舍五入到 4 位小数，消除 double 浮点精度误差
     * 例如：0.30000000000000004 -> 0.3
     */
    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
