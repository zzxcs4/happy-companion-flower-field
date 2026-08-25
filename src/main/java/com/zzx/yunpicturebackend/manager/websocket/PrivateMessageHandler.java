package com.zzx.yunpicturebackend.manager.websocket;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.manager.websocket.model.ChatList;
import com.zzx.yunpicturebackend.manager.websocket.model.Message;
import com.zzx.yunpicturebackend.manager.websocket.model.PrivateMessageRequestMessage;
import com.zzx.yunpicturebackend.manager.websocket.model.PrivateMessageResponseMessage;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.service.ChatListService;
import com.zzx.yunpicturebackend.service.IFollowService;
import com.zzx.yunpicturebackend.service.UserLinkService;
import com.zzx.yunpicturebackend.service.UserService;
import io.netty.util.internal.StringUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 私信功能的 WebSocket 处理器
 */
@Component
public class PrivateMessageHandler extends TextWebSocketHandler {
    @Resource
    private UserService userService;
    @Resource
    private OnlineUserManager onlineUserManager;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private UserLinkService userLinkService;
    @Resource
    private ChatListService chatListService;
    @Resource
    private IFollowService followService;

    // 存储用户ID与WebSocketSession的映射（线程安全）
    private static final Map<String, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();

    /**
     * 连接建立时触发
     */
    /**
     * 连接建立时触发
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        PrivateMessageResponseMessage responseMessage = new PrivateMessageResponseMessage();
        // 1. 获取用户ID并验证登录状态
        String userIdStr = (String) session.getAttributes().get("userId");
        if (userIdStr == null || userIdStr.isEmpty()) {
            responseMessage.setStatus(-1);
            responseMessage.setMessage("未获取到用户ID，连接失败");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(responseMessage)));
            return;
        }

        Long userId = Long.parseLong(userIdStr);
        User user = userService.getById(userId);
        if (user == null) {
            responseMessage.setStatus(-1);
            responseMessage.setMessage("用户不存在");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(responseMessage)));
            return;
        }

        // 2. 检查重复登录
        if (onlineUserManager.getState(user.getId()) != null) {
            responseMessage.setStatus(-1);
            responseMessage.setMessage("当前用户已经登录了, 不要重复登录");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(responseMessage)));
            return;
        }

        // 3. 存储用户会话并设置在线状态
        userSessionMap.put(userIdStr, session);
        onlineUserManager.enterHall(user.getId(), session);

        // 4. 广播用户上线状态
        broadcastUserStatusChange(userId, true);

        // 5. 响应用户列表（包含在线状态）
        responseMessage.setStatus(1);
        responseMessage.setMessage("getUser");
        responseMessage.setFromusername(user.getUserName());
        responseMessage.setCurrentUser(user);

        // 获取用户列表并设置在线状态
        List<User> userList = getAllChatUsersWithOnlineStatus(userId);
        responseMessage.setUsers(userList);

        // 6. 返回响应
        session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(responseMessage)));
    }

    /**
     * 获取用户在线状态
     */
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = onlineUserManager.getState(userId);
        return session != null && session.isOpen();
    }

    /**
     * 广播用户状态变化
     */
    private void broadcastUserStatusChange(Long userId, boolean isOnline) throws Exception {
        // 获取所有与该用户有聊天记录的用户
        List<Long> relatedUserIds = userLinkService.getChatUserIds(userId);

        for (Long relatedUserId : relatedUserIds) {
            WebSocketSession session = onlineUserManager.getState(relatedUserId);
            if (session != null && session.isOpen()) {
                PrivateMessageResponseMessage statusMessage = new PrivateMessageResponseMessage();
                statusMessage.setStatus(1);
                statusMessage.setMessage("userStatusChange");
                statusMessage.setFromUserId(userId.toString());
                statusMessage.setOnline(isOnline);

                session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(statusMessage)));
            }
        }
    }
    /**
     * 收到客户端消息时触发（处理私信逻辑）
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        PrivateMessageResponseMessage responseMessage = new PrivateMessageResponseMessage();
        String userIdStr = (String) session.getAttributes().get("userId");
        // 添加详细日志
        System.out.println("=== 收到 WebSocket 消息 ===");
        System.out.println("用户ID: " + userIdStr);
        System.out.println("消息内容: " + message.getPayload());
        System.out.println("=========================");
        if (userIdStr == null) {
            responseMessage.setStatus(-1);
            responseMessage.setMessage("用户未登录");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(responseMessage)));
            return;
        }

        Long userId = Long.parseLong(userIdStr);
        User user = userService.getById(userId);
        if (user == null) {
            responseMessage.setStatus(-1);
            responseMessage.setMessage("用户不存在");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(responseMessage)));
            return;
        }

        // 1. 解析请求内容
        String payload = message.getPayload();
        System.out.println("收到消息：" + payload);
        PrivateMessageRequestMessage requestMessage = objectMapper.readValue(payload, PrivateMessageRequestMessage.class);
// 添加消息类型日志
        System.out.println("解析后的消息类型: " + requestMessage.getMessage());
        System.out.println("targetUserId: " + requestMessage.getTargetUserId());
        // 2. 处理不同类型的消息
        switch (requestMessage.getMessage()) {
            case "loadMessage":
                handleLoadMessage(session, requestMessage, userId);
                break;
            case "sendMessage":
                handleSendMessage(session, requestMessage, userId);
                break;
            case "getTargetUser":
                handleGetTargetUser(session, requestMessage);
                break;
            default:
                responseMessage.setStatus(0);
                responseMessage.setMessage("未知消息类型");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(responseMessage)));
        }
    }

    /**
     * 处理加载历史消息请求
     */
    private void handleLoadMessage(WebSocketSession session, PrivateMessageRequestMessage requestMessage, Long userId) throws Exception {
        PrivateMessageResponseMessage responseMessage = new PrivateMessageResponseMessage();
        responseMessage.setMessage("loadMessage");

        try {
            Long toId = Long.parseLong(requestMessage.getTo());
            Long min = Math.min(userId, toId);
            Long max = Math.max(userId, toId);
            Integer linkId = userLinkService.selectLinkId(min, max);

            if (linkId == null || linkId == 0) {
                responseMessage.setStatus(1);
                responseMessage.setMessages(null);
            } else {
                List<ChatList> chatLists = chatListService.selectChat(linkId);
                List<Message> messages = new ArrayList<>();
                for (ChatList chatList : chatLists) {
                    Message msg = new Message();
                    msg.setMessage(chatList.getContent());
                    msg.setUserId(chatList.getUserId());
                    msg.setSender(chatList.getUserId().equals(userId));
                    msg.setCreateTime(chatList.getCreateTime());
                    messages.add(msg);
                }
                responseMessage.setStatus(1);
                responseMessage.setMessages(messages);
            }
            session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(responseMessage)));
        } catch (Exception e) {
            responseMessage.setStatus(-1);
            responseMessage.setMessage("加载消息失败：" + e.getMessage());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(responseMessage)));
        }
    }

    /**
     * 处理发送消息请求
     */
    private void handleSendMessage(WebSocketSession session, PrivateMessageRequestMessage requestMessage, Long userId) throws Exception {
        PrivateMessageResponseMessage responseMessage = new PrivateMessageResponseMessage();
        responseMessage.setStatus(1);
        responseMessage.setMessage("sendMessage");

        try {
            Long toId = Long.parseLong(requestMessage.getTo());
            String content = requestMessage.getContent();

            // 检查接收方是否存在
            User targetUser = userService.getById(toId);
            if (targetUser == null) {
                responseMessage.setStatus(-1);
                responseMessage.setMessage("接收方用户不存在");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(responseMessage)));
                return;
            }

            // 确保聊天链路存在
            Long min = Math.min(userId, toId);
            Long max = Math.max(userId, toId);
            Integer linkId = userLinkService.selectLinkId(min, max);
            if (linkId == null || linkId == 0) {
                userLinkService.insertLink(min, max);
                linkId = userLinkService.selectLinkId(min, max);
            }

            // 存储消息到数据库（记录接收者 toId，初始未读，供未读红点轮询使用）
            Timestamp createTime = new Timestamp(System.currentTimeMillis());
            chatListService.insertChat(linkId, userId, content, createTime, toId);

            // 构建消息对象
            Message msg = new Message();
            msg.setMessage(content);
            msg.setUserId(userId);
            msg.setCreateTime(createTime);

            // 发送给发送方（自己）
            msg.setSender(true);
            List<Message> senderMsgList = new ArrayList<>();
            senderMsgList.add(msg);
            responseMessage.setMessages(senderMsgList);
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(responseMessage)));
            }

            // 推送消息给接收方（实时新消息通知）
            pushNewMessageToReceiver(toId, userId, content, createTime);

            // 新增：确保双方都在彼此的聊天列表中
            ensureUsersInEachOtherList(userId, toId);

        } catch (Exception e) {
            responseMessage.setStatus(-1);
            responseMessage.setMessage("发送消息失败：" + e.getMessage());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(responseMessage)));
        }
    }

    /**
     * 处理获取目标用户信息请求（支持非关注用户）
     */
    private void handleGetTargetUser(WebSocketSession session, PrivateMessageRequestMessage requestMessage) throws Exception {
        PrivateMessageResponseMessage responseMessage = new PrivateMessageResponseMessage();
        responseMessage.setMessage("getTargetUser");

        try {
            String targetUserIdStr = requestMessage.getTargetUserId();
            if (targetUserIdStr == null || targetUserIdStr.isEmpty()) {
                responseMessage.setStatus(-1);
                responseMessage.setMessage("目标用户ID不能为空");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(responseMessage)));
                return;
            }

            Long targetUserId = Long.parseLong(targetUserIdStr);
            User targetUser = userService.getById(targetUserId);

            if (targetUser == null) {
                responseMessage.setStatus(-1);
                responseMessage.setMessage("目标用户不存在");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(responseMessage)));
                return;
            }

            // 响应目标用户信息
            responseMessage.setStatus(1);
            responseMessage.setUserInfo(targetUser);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(responseMessage)));

        } catch (Exception e) {
            responseMessage.setStatus(-1);
            responseMessage.setMessage("获取用户信息失败：" + e.getMessage());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(responseMessage)));
        }
    }

    /**
     * 推送新消息给接收方
     */
    private void pushNewMessageToReceiver(Long toId, Long fromId, String content, Timestamp createTime) throws Exception {
        WebSocketSession receiverSession = onlineUserManager.getState(toId);
        if (receiverSession == null || !receiverSession.isOpen()) {
            System.out.println("用户 " + toId + " 不在线，无法实时推送消息");
            return;
        }

        PrivateMessageResponseMessage pushMessage = new PrivateMessageResponseMessage();
        pushMessage.setStatus(1);
        pushMessage.setMessage("newMessage");
        pushMessage.setFromUserId(fromId.toString());

        Message msg = new Message();
        msg.setMessage(content);
        msg.setUserId(fromId);
        msg.setSender(false);
        msg.setCreateTime(createTime);

        List<Message> msgList = new ArrayList<>();
        msgList.add(msg);
        pushMessage.setMessages(msgList);

        // 推送消息
        receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(pushMessage)));
        System.out.println("已向用户 " + toId + " 推送新消息");
    }

    /**
     * 新增：确保双方都在彼此的聊天列表中
     */
    private void ensureUsersInEachOtherList(Long user1Id, Long user2Id) throws Exception {
        // 获取双方用户信息
        User user1 = userService.getById(user1Id);
        User user2 = userService.getById(user2Id);

        if (user1 == null || user2 == null) {
            return;
        }

        // 将user2添加到user1的聊天列表
        addUserToChatList(user1Id, user2);

        // 将user1添加到user2的聊天列表
        addUserToChatList(user2Id, user1);
    }

    /**
     * 新增：将用户添加到指定用户的聊天列表
     */
    private void addUserToChatList(Long targetUserId, User userToAdd) throws Exception {
        WebSocketSession targetSession = onlineUserManager.getState(targetUserId);
        if (targetSession != null && targetSession.isOpen()) {
            PrivateMessageResponseMessage addUserMessage = new PrivateMessageResponseMessage();
            addUserMessage.setStatus(1);
            addUserMessage.setMessage("addUser");
            addUserMessage.setUserInfo(userToAdd);
            targetSession.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(addUserMessage)));
        }
    }


    /**
     * 连接关闭时触发
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userIdStr = (String) session.getAttributes().get("userId");
        if (userIdStr != null) {
            userSessionMap.remove(userIdStr);
            Long userId = Long.parseLong(userIdStr);
            User user = userService.getById(userId);
            if (user != null) {
                WebSocketSession webSocketSession = onlineUserManager.getState(user.getId());
                if (webSocketSession == session) {
                    onlineUserManager.exitHall(user.getId());
                    // 广播用户下线状态
                    broadcastUserStatusChange(userId, false);
                }
                System.out.println("用户 " + user.getUserName() + " 已断开连接");
            }
        }
    }
    /**
     * 连接异常时触发
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userIdStr = (String) session.getAttributes().get("userId");
        if (userIdStr != null) {
            Long userId = Long.parseLong(userIdStr);
            User user = userService.getById(userId);
            if (user != null) {
                WebSocketSession webSocketSession = onlineUserManager.getState(user.getId());
                if (webSocketSession == session) {
                    onlineUserManager.exitHall(user.getId());
                }
                System.out.println("用户 " + user.getUserName() + " 连接异常：" + exception.getMessage());
            }
        }
    }
    /**
     * 获取用户列表并包含在线状态
     */
    private List<User> getAllChatUsersWithOnlineStatus(Long userId) {
        List<Long> chatUserIds = userLinkService.getChatUserIds(userId);
        List<User> userList = new ArrayList<>();

        for (Long chatUserId : chatUserIds) {
            User chatUser = userService.getById(chatUserId);
            if (chatUser != null) {
                // 设置在线状态
                chatUser.setOnline(onlineUserManager.getState(chatUserId) != null);
                userList.add(chatUser);
            }
        }

        return userList;
    }
    /**
     * 修改：获取所有有聊天记录的用户，不再限制为关注的人
     */
    private List<User> getAllChatUsers(Long userId) {
        // 获取有聊天记录的用户ID列表
        List<Long> chatUserIds = userLinkService.getChatUserIds(userId);
        List<User> userList = new ArrayList<>();

        for (Long chatUserId : chatUserIds) {
            User chatUser = userService.getById(chatUserId);
            if (chatUser != null) {
                userList.add(chatUser);
            }
        }

        return userList;
    }
}