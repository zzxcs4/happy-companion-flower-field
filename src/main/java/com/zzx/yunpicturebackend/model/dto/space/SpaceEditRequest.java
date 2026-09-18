package com.zzx.yunpicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceEditRequest implements Serializable {

    /**
     * 空间 id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间封面图
     */
    private String picUrl;

    /**
     * 可见状态：0-公开 1-私有 2-加密（仅社区空间）
     */
    private Integer status;

    /**
     * 社区密码（仅加密社区空间）
     */
    private String password;

    /**
     * 分类id（仅社区空间）
     */
    private Long categoryId;

    /**
     * 最大人数（团队/社区空间）
     */
    private Integer maxNum;

    /**
     * 描述
     */
    private String description;

    private static final long serialVersionUID = 1L;
}
