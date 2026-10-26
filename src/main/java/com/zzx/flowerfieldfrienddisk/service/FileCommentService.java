package com.zzx.flowerfieldfrienddisk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.flowerfieldfrienddisk.model.entity.FileComment;
import com.zzx.flowerfieldfrienddisk.model.entity.User;
import com.zzx.flowerfieldfrienddisk.model.vo.UserVO;

import java.util.List;

/**
 * 文件评论服务
 */
public interface FileCommentService extends IService<FileComment> {

    /**
     * 发布文件评论
     *
     * @param fileId    文件 id
     * @param content   评论内容
     * @param atUserIds 被 @ 的用户 id 列表
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean addComment(Long fileId, String content, List<Long> atUserIds, User loginUser);

    /**
     * 查询文件评论列表（含评论者信息）
     *
     * @param fileId    文件 id
     * @param loginUser 当前登录用户
     */
    List<FileComment> listComments(Long fileId, User loginUser);

    /**
     * 删除评论（仅本人或管理员）
     *
     * @param commentId 评论 id
     * @param loginUser 当前登录用户
     */
    boolean deleteComment(Long commentId, User loginUser);

    /**
     * 获取可 @ 的用户列表
     * 公共空间（spaceId 为空）→ 当前用户关注的用户；团队/社区空间 → 空间成员
     *
     * @param fileId    文件 id
     * @param loginUser 当前登录用户
     */
    List<UserVO> listMentionableUsers(Long fileId, User loginUser);
}
