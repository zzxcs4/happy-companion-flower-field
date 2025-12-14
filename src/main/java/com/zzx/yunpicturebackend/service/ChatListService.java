package com.zzx.yunpicturebackend.service;

import com.zzx.yunpicturebackend.manager.websocket.model.ChatList;
import com.zzx.yunpicturebackend.mapper.ChatListMapper;
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

    public void insertChat(Integer linkId, Long fromId, String content, Timestamp timestamp) {
        chatListMapper.insertChat(linkId,fromId,content,timestamp);
    }
}
