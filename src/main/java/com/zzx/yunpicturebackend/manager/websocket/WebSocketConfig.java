package com.zzx.yunpicturebackend.manager.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import javax.annotation.Resource;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private PictureEditHandler pictureEditHandler;
    @Resource
    private PrivateMessageHandler privateMessageHandler; // 新增的私信处理器
    @Resource
    private DocumentEditHandler documentEditHandler; // 文档协同编辑处理器
    @Resource
    private WsHandshakeInterceptor wsHandshakeInterceptor;
    @Resource
    private WsMessageHandshakeInterceptor wsMessageHandshakeInterceptor;
    @Resource
    private DocumentEditHandshakeInterceptor documentEditHandshakeInterceptor; // 文档协同编辑握手拦截器
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
        // 3. 文档协同编辑 WebSocket
        registry.addHandler(documentEditHandler, "/ws/document/edit")
                .addInterceptors(documentEditHandshakeInterceptor)
                .setAllowedOrigins("*");
    }

    /**
     * WebSocket 容器配置：默认文本/二进制消息缓冲仅 8KB，
     * 协同编辑的状态同步（state_response）与批量保存消息会超限导致断连，这里提高到 1MB；
     * 空闲超时置 0（不主动断开，前端定时 save 消息充当心跳）
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(1024 * 1024);
        container.setMaxBinaryMessageBufferSize(1024 * 1024);
        container.setMaxSessionIdleTimeout(0L);
        return container;
    }
}
