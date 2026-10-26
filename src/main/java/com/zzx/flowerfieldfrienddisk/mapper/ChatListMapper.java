package com.zzx.flowerfieldfrienddisk.mapper;

import com.zzx.flowerfieldfrienddisk.manager.websocket.model.ChatList;
import com.zzx.flowerfieldfrienddisk.model.vo.UnreadChatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface ChatListMapper {
    List<ChatList> selectChat(@Param("linkId")int linkId);

    void insertChat(@Param("linkId")Integer linkId, @Param("fromId")Long fromId,
                    @Param("content")String content, @Param("timestamp")Timestamp timestamp,
                    @Param("toId")Long toId);

    /**
     * 查询某接收者的未读会话（按发送者分组统计未读数）
     */
    List<UnreadChatVO> selectUnreadConversations(@Param("toId") Long toId);

    /**
     * 将某发送者发给某接收者的消息全部标记已读
     */
    void markRead(@Param("fromId") Long fromId, @Param("toId") Long toId);
}
