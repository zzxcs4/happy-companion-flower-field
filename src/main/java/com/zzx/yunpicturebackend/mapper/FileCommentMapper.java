package com.zzx.yunpicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzx.yunpicturebackend.model.entity.FileComment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件评论 数据库操作
 */
public interface FileCommentMapper extends BaseMapper<FileComment> {

    /**
     * 查询文件的评论列表（联表填充评论者昵称、头像）
     *
     * @param fileId 文件 id
     */
    List<FileComment> listCommentsWithUser(@Param("fileId") Long fileId);
}
