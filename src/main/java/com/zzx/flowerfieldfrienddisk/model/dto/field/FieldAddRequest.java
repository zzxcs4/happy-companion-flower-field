package com.zzx.flowerfieldfrienddisk.model.dto.field;

import lombok.Data;

import java.io.Serializable;

@Data
public class FieldAddRequest implements Serializable {

    /**
     * 空间名称
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
    private Integer maxNum;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;

    /**
     * 一级分类
     */
    private String firstCategory;

    /**
     * 二级分类
     */
    private String secondCategory;


    private static final long serialVersionUID = 1L;
}