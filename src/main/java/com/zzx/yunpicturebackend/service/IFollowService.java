package com.zzx.yunpicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.model.entity.Follow;
import com.zzx.yunpicturebackend.model.entity.User;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface IFollowService extends IService<Follow> {

    BaseResponse<Boolean> follow(Long followUserId, Boolean isFollow, HttpServletRequest request);

    BaseResponse<Boolean> isFollow(Long followUserId,HttpServletRequest request);

    // 共同关注：Redis 缓存与实现已注释（暂未启用），接口声明一并注释，避免实现类未实现抽象方法
    // BaseResponse<List<User>> followCommons(Long id,HttpServletRequest request);

    List<User> follows(Long userId);

    List<User> fans(Long followUserId);
}