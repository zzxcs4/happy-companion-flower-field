package com.zzx.yunpicturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 帖子资源上传结果
 * 上传图片/视频即创建帖子（仅含资源 url），返回帖子 id 与资源 url 供前端回显，
 * 前端填写标题/内容后调用更新接口补全帖子文字
 */
@Data
public class PostUploadVO implements Serializable {

    /**
     * 帖子 id
     */
    private Long postId;

    /**
     * 资源 url（视频为临时签名 URL，可直接播放）
     */
    private String url;

    /**
     * 视频缩略图 url（图片为 null）
     */
    private String thumbnailUrl;

    private static final long serialVersionUID = 1L;
}
