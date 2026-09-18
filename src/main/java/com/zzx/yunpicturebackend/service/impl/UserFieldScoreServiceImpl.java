package com.zzx.yunpicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.yunpicturebackend.mapper.UserFieldScoreMapper;
import com.zzx.yunpicturebackend.model.entity.UserFieldScore;
import com.zzx.yunpicturebackend.service.UserFieldScoreService;
import org.springframework.stereotype.Service;

@Service
public class UserFieldScoreServiceImpl extends ServiceImpl<UserFieldScoreMapper, UserFieldScore>
        implements UserFieldScoreService {

    @Override
    public void addScore(Long userId, Long spaceId, double delta) {
        // 原子 UPSERT：不存在则插入、存在则累加，
        // 由唯一索引 uk_user_field(user_id, space_id) 保证并发正确，避免丢更新
        baseMapper.addScoreAtomic(userId, spaceId, delta);
    }

    @Override
    public void addScoreCapped(Long userId, Long spaceId, double delta, double cap) {
        // 基础评分增加（受 0-10 限制）
        addScore(userId, spaceId, delta);
    }
}