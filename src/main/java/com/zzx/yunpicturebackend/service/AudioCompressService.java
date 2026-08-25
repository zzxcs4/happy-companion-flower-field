package com.zzx.yunpicturebackend.service;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.zzx.yunpicturebackend.config.CosClientConfig;
import com.zzx.yunpicturebackend.manager.CosManager;
import com.zzx.yunpicturebackend.manager.VideoAiManager;
import com.zzx.yunpicturebackend.mapper.FileMapper;
import com.zzx.yunpicturebackend.model.entity.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * 音频压缩服务
 * 音频上传成功后，异步用 FFmpeg 转码为 MP3 128kbps，降低播放与下载流量。
 * 说明：图片压缩用的是数据万象上传时同步的 PicOperations（图片处理），
 * 而音频转码在数据万象中属于异步媒体处理任务（需开通服务、轮询/回调），链路过重；
 * 本项目已依赖 FFmpeg（视频缩略图、AI 摘要），因此直接复用。
 * 压缩为事后增强，任何失败都不影响已成功的上传。
 */
@Slf4j
@Service
public class AudioCompressService {

    /**
     * 支持压缩的音频格式（与前端 isAudio 保持一致）
     */
    private static final List<String> AUDIO_FORMATS = Arrays.asList(
            "mp3", "wav", "flac", "m4a", "ogg", "wma", "aac"
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
     * 判断文件格式是否为音频
     */
    public boolean isAudioFormat(String format) {
        return format != null && AUDIO_FORMATS.contains(format.toLowerCase());
    }

    /**
     * 异步压缩音频：从 COS 下载 → FFmpeg 转码为 MP3 128kbps → 上传替换原对象（更新 DB 的 url/format/size）
     */
    @Async
    public void compressAsync(Long fileId) {
        try {
            File file = fileMapper.selectById(fileId);
            if (file == null || !isAudioFormat(file.getFormat())) {
                return;
            }
            // FFmpeg 不可用时优雅降级，保留原文件
            if (!videoAiManager.isAvailable()) {
                log.info("FFmpeg 不可用，跳过音频压缩, fileId={}", fileId);
                return;
            }
            long originSize = file.getSize() == null ? 0 : file.getSize();
            String format = file.getFormat().toLowerCase();
            // 小文件，以及已经足够小的 mp3，压缩收益低，跳过
            if (originSize < 512 * 1024L || ("mp3".equals(format) && originSize <= 2 * 1024 * 1024L)) {
                return;
            }

            String url = file.getUrl();
            String host = cosClientConfig.getHost();
            String originKey = url.startsWith(host) ? url.substring(host.length()) : url;

            java.io.File tempOrigin = null;
            java.io.File tempCompressed = null;
            try {
                // 1. 从 COS 下载原始音频
                tempOrigin = java.io.File.createTempFile("audio-origin-", "." + format);
                COSObject cosObject = cosManager.getObject(originKey);
                try (COSObjectInputStream cosInput = cosObject.getObjectContent()) {
                    java.nio.file.Files.copy(cosInput, tempOrigin.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                // 2. FFmpeg 转码为 MP3 128kbps
                tempCompressed = java.io.File.createTempFile("audio-compress-", ".mp3");
                videoAiManager.compressAudio(tempOrigin, tempCompressed.getAbsolutePath());

                // 3. 压缩无收益（体积未减小），保留原文件
                if (tempCompressed.length() >= tempOrigin.length()) {
                    log.info("音频压缩无收益，保留原文件, fileId={}, origin={}KB, compressed={}KB",
                            fileId, tempOrigin.length() / 1024, tempCompressed.length() / 1024);
                    return;
                }

                // 4. 上传压缩版（与原文件同目录，扩展名 .mp3）
                String compressedKey = FileUtil.mainName(originKey) + ".mp3";
                cosManager.putObject(compressedKey, tempCompressed);

                // 5. 更新 DB（url、format、size），再删除旧对象
                File update = new File();
                update.setId(fileId);
                update.setUrl(host + compressedKey);
                update.setFormat("mp3");
                update.setSize(tempCompressed.length());
                fileMapper.updateById(update);
                if (!originKey.equals(compressedKey)) {
                    try {
                        cosManager.deleteObject(originKey);
                    } catch (Exception e) {
                        log.warn("原始音频对象删除失败, key={}", originKey, e);
                    }
                }
                log.info("音频压缩完成, fileId={}, {}KB -> {}KB", fileId, tempOrigin.length() / 1024, tempCompressed.length() / 1024);
            } finally {
                if (tempOrigin != null && tempOrigin.exists()) {
                    tempOrigin.delete();
                }
                if (tempCompressed != null && tempCompressed.exists()) {
                    tempCompressed.delete();
                }
            }
        } catch (Exception e) {
            // 压缩失败不影响已成功的上传
            log.error("音频压缩失败, fileId={}", fileId, e);
        }
    }
}
