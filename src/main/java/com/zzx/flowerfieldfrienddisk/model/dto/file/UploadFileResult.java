package com.zzx.flowerfieldfrienddisk.model.dto.file;

import lombok.Data;

@Data
public class UploadFileResult {
    /**
     * 地址
     */
    private String url;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 文件体积
     */
    private Long size;

    /**
     * 格式
     */
    private String format;
}
