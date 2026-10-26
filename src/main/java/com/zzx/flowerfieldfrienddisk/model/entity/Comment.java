package com.zzx.flowerfieldfrienddisk.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * @TableName news_headline
 */
@TableName("post_comment")
@Data
public class Comment {
    @TableId
    private Long id;

    private String context;

    private Long publisher;

    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String userAvatar;


    private Long post;

    private Date createTime;

    private Date updateTime;
//    @Version
//    private Integer version;
//
//    private Integer isDeleted;
}