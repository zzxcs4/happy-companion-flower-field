package com.zzx.yunpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureEditRequest implements Serializable {
  
    /**  
     * id  
     */  
    private Long id;

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
     * 是否允许下载：0-不允许 1-允许（不传则不修改）
     */
    private Integer canDownload;

    private static final long serialVersionUID = 1L;
}
