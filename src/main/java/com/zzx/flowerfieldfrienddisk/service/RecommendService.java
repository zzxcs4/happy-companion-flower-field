package com.zzx.flowerfieldfrienddisk.service;

import com.zzx.flowerfieldfrienddisk.model.entity.Space;

import java.util.List;

public interface RecommendService {

    /**
     * 基于用户基协同过滤（皮尔森相关系数）推荐社区空间（花田）
     *
     * @param userId 当前用户id
     * @param topN   推荐数量
     * @return 推荐社区空间列表
     */
    List<Space> recommendFields(Long userId, int topN);
}