package com.zzx.flowerfieldfrienddisk.manager.websocket.model;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class Message {
    private String Message;
    private Long UserId;
    private boolean Sender;
    private Timestamp createTime; // 新增：消息创建时间
}
