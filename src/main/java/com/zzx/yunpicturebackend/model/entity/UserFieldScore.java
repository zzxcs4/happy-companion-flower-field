package com.zzx.yunpicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户-花田 评分表（用于协同过滤推荐）
 * @TableName user_field_score
 */
@TableName(value = "user_field_score")
@Data
public class UserFieldScore {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户id */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 社区空间id（原花田id） */
    @TableField(value = "space_id")
    private Long spaceId;

    /** 评分（0~10） */
    private Double score;

    /** 创建时间 */
    @TableField(value = "create_time")
    private Date createTime;

    /** 修改时间 */
    @TableField(value = "update_time")
    private Date updateTime;
}
