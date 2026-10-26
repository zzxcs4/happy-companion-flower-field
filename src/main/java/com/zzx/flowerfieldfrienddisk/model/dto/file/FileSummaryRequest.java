package com.zzx.flowerfieldfrienddisk.model.dto.file;

import lombok.Data;

import java.io.Serializable;

/**
 * 文档 AI 摘要生成请求
 */
@Data
public class FileSummaryRequest implements Serializable {

    /**
     * 文件 id
     */
    private Long fileId;

    private static final long serialVersionUID = 1L;
}
