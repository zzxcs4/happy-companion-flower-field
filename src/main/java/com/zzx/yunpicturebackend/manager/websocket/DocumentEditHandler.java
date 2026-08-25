package com.zzx.yunpicturebackend.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.zzx.yunpicturebackend.manager.CosManager;
import com.zzx.yunpicturebackend.model.entity.File;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.vo.UserVO;
import com.zzx.yunpicturebackend.service.FileService;
import com.zzx.yunpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档协同编辑 WebSocket 处理器
 * 后端不理解 Yjs 协议，仅做消息中继：
 * - 二进制消息：Yjs 文档更新，中继给同文档的其他客户端
 * - 文本消息：控制信令（加入/离开/保存/用户列表/状态同步）
 */
@Component
@Slf4j
public class DocumentEditHandler extends AbstractWebSocketHandler {

    @Resource
    private UserService userService;

    @Resource
    private FileService fileService;

    @Resource
    private CosManager cosManager;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 每个文档的 WebSocket 会话集合，key: fileId, value: 会话集合
     */
    private final Map<Long, Set<WebSocketSession>> documentSessions = new ConcurrentHashMap<>();

    /**
     * 每个文档的最新文本内容缓存（用于最后一个用户断开时保存到 COS）
     */
    private final Map<Long, String> contentBuffer = new ConcurrentHashMap<>();

    // ==================== 连接生命周期 ====================

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long fileId = (Long) session.getAttributes().get("fileId");
        User user = (User) session.getAttributes().get("user");

        // 加入文档会话组
        documentSessions.putIfAbsent(fileId, ConcurrentHashMap.newKeySet());
        documentSessions.get(fileId).add(session);

        // 广播用户加入消息
        JSONObject joinMsg = new JSONObject();
        joinMsg.set("type", "join");
        joinMsg.set("userId", user.getId());
        joinMsg.set("userName", user.getUserName());
        joinMsg.set("userAvatar", user.getUserAvatar());
        broadcastText(fileId, joinMsg.toString(), session);

        // 向全体在线用户广播最新用户列表（含新加入者），保证先到者也能看到后来者
        broadcastUserList(fileId);

        log.info("用户 {} 加入文档 {} 的协同编辑", user.getUserName(), fileId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long fileId = (Long) session.getAttributes().get("fileId");
        User user = (User) session.getAttributes().get("user");

        // 从文档会话组中移除
        Set<WebSocketSession> sessions = documentSessions.get(fileId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                documentSessions.remove(fileId);

                // 最后一个用户离开，保存内容到 COS
                String content = contentBuffer.remove(fileId);
                if (content != null) {
                    saveContentToCos(fileId, content);
                }
            }
        }

        // 广播用户离开消息，并刷新剩余用户的在线列表
        JSONObject leaveMsg = new JSONObject();
        leaveMsg.set("type", "leave");
        leaveMsg.set("userId", user.getId());
        leaveMsg.set("userName", user.getUserName());
        broadcastText(fileId, leaveMsg.toString(), null);
        broadcastUserList(fileId);

        log.info("用户 {} 离开文档 {} 的协同编辑", user.getUserName(), fileId);
    }

    // ==================== 消息处理 ====================

    /**
     * 处理二进制消息 —— Yjs 文档更新，直接中继给其他客户端
     */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        Long fileId = (Long) session.getAttributes().get("fileId");
        // 中继给同一文档的其他客户端
        broadcastBinary(fileId, message, session);
    }

    /**
     * 处理文本消息 —— 控制信令
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long fileId = (Long) session.getAttributes().get("fileId");
        User user = (User) session.getAttributes().get("user");

        JSONObject msg = JSONUtil.parseObj(message.getPayload());
        String type = msg.getStr("type");

        if (type == null) return;

        switch (type) {
            case "save":
                // 客户端定期保存文档内容到后端缓存
                String content = msg.getStr("content");
                if (content != null) {
                    contentBuffer.put(fileId, content);
                }
                break;
            case "state_request":
                // 新加入的客户端请求当前文档状态：附上请求者 userId 后中继，让已有客户端定向回复；
                // 前端未同步成功前会定时重发，兜底首次广播丢失的时序问题
                msg.set("fromUserId", user.getId());
                broadcastText(fileId, msg.toString(), session);
                break;
            case "state_response":
                // 已有客户端回复文档状态给新客户端
                // 中继给请求者（targetUserId）
                Long targetUserId = msg.getLong("targetUserId");
                if (targetUserId != null) {
                    sendToUser(fileId, targetUserId, message.getPayload());
                }
                break;
            default:
                log.warn("未知的消息类型: {}", type);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 向文档的全体在线用户广播最新用户列表（加入/离开时调用）
     */
    private void broadcastUserList(Long fileId) {
        Set<WebSocketSession> sessions = documentSessions.get(fileId);
        if (CollUtil.isEmpty(sessions)) return;
        for (WebSocketSession s : sessions) {
            if (!s.isOpen()) continue;
            try {
                sendUserList(fileId, s);
            } catch (Exception e) {
                log.warn("广播用户列表失败, fileId = {}", fileId, e);
            }
        }
    }

    /**
     * 发送当前文档的在线用户列表
     */
    private void sendUserList(Long fileId, WebSocketSession targetSession) throws Exception {
        Set<WebSocketSession> sessions = documentSessions.get(fileId);
        if (CollUtil.isEmpty(sessions)) return;

        List<JSONObject> users = new ArrayList<>();
        for (WebSocketSession s : sessions) {
            if (!s.isOpen()) continue;
            User u = (User) s.getAttributes().get("user");
            if (u == null) continue;
            JSONObject userObj = new JSONObject();
            userObj.set("userId", u.getId());
            userObj.set("userName", u.getUserName());
            userObj.set("userAvatar", u.getUserAvatar());
            users.add(userObj);
        }

        JSONObject userListMsg = new JSONObject();
        userListMsg.set("type", "user_list");
        userListMsg.set("users", users);
        targetSession.sendMessage(new TextMessage(userListMsg.toString()));
    }

    /**
     * 发送消息给指定用户（通过 userId 查找 session）
     */
    private void sendToUser(Long fileId, Long targetUserId, String payload) throws Exception {
        Set<WebSocketSession> sessions = documentSessions.get(fileId);
        if (CollUtil.isEmpty(sessions)) return;

        for (WebSocketSession s : sessions) {
            if (!s.isOpen()) continue;
            User u = (User) s.getAttributes().get("user");
            if (u != null && u.getId().equals(targetUserId)) {
                s.sendMessage(new TextMessage(payload));
                return;
            }
        }
    }

    /**
     * 广播二进制消息给同文档的其他客户端
     */
    private void broadcastBinary(Long fileId, BinaryMessage message, WebSocketSession excludeSession) throws Exception {
        Set<WebSocketSession> sessions = documentSessions.get(fileId);
        if (CollUtil.isEmpty(sessions)) return;

        for (WebSocketSession s : sessions) {
            if (excludeSession != null && excludeSession.equals(s)) continue;
            if (s.isOpen()) {
                s.sendMessage(message);
            }
        }
    }

    /**
     * 广播文本消息给同文档的所有客户端（或排除某个 session）
     */
    private void broadcastText(Long fileId, String payload, WebSocketSession excludeSession) throws Exception {
        Set<WebSocketSession> sessions = documentSessions.get(fileId);
        if (CollUtil.isEmpty(sessions)) return;

        TextMessage textMessage = new TextMessage(payload);
        for (WebSocketSession s : sessions) {
            if (excludeSession != null && excludeSession.equals(s)) continue;
            if (s.isOpen()) {
                s.sendMessage(textMessage);
            }
        }
    }

    /**
     * 保存文本内容到 COS（覆盖原文件）
     */
    private void saveContentToCos(Long fileId, String content) {
        try {
            File file = fileService.getById(fileId);
            if (file == null || file.getUrl() == null) return;

            // 从完整 URL 中提取 COS key
            String url = file.getUrl();
            String host = cosManager.getCosClientConfig().getHost();
            String key = url.startsWith(host) ? url.substring(host.length()) : url;

            // 写入临时文件并上传到 COS
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            java.io.File tempFile = java.io.File.createTempFile("doc-edit-", ".tmp");
            try {
                java.nio.file.Files.write(tempFile.toPath(), bytes);
                cosManager.putObject(key, tempFile);
                log.info("文档 {} 内容已保存到 COS: {}", fileId, key);
            } finally {
                tempFile.delete();
            }
        } catch (Exception e) {
            log.error("保存文档内容到 COS 失败, fileId = {}", fileId, e);
        }
    }
}
