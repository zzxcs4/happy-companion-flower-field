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

    BaseResponse<List<User>> followCommons(Long id,HttpServletRequest request);

    List<User> follows(Long userId);

    List<User> fans(Long followUserId);
}