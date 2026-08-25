package com.zzx.yunpicturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 「@我的」提及记录视图（联表填充文件名、评论内容、评论者信息）
 */
@Data
public class FileMentionVO implements Serializable {

    /**
     * 评论 id
     */
    private Long commentId;

    /**
     * 文件 id
     */
    private Long fileId;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 被评论的文件名
     */
    private String fileName;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论者昵称
     */
    private String fromUserName;

    /**
     * 评论者头像
     */
    private String fromUserAvatar;

    /**
     * 提及产生时间（评论时间）
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
