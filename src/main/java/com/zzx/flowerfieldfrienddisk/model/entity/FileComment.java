package com.zzx.flowerfieldfrienddisk.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件评论
 * @TableName file_comment
 */
@TableName("file_comment")
@Data
public class FileComment implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文件 id
     */
    private Long fileId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论用户 id
     */
    private Long userId;

    /**
     * 被 @ 的用户 id 列表（JSON 数组，如 [1,2,3]）
     */
    private String atUserIds;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 评论者昵称（联表查询填充）
     */
    @TableField(exist = false)
    private String userName;

    /**
     * 评论者头像（联表查询填充）
     */
    @TableField(exist = false)
    private String userAvatar;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
