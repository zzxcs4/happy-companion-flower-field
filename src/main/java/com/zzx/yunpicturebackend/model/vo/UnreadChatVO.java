package com.zzx.yunpicturebackend.model.vo;

import lombok.Data;

/**
 * 未读私信会话视图（按发送者聚合）
 */
@Data
public class UnreadChatVO {
    /**
     * 发送者用户 id
     */
    private Long userId;
    /**
     * 未读消息条数
     */
    private Integer unreadCount;
}
