package com.zzx.yunpicturebackend.service;

import com.zzx.yunpicturebackend.manager.websocket.model.ChatList;
import com.zzx.yunpicturebackend.mapper.ChatListMapper;
import com.zzx.yunpicturebackend.model.vo.UnreadChatVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class ChatListService {
    @Autowired
    private ChatListMapper chatListMapper;

    public List<ChatList> selectChat(int linkId){
        return chatListMapper.selectChat(linkId);
    }

    public void insertChat(Integer linkId, Long fromId, String content, Timestamp timestamp, Long toId) {
        chatListMapper.insertChat(linkId, fromId, content, timestamp, toId);
    }

    /**
     * 查询某接收者的未读会话（供轮询红点使用）
     */
    public List<UnreadChatVO> selectUnreadConversations(Long toId) {
        return chatListMapper.selectUnreadConversations(toId);
    }

    /**
     * 将某会话的消息标记已读（点击聊天列表项时调用）
     */
    public boolean markRead(Long fromId, Long toId) {
        chatListMapper.markRead(fromId, toId);
        return true;
    }
}
