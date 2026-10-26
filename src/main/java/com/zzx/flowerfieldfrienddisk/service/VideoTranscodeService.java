package com.zzx.flowerfieldfrienddisk.service;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.zzx.flowerfieldfrienddisk.config.CosClientConfig;
import com.zzx.flowerfieldfrienddisk.manager.CosManager;
import com.zzx.flowerfieldfrienddisk.manager.VideoAiManager;
import com.zzx.flowerfieldfrienddisk.mapper.FileMapper;
import com.zzx.flowerfieldfrienddisk.model.entity.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * 视频转码服务
 * 视频上传成功后，异步用 FFmpeg 将非 MP4 格式统一转码为 H.264 编码的 MP4，
 * 解决 avi/mov/wmv/flv/mkv 等格式浏览器无法直接播放的兼容性问题，顺带规范码率。
 * 压缩为事后增强，任何失败都不影响已成功的上传。
 */
@Slf4j
@Service
public class VideoTranscodeService {

    /**
     * 支持转码的视频格式（与前端 isVideo 保持一致）
     */
    private static final List<String> VIDEO_FORMATS = Arrays.asList(
            "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm"
    );

    @Resource
    private FileMapper fileMapper;
    @Resource
    private CosManager cosManager;
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private VideoAiManager videoAiManager;

    /**
     * 判断文件格式是否为视频
     */
    public boolean isVideoFormat(String format) {
        return format != null && VIDEO_FORMATS.contains(format.toLowerCase());
    }

    /**
     * 异步转码视频：从 COS 下载 → FFmpeg 转码为 H.264 MP4 → 上传替换原对象（更新 DB 的 url/format/size）
     */
    @Async
    public void transcodeAsync(Long fileId) {
        try {
            File file = fileMapper.selectById(fileId);
            if (file == null || !isVideoFormat(file.getFormat())) {
                return;
            }
            // FFmpeg 不可用时优雅降级，保留原文件
            if (!videoAiManager.isAvailable()) {
                log.info("FFmpeg 不可用，跳过视频转码, fileId={}", fileId);
                return;
            }
            String format = file.getFormat().toLowerCase();
            // mp4 浏览器可直接播放（H.264），跳过转码以节省服务器 CPU
            if ("mp4".equals(format)) {
                return;
            }

            String url = file.getUrl();
            String host = cosClientConfig.getHost();
            String originKey = url.startsWith(host) ? url.substring(host.length()) : url;

            java.io.File tempOrigin = null;
            java.io.File tempTranscoded = null;
            try {
                // 1. 从 COS 下载原始视频
                tempOrigin = java.io.File.createTempFile("video-origin-", "." + format);
                COSObject cosObject = cosManager.getObject(originKey);
                try (COSObjectInputStream cosInput = cosObject.getObjectContent()) {
                    java.nio.file.Files.copy(cosInput, tempOrigin.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                // 2. FFmpeg 转码为 H.264 MP4
                tempTranscoded = java.io.File.createTempFile("video-transcode-", ".mp4");
                videoAiManager.transcodeToMp4(tempOrigin, tempTranscoded.getAbsolutePath());

                // 3. 转码产物异常（空文件或转码失败），保留原文件
                if (tempTranscoded.length() <= 0) {
                    log.warn("视频转码产物为空，保留原文件, fileId={}", fileId);
                    return;
                }

                // 4. 上传转码版（与原文件同目录，扩展名 .mp4）
                String transcodedKey = FileUtil.mainName(originKey) + ".mp4";
                cosManager.putObject(transcodedKey, tempTranscoded);
                // 转码产物同样设为私有读（防下载）
                cosManager.setObjectPrivate(transcodedKey);

                // 5. 更新 DB（url、format、size），再删除旧对象
                File update = new File();
                update.setId(fileId);
                update.setUrl(host + transcodedKey);
                update.setFormat("mp4");
                update.setSize(tempTranscoded.length());
                fileMapper.updateById(update);
                if (!originKey.equals(transcodedKey)) {
                    try {
                        cosManager.deleteObject(originKey);
                    } catch (Exception e) {
                        log.warn("原始视频对象删除失败, key={}", originKey, e);
                    }
                }
                log.info("视频转码完成, fileId={}, {} -> mp4, {}KB -> {}KB",
                        fileId, format, tempOrigin.length() / 1024, tempTranscoded.length() / 1024);
            } finally {
                if (tempOrigin != null && tempOrigin.exists()) {
                    tempOrigin.delete();
                }
                if (tempTranscoded != null && tempTranscoded.exists()) {
                    tempTranscoded.delete();
                }
            }
        } catch (Exception e) {
            // 转码失败不影响已成功的上传
            log.error("视频转码失败, fileId={}", fileId, e);
        }
    }
}
