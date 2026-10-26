package com.zzx.flowerfieldfrienddisk.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUpdateRequest implements Serializable {
  
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
  
    private static final long serialVersionUID = 1L;  
}
