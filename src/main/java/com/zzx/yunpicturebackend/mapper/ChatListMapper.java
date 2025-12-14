package com.zzx.yunpicturebackend.mapper;

import com.zzx.yunpicturebackend.manager.websocket.model.ChatList;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface ChatListMapper {
    List<ChatList> selectChat(@Param("linkId")int linkId);

    void insertChat(@Param("linkId")Integer linkId, @Param("fromId")Long fromId,
                    @Param("content")String content, @Param("timestamp")Timestamp timestamp);
}
