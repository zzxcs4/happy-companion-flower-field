package com.zzx.flowerfieldfrienddisk.manager.websocket.model;

import com.zzx.flowerfieldfrienddisk.model.entity.User;
import lombok.Data;

import java.util.List;
@Data
public class PrivateMessageResponseMessage {
        private Integer status;
        private String Message;
        private List<Message>  messages; // 以前的聊天记录, 每一条包含, 用户Id, 内容, 是否是发送者.
        private String Fromusername;
        private List<User> Users;
        private String Tousername;
        private User userInfo; // 新增：用于返回目标用户信息
        private User currentUser;
        private String fromUserId; // 新增：用于新消息推送时标识发送方
        private Boolean Online;
}
