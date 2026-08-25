package com.zzx.yunpicturebackend.api.siliconflow;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zzx.yunpicturebackend.exception.BusinessException;
import com.zzx.yunpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 硅基流动 ASR（语音转文字）API 服务
 * 使用 FunAudioLLM/SenseVoiceSmall 模型
 */
@Slf4j
@Component
public class SiliconFlowAsrService {

    @Value("${siliconFlow.apiKey}")
    private String apiKey;

    /**
     * 硅基流动语音转文字接口地址
     */
    private static final String ASR_URL = "https://api.siliconflow.cn/v1/audio/transcriptions";

    /**
     * ASR 模型名称
     */
    private static final String ASR_MODEL = "FunAudioLLM/SenseVoiceSmall";

    /**
     * 支持的音频格式
     */
    private static final List<String> SUPPORTED_AUDIO_FORMATS = Arrays.asList(
            "mp3", "wav", "flac", "m4a", "ogg", "wma", "aac"
    );

    /**
     * 判断文件格式是否支持 ASR
     */
    public boolean isSupportedFormat(String format) {
        if (format == null) {
            return false;
        }
        return SUPPORTED_AUDIO_FORMATS.contains(format.toLowerCase());
    }

    /**
     * 音频文件转文字
     *
     * @param audioFile 音频临时文件
     * @param fileName  原始文件名（用于日志）
     * @return 转录后的文本
     */
    public String transcribe(File audioFile, String fileName) {
        if (audioFile == null || !audioFile.exists()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "音频文件不存在");
        }

        // 文件大小限制：50MB
        long fileSize = audioFile.length();
        if (fileSize > 50 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "音频文件超过 50MB 限制，无法转写");
        }

        log.info("开始 ASR 转写, fileName={}, size={}KB", fileName, fileSize / 1024);

        try (HttpResponse response = HttpRequest.post(ASR_URL)
                .header("Authorization", "Bearer " + apiKey)
                .form("file", audioFile)
                .form("model", ASR_MODEL)
                .timeout(120000) // 2 分钟超时（音频转写可能较慢）
                .execute()) {

            if (!response.isOk()) {
                log.error("SiliconFlow ASR 请求失败, status={}, body={}", response.getStatus(), response.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "语音转文字服务调用失败");
            }

            JSONObject responseBody = JSONUtil.parseObj(response.body());
            String text = responseBody.getStr("text");

            if (text == null || text.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未能识别到语音内容");
            }

            log.info("ASR 转写完成, fileName={}, textLength={}", fileName, text.length());
            return text.trim();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("SiliconFlow ASR 调用异常, fileName={}", fileName, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "语音转文字服务异常");
        }
    }
}
