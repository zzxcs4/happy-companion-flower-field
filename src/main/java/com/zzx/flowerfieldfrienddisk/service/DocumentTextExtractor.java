package com.zzx.flowerfieldfrienddisk.service;

import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 文档文本提取服务
 * 使用 Apache Tika 从 PDF、Word、TXT 等文档中提取纯文本
 */
@Service
@Slf4j
public class DocumentTextExtractor {

    /**
     * 支持提取文本的文档类型
     */
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
            "pdf", "doc", "docx", "txt", "md", "rtf", "xls", "xlsx", "ppt", "pptx", "csv"
    );

    private final Tika tika = new Tika();

    /**
     * 判断文件格式是否支持文本提取
     */
    public boolean isSupported(String format) {
        return format != null && SUPPORTED_FORMATS.contains(format.toLowerCase());
    }

    /**
     * 从文件 URL 下载并提取文本内容
     *
     * @param fileUrl 文件下载地址（COS URL）
     * @param format  文件格式
     * @return 提取的纯文本（最多返回前 8000 字符，避免超出 LLM token 限制）
     */
    public String extractText(String fileUrl, String format) {
        if (!isSupported(format)) {
            throw new UnsupportedOperationException("不支持的文件格式: " + format);
        }
        try {
            // 下载文件到内存（限制最大 20MB）
            byte[] fileBytes = HttpUtil.downloadBytes(fileUrl);
            if (fileBytes.length > 20 * 1024 * 1024) {
                log.warn("文件过大，跳过文本提取，size={}", fileBytes.length);
                return null;
            }
            // 使用 Tika 提取文本
            String text = tika.parseToString(new ByteArrayInputStream(fileBytes));
            if (text == null || text.trim().isEmpty()) {
                log.warn("文件文本提取结果为空，url={}", fileUrl);
                return null;
            }
            // 清理空白字符，截取前 8000 字符
            text = text.trim().replaceAll("\\s+", " ");
            if (text.length() > 8000) {
                text = text.substring(0, 8000);
            }
            return text;
        } catch (IOException | TikaException e) {
            log.error("文档文本提取失败，url={}", fileUrl, e);
            return null;
        }
    }

    /**
     * 从文件字节中提取纯文本（用于 doc 等格式的在线预览）
     * 与 extractText 不同：保留换行排版，不压缩空白，截取前 50000 字符
     *
     * @param fileBytes 文件字节
     * @return 提取的文本（保留换行），提取失败返回 null
     */
    public String extractTextForPreview(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            return null;
        }
        try {
            String text = tika.parseToString(new ByteArrayInputStream(fileBytes));
            if (text == null) {
                return null;
            }
            if (text.length() > 50000) {
                text = text.substring(0, 50000);
            }
            return text;
        } catch (IOException | TikaException e) {
            log.error("文档预览文本提取失败", e);
            return null;
        }
    }
}
