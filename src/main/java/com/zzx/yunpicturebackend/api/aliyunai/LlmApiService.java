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

/**
 * 通义千问 LLM API 调用服务
 * 使用阿里云 DashScope OpenAI 兼容接口
 */
@Slf4j
@Component
public class LlmApiService {

    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    /**
     * DashScope OpenAI 兼容接口地址
     */
    private static final String CHAT_COMPLETION_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    /**
     * 默认模型
     */
    private static final String DEFAULT_MODEL = "deepseek-v4-flash-0731";

    /**
     * 系统提示词：文档摘要生成
     */
    private static final String SUMMARY_SYSTEM_PROMPT =
            "你是一个专业的文档摘要助手。用户会给你一段从文档中提取的文本，你需要生成一段简洁、准确的中文摘要。\n" +
            "要求：\n" +
            "1. 摘要控制在 100-200 字以内\n" +
            "2. 抓住文档的核心内容和关键信息\n" +
            "3. 使用客观、专业的语言\n" +
            "4. 直接输出摘要内容，不要加任何前缀说明";

    /**
     * 为文档内容生成 AI 摘要
     *
     * @param documentText 从文档中提取的纯文本
     * @return 生成的摘要文本
     */
    public String generateSummary(String documentText) {
        if (documentText == null || documentText.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文档内容为空，无法生成摘要");
        }

        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", DEFAULT_MODEL);

        JSONArray messages = new JSONArray();
        // 系统消息
        JSONObject systemMsg = new JSONObject();
        systemMsg.set("role", "system");
        systemMsg.set("content", SUMMARY_SYSTEM_PROMPT);
        messages.add(systemMsg);
        // 用户消息（文档内容）
        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", "请为以下文档内容生成摘要：\n\n" + documentText);
        messages.add(userMsg);

        requestBody.set("messages", messages);

        // 发送请求
        try (HttpResponse response = HttpRequest.post(CHAT_COMPLETION_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(requestBody))
                .timeout(30000) // 30 秒超时
                .execute()) {

            if (!response.isOk()) {
                log.error("LLM API 请求失败，status={}, body={}", response.getStatus(), response.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 摘要生成失败");
            }

            // 解析响应
            JSONObject responseBody = JSONUtil.parseObj(response.body());
            JSONArray choices = responseBody.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("LLM API 响应异常，body={}", response.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 摘要响应异常");
            }

            String summary = choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getStr("content");

            if (summary == null || summary.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 摘要内容为空");
            }

            return summary.trim();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM API 调用异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 摘要服务调用失败");
        }
    }

    /**
     * 为视频内容生成 AI 摘要（基于 ASR 转写的语音文本 + 视频元信息）
     *
     * @param transcript       视频语音转写的文本
     * @param durationSeconds  视频时长（秒）
     * @return 生成的摘要文本
     */
    public String generateVideoSummary(String transcript, long durationSeconds) {
        if (transcript == null || transcript.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "视频语音内容为空，无法生成摘要");
        }

        // 拼接视频元信息 + 转写文本，复用文档摘要提示词
        String meta = String.format("【视频元信息】时长约 %d 分 %d 秒\n【视频语音转写内容如下】\n",
                durationSeconds / 60, durationSeconds % 60);

        JSONObject requestBody = new JSONObject();
        requestBody.set("model", DEFAULT_MODEL);

        JSONArray messages = new JSONArray();
        JSONObject systemMsg = new JSONObject();
        systemMsg.set("role", "system");
        systemMsg.set("content", SUMMARY_SYSTEM_PROMPT);
        messages.add(systemMsg);
        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", "请为以下视频的语音转写内容生成摘要：\n\n" + meta + transcript);
        messages.add(userMsg);

        requestBody.set("messages", messages);

        try (HttpResponse response = HttpRequest.post(CHAT_COMPLETION_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(requestBody))
                .timeout(30000)
                .execute()) {

            if (!response.isOk()) {
                log.error("LLM API 请求失败，status={}, body={}", response.getStatus(), response.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "视频 AI 摘要生成失败");
            }

            JSONObject responseBody = JSONUtil.parseObj(response.body());
            JSONArray choices = responseBody.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "视频 AI 摘要响应异常");
            }

            String summary = choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getStr("content");

            if (summary == null || summary.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "视频 AI 摘要内容为空");
            }

            return summary.trim();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM API 调用异常（视频摘要）", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "视频 AI 摘要服务调用失败");
        }
    }
}
