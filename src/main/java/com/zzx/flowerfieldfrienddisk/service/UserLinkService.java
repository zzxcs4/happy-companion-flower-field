package com.zzx.flowerfieldfrienddisk.service;

import com.zzx.flowerfieldfrienddisk.mapper.UserLinkMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserLinkService {
    @Autowired
    private UserLinkMapper userLinkMapper;

    public Integer selectLinkId(Long fromId,Long toId){
        return userLinkMapper.selectLinkId(fromId,toId);
    }

    public void insertLink(Long min, Long max) {
        userLinkMapper.insertLink(min,max);
    }
    public List<Long> getChatUserIds(Long userId){
       return userLinkMapper.selectUserIds(userId);
    }
}

