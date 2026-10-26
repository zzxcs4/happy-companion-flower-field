package com.zzx.flowerfieldfrienddisk.model.dto.filecomment;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 发布文件评论请求
 */
@Data
public class FileCommentAddRequest implements Serializable {

    /**
     * 文件 id
     */
    private Long fileId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 被 @ 的用户 id 列表
     */
    private List<Long> atUserIds;

    private static final long serialVersionUID = 1L;
}
