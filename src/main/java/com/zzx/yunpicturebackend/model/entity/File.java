package com.zzx.yunpicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

import javax.annotation.Nullable;

/**
 * 图片
 * @TableName picture
 */
@TableName(value ="file")
@Data
public class File implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 图片体积
     */
    private Long size;

    /**
     * 图片宽度
     */
   @Nullable
    private Integer picWidth;

    /**
     * 图片高度
     */
   @Nullable
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
   @Nullable
    private Double picScale;

    /**
     * 文件格式
     */
    private String format;

    /**
     * 图片主色调
     */
   @Nullable
    private String picColor;


    /**
     * 创建用户 id
     */
    private Long userId;

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
     * 状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人 id
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;


    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
   @Nullable
    private  String coverUrl;
   @Nullable
    private  Integer duration;
   @Nullable
    private  String fileType;

    /**
     * AI 生成的摘要
     */
   @Nullable
    private String summary;

    /**
     * 是否允许下载：0-不允许 1-允许（默认允许，发布帖子时可选）
     */
   @Nullable
    private Integer canDownload;
}
