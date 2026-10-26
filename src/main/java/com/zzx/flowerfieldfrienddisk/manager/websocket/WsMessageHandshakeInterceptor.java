package com.zzx.flowerfieldfrienddisk.manager.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Component
public class WsMessageHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * 握手前执行：验证用户身份并传递userId
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 1. 从请求中获取用户身份（示例：从HttpSession中获取已登录用户ID）
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            // 假设登录时已将userId存入session
            String userId = servletRequest.getParameter("userId");
            if (userId == null) {
                // 用户未登录，拒绝握手
                return false;
            }
            // 2. 将userId存入WebSocket会话属性，供Handler使用
            attributes.put("userId", userId);
        }
        return true; // 允许握手
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后执行（可选）

    }
}
