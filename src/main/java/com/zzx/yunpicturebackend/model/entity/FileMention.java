package com.zzx.yunpicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件评论 @ 提及记录
 * 每个被 @ 的用户一条记录，用于「@我的」未读红点与提及列表
 * @TableName file_mention
 */
@TableName("file_mention")
@Data
public class FileMention implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 评论 id
     */
    private Long commentId;

    /**
     * 文件 id
     */
    private Long fileId;

    /**
     * 空间 id（仅团队/社区空间产生提及）
     */
    private Long spaceId;

    /**
     * 评论者 id
     */
    private Long fromUserId;

    /**
     * 被 @ 的用户 id
     */
    private Long toUserId;

    /**
     * 是否已读（0 未读 1 已读）
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
