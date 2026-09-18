package com.zzx.yunpicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@TableName(value ="space")
@Data
public class Space implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    private Long totalCount;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 空间类型：0-私有 1-团队 2-社区
     */
    private Integer spaceType;

    /**
     * 空间封面图（个人/团队/社区空间均可使用）
     */
    private String picUrl;

    /**
     * 可见状态：0-公开 1-私有 2-加密（仅社区空间使用）
     */
    private Integer status;

    /**
     * 社区密码（仅加密社区空间使用）
     */
    private String password;

    /**
     * 分类id（冗余字段，仅社区空间使用）
     */
    private Long categoryId;

    /**
     * 最大人数（团队/社区空间使用）
     */
    private Integer maxNum;

    /**
     * 描述（社区空间使用）
     */
    private String description;


    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
