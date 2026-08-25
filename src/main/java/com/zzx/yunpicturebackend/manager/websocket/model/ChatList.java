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
    /** 接收者 id */
    private Long toId;
    /** 接收者是否已读：0-未读 1-已读 */
    private Integer isRead;
}
