package com.zzx.flowerfieldfrienddisk.model.dto.post;

import com.zzx.flowerfieldfrienddisk.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 帖子分页查询请求（统一查询入口：花田动态 / 用户帖子 / 我的帖子 / 关注动态 / 帖子详情）
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PostQueryRequest extends PageRequest implements Serializable {

    /**
     * 所属社区空间 id（即花田 id）
     */
    private Long spaceId;

    /**
     * 用户 id（查询某个用户的帖子）
     */
    private Long userId;

    /**
     * 搜索词（同时搜索标题、内容）
     */
    private String searchText;

    /**
     * 是否仅查询当前登录用户的帖子
     */
    private boolean onlyMe;

    /**
     * 是否仅查询关注用户的帖子（关注动态流）
     */
    private boolean onlyFollowed;

    private static final long serialVersionUID = 1L;
}
