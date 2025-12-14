package com.zzx.yunpicturebackend.manager.websocket.model;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class ChatList {
    private int listId;
    private int linkId;
    private Long userId;
    private String content;
    private Timestamp createTime;
}
