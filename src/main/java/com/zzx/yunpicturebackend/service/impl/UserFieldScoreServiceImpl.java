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
            record.setScore(Math.min(Math.max(delta, 0), MAX_SCORE));
            record.setCreateTime(new Date());
            record.setUpdateTime(new Date());
            save(record);
        } else {
            double newScore = Math.min(Math.max(record.getScore() + delta, 0), MAX_SCORE);
            record.setScore(newScore);
            record.setUpdateTime(new Date());
            updateById(record);
        }
    }
}
