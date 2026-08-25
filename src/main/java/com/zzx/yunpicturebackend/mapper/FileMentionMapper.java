package com.zzx.yunpicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzx.yunpicturebackend.model.entity.FileMention;
import com.zzx.yunpicturebackend.model.vo.FileMentionVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件评论 @ 提及记录
 */
public interface FileMentionMapper extends BaseMapper<FileMention> {

    /**
     * 查询某空间内 @ 我的提及列表（联表填充文件名、评论内容、评论者信息）
     *
     * @param toUserId 被 @ 的用户 id
     * @param spaceId  空间 id
     */
    List<FileMentionVO> listMentionsWithDetail(@Param("toUserId") Long toUserId, @Param("spaceId") Long spaceId);

    /**
     * 查询我有未读 @ 提及的空间 id 列表（用于菜单栏红点）
     *
     * @param toUserId 被 @ 的用户 id
     */
    List<Long> listUnreadSpaceIds(@Param("toUserId") Long toUserId);
}
