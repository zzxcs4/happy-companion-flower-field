package com.zzx.yunpicturebackend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserLinkMapper {
    Integer selectLinkId(@Param("fromId")Long fromId,  @Param("toId")Long toId);

    void insertLink(@Param("min")Long min, @Param("max")Long max);

    List<Long> selectUserIds(Long userId);
}
