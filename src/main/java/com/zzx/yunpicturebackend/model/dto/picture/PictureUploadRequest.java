package com.zzx.yunpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {
  
    /**  
     * 图片 id（用于修改）  
     */  
    private Long id;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 是否允许下载：0-不允许 1-允许（默认允许，不传则保持默认）
     */
    private Integer canDownload;


    private static final long serialVersionUID = 1L;  
}
