package com.zzx.flowerfieldfrienddisk.manager;

import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

/**
 * 视频 AI 处理管理器
 * 负责视频摘要生成前的预处理：音频轨道提取、时长探测
 */
@Slf4j
@Component
public class VideoAiManager {

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
            log.info("VideoAiManager FFmpeg initialized successfully");
        } catch (IOException e) {
            log.warn("FFmpeg not found, video AI features will be unavailable: {}", e.getMessage());
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * 探测视频时长（秒）
     *
     * @param videoFile 视频文件
     * @return 时长（秒），探测失败返回 0
     */
    public long probeDuration(File videoFile) {
        if (!available) {
            return 0;
        }
        try {
            FFmpegProbeResult result = ffprobe.probe(videoFile.getAbsolutePath());
            if (result != null && result.getFormat() != null) {
                double duration = result.getFormat().duration;
                return (long) duration;
            }
            return 0;
        } catch (Exception e) {
            log.error("探测视频时长失败: {}", videoFile.getName(), e);
            return 0;
        }
    }

    /**
     * 提取视频音频轨道（转为 16kHz 单声道 mp3，适配 ASR 模型）
     *
     * @param videoFile  视频文件
     * @param outputPath 输出音频文件路径
     * @return 提取出的音频文件
     */
    public File extractAudio(File videoFile, String outputPath) throws IOException {
        if (!available) {
            throw new IOException("FFmpeg 未安装或不可用，无法提取音频");
        }
        if (!videoFile.exists()) {
            throw new IllegalArgumentException("视频文件不存在: " + videoFile.getAbsolutePath());
        }

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(videoFile.getAbsolutePath())
                .overrideOutputFiles(true)
                .addOutput(outputPath)
                .disableVideo()                    // -vn 丢弃视频流
                .setAudioCodec("libmp3lame")       // mp3 编码
                .setAudioSampleRate(16000)         // 16kHz 采样率（ASR 友好）
                .setAudioChannels(1)               // 单声道
                .setAudioBitRate(64_000)           // 64kbps，压缩体积
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();

        File outputFile = new File(outputPath);
        log.info("视频音频提取完成: {} -> {} ({}KB)", videoFile.getName(), outputPath, outputFile.length() / 1024);
        return outputFile;
    }

    /**
     * 压缩音频：转码为 MP3 128kbps / 44.1kHz / 双声道（兼顾音质与体积，浏览器兼容性最佳）
     *
     * @param audioFile  原始音频文件
     * @param outputPath 输出文件路径（.mp3）
     * @return 压缩后的音频文件
     */
    public File compressAudio(File audioFile, String outputPath) throws IOException {
        if (!available) {
            throw new IOException("FFmpeg 未安装或不可用，无法压缩音频");
        }
        if (!audioFile.exists()) {
            throw new IllegalArgumentException("音频文件不存在: " + audioFile.getAbsolutePath());
        }

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(audioFile.getAbsolutePath())
                .overrideOutputFiles(true)
                .addOutput(outputPath)
                .disableVideo()                    // -vn 丢弃可能存在的封面图等视频流
                .setAudioCodec("libmp3lame")       // mp3 编码
                .setAudioSampleRate(44100)         // 44.1kHz 标准采样率
                .setAudioChannels(2)               // 双声道
                .setAudioBitRate(128_000)          // 128kbps，音质与体积平衡
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();

        File outputFile = new File(outputPath);
        log.info("音频压缩完成: {} -> {} ({}KB)", audioFile.getName(), outputPath, outputFile.length() / 1024);
        return outputFile;
    }

    /**
     * 视频转码：统一转为 H.264 编码的 MP4（浏览器兼容性最佳）
     * CRF 23 兼顾画质与体积，veryfast 预设控制服务器 CPU 耗时，
     * faststart 将 moov atom 前置，支持边下边播、秒开首帧。
     *
     * @param videoFile  原始视频文件
     * @param outputPath 输出文件路径（.mp4）
     * @return 转码后的视频文件
     */
    public File transcodeToMp4(File videoFile, String outputPath) throws IOException {
        if (!available) {
            throw new IOException("FFmpeg 未安装或不可用，无法转码视频");
        }
        if (!videoFile.exists()) {
            throw new IllegalArgumentException("视频文件不存在: " + videoFile.getAbsolutePath());
        }

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(videoFile.getAbsolutePath())
                .overrideOutputFiles(true)
                .addOutput(outputPath)
                .setFormat("mp4")
                .setVideoCodec("libx264")          // H.264 编码
                .setAudioCodec("aac")              // AAC 音频
                .setAudioBitRate(128_000)          // 128kbps
                .addExtraArgs("-preset", "veryfast", "-crf", "23", "-movflags", "+faststart")
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();

        File outputFile = new File(outputPath);
        log.info("视频转码完成: {} -> {} ({}KB)", videoFile.getName(), outputPath, outputFile.length() / 1024);
        return outputFile;
    }
}
