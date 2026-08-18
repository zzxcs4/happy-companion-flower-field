package com.zzx.yunpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

/**
 * 视频缩略图生成管理器
 */
@Slf4j
@Component
public class VideoThumbnailManager {

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${ffprobe.path:ffprobe}")
    private String ffprobePath;

    private FFmpeg ffmpeg;
    private FFprobe ffprobe;
    private boolean available = false;

    @PostConstruct
    public void init() {
        try {
            ffmpeg = new FFmpeg(ffmpegPath);
            ffprobe = new FFprobe(ffprobePath);
            available = true;
            log.info("FFmpeg initialized successfully");
        } catch (IOException e) {
            log.warn("FFmpeg not found, thumbnail generation will be skipped: {}", e.getMessage());
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * 从视频文件生成缩略图
     * @param videoFile 视频文件
     * @param outputPath 输出缩略图路径
     * @param timeOffset 提取时间点（秒），默认第1秒
     * @return 缩略图文件
     */
    public File generateThumbnail(File videoFile, String outputPath, int timeOffset) throws IOException {
        if (!videoFile.exists()) {
            throw new IllegalArgumentException("Video file does not exist: " + videoFile.getAbsolutePath());
        }

        File outputFile = new File(outputPath);
        FileUtil.mkParentDirs(outputFile);

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(videoFile.getAbsolutePath())
                .overrideOutputFiles(true)
                .addOutput(outputPath)
                .setFrames(1)
                .setVideoFilter("scale=640:-1")
                .setStartOffset(timeOffset, java.util.concurrent.TimeUnit.SECONDS)
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();

        log.info("Generated thumbnail for video: {} at {}", videoFile.getName(), outputPath);
        return outputFile;
    }

    /**
     * 从视频文件生成缩略图（默认第1秒）
     */
    public File generateThumbnail(File videoFile, String outputPath) throws IOException {
        return generateThumbnail(videoFile, outputPath, 1);
    }
}
