package com.zzx.yunpicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import java.util.List;

import lombok.Data;

/**
 * 
 * @TableName post
 */
@TableName(value ="post")
@Data
public class Post {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属社区空间id（即原花田）
     */
    private Long spaceId;

    /**
     * 用户id
     */
    private Long userId;


    /**
     * 用户图标
     */
    @TableField(exist = false)
    private String icon;
    /**
     * 用户姓名
     */
    @TableField(exist = false)
    private String name;
    /**
     * 是否点赞过了
     */
    @TableField(exist = false)
    private Boolean isLike;

    @TableField(exist = false)
    private String field;

    /**
     * 标题
     */
    private String title;

    /**
     * 帖子照片，最多9张，多张以","隔开
     */
    private String images;

    /**
     * 文字描述
     */
    private String content;

    /**
     * 点赞数量
     */
    private Integer liked;

    /**
     * 评论数量
     */
    private Integer comments;

    /**
     * 权限列表
     */
    @TableField(exist = false)
    private List<String> permissionsList;
    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}