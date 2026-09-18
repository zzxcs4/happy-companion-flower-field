package com.zzx.yunpicturebackend.model.dto.post;

import lombok.Data;

import java.io.Serializable;

/**
 * 帖子视频上传请求
 * 帖子入库仅使用：资源 url、空间 id、用户 id（登录态），其他字段默认
 */
@Data
public class PostVideoUploadRequest implements Serializable {

    /**
     * 视频文件 MD5（前端已计算，用于 COS 对象命名；为空时后端兜底流式计算）
     */
    private String md5;

    /**
     * 所属空间（社区/花田）id
     */
    private Long spaceId;

    /**
     * 是否允许下载：0-不允许 1-允许（默认允许，不传则保持默认）
     */
    private Integer canDownload;

    private static final long serialVersionUID = 1L;
}
