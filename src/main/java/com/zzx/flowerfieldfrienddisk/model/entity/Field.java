package com.zzx.flowerfieldfrienddisk.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;
import lombok.Data;

/**
 * 花田
 * @TableName field
 */
@TableName(value ="field")
@Data
public class Field {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍封面
     */
    private String fieldPicUrl;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer maxNum;

    /**
     * 帖子数量
     */
    private Integer postNum;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;

    /**
     * 空间(团队图库) id
     */
    private Long spaceId;

    /**
     * 一级分类id
     */
    private String firstCategory;

    /**
     * 二级分类id
     */
    private String secondCategory;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;
}