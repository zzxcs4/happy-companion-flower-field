package com.zzx.flowerfieldfrienddisk.manager.websocket.model;

import lombok.Data;

@Data
public class PrivateMessageRequestMessage {
    private String from; //发送用户账户
    private String to; //接收用户账户
    private String message;
    private String Content;
    private String TargetUserId;
}
