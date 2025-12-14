package com.zzx.yunpicturebackend.manager.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private PictureEditHandler pictureEditHandler;
    @Resource
    private PrivateMessageHandler privateMessageHandler; // 新增的私信处理器
    @Resource
    private WsHandshakeInterceptor wsHandshakeInterceptor;
    @Resource
    private WsMessageHandshakeInterceptor wsMessageHandshakeInterceptor;
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // websocket
        registry.addHandler(pictureEditHandler, "/ws/picture/edit")
                .addInterceptors(wsHandshakeInterceptor)
                .setAllowedOrigins("*");
        // 2. 新增的私信功能WebSocket
        registry.addHandler(privateMessageHandler, "/ws/private/message")
                .addInterceptors(wsMessageHandshakeInterceptor) // 复用握手拦截器（用于身份验证）
                .setAllowedOrigins("*");
    }
}
