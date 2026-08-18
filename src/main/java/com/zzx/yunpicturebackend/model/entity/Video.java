package com.zzx.yunpicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 视频信息表
 * @TableName videos
 */
@TableName(value ="videos")
@Data
public class Video {
    /**
     * 视频ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 视频标题
     */
    private String title;

    /**
     * 视频描述
     */
    private String description;

    /**
     * 分类ID
     */
    private Long category_id;

    /**
     * 视频文件URL
     */
    private String file_url;

    /**
     * 封面图片URL
     */
    private String cover_url;

    /**
     * 视频时长（秒）
     */
    private Integer duration;

    /**
     * 文件大小（字节）
     */
    private Long file_size;

    /**
     * 上传者名称
     */
    private String uploader_name;

    /**
     * 上传者类型：1-用户投稿，2-管理员上传
     */
    private Integer uploader_type;

    /**
     * 上传用户ID（用户投稿时）
     */
    private Long user_id;

    /**
     * 管理员ID（管理员上传时）
     */
    private Long admin_id;

    /**
     * 状态：0-待审核，1-已发布，2-已拒绝，3-已下架
     */
    private Integer status;

    /**
     * 审核管理员ID
     */
    private Long audit_admin_id;

    /**
     * 审核时间
     */
    private Date audit_time;

    /**
     * 审核原因（拒绝时）
     */
    private String audit_reason;

    /**
     * 观看次数
     */
    private Long view_count;

    /**
     * 点赞次数
     */
    private Long like_count;

    /**
     * 标签，逗号分隔
     */
    private String tags;

    /**
     * 创建时间
     */
    private Date created_at;

    /**
     * 更新时间
     */
    private Date updated_at;
}