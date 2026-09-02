package com.zzx.yunpicturebackend.controller;



import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.common.ResultUtils;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.service.IFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Autowired
    private IFollowService followService;
    @PutMapping("/{id}/{isFollow}")
    public BaseResponse<Boolean> follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow, HttpServletRequest request){
        return followService.follow(followUserId,isFollow,request);
    }
    @GetMapping("/or/not/{id}")
    public BaseResponse<Boolean> follow(@PathVariable("id") Long followUserId,HttpServletRequest request){
        return followService.isFollow(followUserId,request);
    }
    @GetMapping("/follows")
    public BaseResponse<List<User>> follows(@RequestParam("id") Long userId ,HttpServletRequest request){
        return ResultUtils.success(followService.follows(userId));
    }
    @GetMapping("/fans")
    public BaseResponse<List<User>> fans(@RequestParam(value = "id")Long followUserId,  HttpServletRequest request){
        return ResultUtils.success(followService.fans(followUserId));
    }

    @GetMapping("/common/{id}")
    public BaseResponse<List<User>> followCommons(@PathVariable("id") Long id,HttpServletRequest request){
        return followService.followCommons(id,request);
    }
}
