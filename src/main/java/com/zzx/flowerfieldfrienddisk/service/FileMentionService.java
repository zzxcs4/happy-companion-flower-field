package com.zzx.flowerfieldfrienddisk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.flowerfieldfrienddisk.model.entity.File;
import com.zzx.flowerfieldfrienddisk.model.entity.FileComment;
import com.zzx.flowerfieldfrienddisk.model.entity.FileMention;
import com.zzx.flowerfieldfrienddisk.model.entity.User;
import com.zzx.flowerfieldfrienddisk.model.vo.FileMentionVO;

import java.util.List;

/**
 * 文件评论 @ 提及服务
 */
public interface FileMentionService extends IService<FileMention> {

    /**
     * 发布评论时写入提及记录（仅团队/社区空间、排除 @ 自己）
     *
     * @param fileComment 已保存的评论
     * @param file        被评论的文件
     * @param atUserIds   被 @ 的用户 id 列表
     * @param loginUser   评论者
     */
    void saveMentions(FileComment fileComment, File file, List<Long> atUserIds, User loginUser);

    /**
     * 查询我有未读 @ 提及的空间 id 列表（菜单栏红点）
     */
    List<Long> listUnreadSpaceIds(User loginUser);

    /**
     * 查询某空间内 @ 我的提及列表
     */
    List<FileMentionVO> listMyMentions(Long spaceId, User loginUser);

    /**
     * 将某空间内 @ 我的提及全部标记已读
     */
    boolean markRead(Long spaceId, User loginUser);
}
