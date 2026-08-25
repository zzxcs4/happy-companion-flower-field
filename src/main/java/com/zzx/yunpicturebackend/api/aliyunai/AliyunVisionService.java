package com.zzx.yunpicturebackend.api.aliyunai;

import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zzx.yunpicturebackend.exception.BusinessException;
import com.zzx.yunpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 阿里云百炼视觉理解服务
 * 使用 qwen3.8-max 原生多模态模型，通过 DashScope OpenAI 兼容接口传图片 URL 识图
 */
@Slf4j
@Component
public class AliyunVisionService {

    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    /**
     * DashScope OpenAI 兼容接口地址（与 LlmApiService 一致）
     */
    private static final String CHAT_COMPLETION_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    /**
     * 视觉理解模型名称
     */
    private static final String VISION_MODEL = "qwen3.8-max";

    /**
     * 系统提示词：识图结果生成
     */
    private static final String SUMMARY_SYSTEM_PROMPT =
            "你是一个专业的图片识别助手。用户会给你一张图片，你需要生成一段简洁、准确的中文识别结果。\n" +
                    "要求：\n" +
                    "1. 识别结果控制在 20-100 字以内\n" +
                    "2. 抓住图片的核心内容和关键信息\n" +
                    "3. 使用客观、专业的语言\n" +
                    "4. 直接输出摘要内容，不要加任何前缀说明";

    /**
     * 支持的图片格式
     */
    private static final List<String> SUPPORTED_IMAGE_FORMATS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp"
    );

    /**
     * 判断文件格式是否支持图片识别
     */
    public boolean isSupportedFormat(String format) {
        if (format == null) {
            return false;
        }
        return SUPPORTED_IMAGE_FORMATS.contains(format.toLowerCase());
    }

    /**
     * 识别图片内容（视觉理解）
     *
     * @param imageUrl 图片的可公网访问 URL
     * @return 识别出的文本内容
     */
    public String recognizeImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片 URL 不能为空");
        }

        // 构造 chat completions 请求体：user 消息的 content 为多模态数组（图片 + 文本指令）
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", VISION_MODEL);

        JSONArray messages = new JSONArray();
        // 系统消息（识图结果约束）
        JSONObject systemMsg = new JSONObject();
        systemMsg.set("role", "system");
        systemMsg.set("content", SUMMARY_SYSTEM_PROMPT);
        messages.add(systemMsg);

        // 用户消息：image_url 传图片 + text 指令
        JSONArray content = new JSONArray();
        JSONObject imagePart = new JSONObject();
        imagePart.set("type", "image_url");
        imagePart.set("image_url", new JSONObject().set("url", imageUrl));
        content.add(imagePart);
        JSONObject textPart = new JSONObject();
        textPart.set("type", "text");
        textPart.set("text", "请识别并描述这张图片的内容");
        content.add(textPart);

        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", content);
        messages.add(userMsg);

        requestBody.set("messages", messages);

        try (HttpResponse response = HttpRequest.post(CHAT_COMPLETION_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(requestBody))
                .timeout(120000)
                .execute()) {

            if (!response.isOk()) {
                log.error("图片识别 API 请求失败，status={}, body={}", response.getStatus(), response.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片识别服务调用失败");
            }

            JSONObject responseBody = JSONUtil.parseObj(response.body());
            JSONArray choices = responseBody.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("图片识别 API 响应异常，body={}", response.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片识别响应异常");
            }

            String result = choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getStr("content");

            if (result == null || result.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未识别到图片内容");
            }

            return result.trim();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("图片识别调用异常, imageUrl={}", imageUrl, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片识别服务调用失败");
        }
    }
}
