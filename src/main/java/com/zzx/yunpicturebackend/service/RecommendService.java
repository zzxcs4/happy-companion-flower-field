package com.zzx.yunpicturebackend.service;

import com.zzx.yunpicturebackend.model.entity.Field;

import java.util.List;

public interface RecommendService {

    /**
     * 基于用户基协同过滤（皮尔森相关系数）推荐花田
     *
     * @param userId 当前用户id
     * @param topN   推荐数量
     * @return 推荐花田列表
     */
    List<Field> recommendFields(Long userId, int topN);
}
