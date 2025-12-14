package com.zzx.yunpicturebackend.controller;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.common.DeleteRequest;
import com.zzx.yunpicturebackend.common.ResultUtils;
import com.zzx.yunpicturebackend.constant.SystemConstants;
import com.zzx.yunpicturebackend.exception.BusinessException;
import com.zzx.yunpicturebackend.exception.ErrorCode;
import com.zzx.yunpicturebackend.model.dto.picture.PictureQueryRequest;
import com.zzx.yunpicturebackend.model.dto.user.UserUpdateRequest;
import com.zzx.yunpicturebackend.model.entity.Field;
import com.zzx.yunpicturebackend.model.entity.Post;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.enums.PictureReviewStatusEnum;
import com.zzx.yunpicturebackend.model.vo.PictureVO;
import com.zzx.yunpicturebackend.model.vo.ScrollVO;
import com.zzx.yunpicturebackend.service.FieldService;
import com.zzx.yunpicturebackend.service.PostService;
import com.zzx.yunpicturebackend.service.UserService;
import jodd.util.StringUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/post")
public class PostController {

    @Resource
    private PostService postService;
    @Resource
    private UserService userService;
    @Resource
    private FieldService fieldService;


    @PostMapping
    public BaseResponse<Long> savePost(@RequestBody Post post , HttpServletRequest request ) {
        return postService.savePost(post,request);
    }
    /**
     * 删除头条业务接口 实现
     * @param deleteRequest
     * @throws IOException
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePost(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        if (deleteRequest == null || deleteRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        boolean result = postService.deletePost(id, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return ResultUtils.success(true);

    }


    /**
     * 更新头条的业务接口实现
     * @param post
     * @throws IOException
     */

    @PostMapping("/update")
    public BaseResponse<Boolean> updatePost(@RequestBody Post post, HttpServletRequest request){
        if (post == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = postService.updatePost(post,loginUser);
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新失败");
        }
        return ResultUtils.success(true);
    }


    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<List<Post>> queryFieldPost(@RequestParam("id") Long fieldId,
                                                             HttpServletRequest request) {
        return postService.queryFieldPost(fieldId,request);

    }
    @PutMapping("/like/{id}")
    public BaseResponse<Boolean> likePost(@PathVariable("id") Long id, HttpServletRequest request) {
        return postService.likePost(id,request);
    }
    @GetMapping("/query")
    public BaseResponse<List<Post>> queryPost(@RequestParam(value = "current", defaultValue = "1") Integer current,String searchText,@RequestParam("id") Long fieldId,HttpServletRequest request) {

        return postService.queryPost(current,searchText,fieldId,request);
    }
    @GetMapping("/of/me")
    public BaseResponse<List<Post>> queryMyPost(@RequestParam(value = "current", defaultValue = "1") Integer current,HttpServletRequest request) {


        return postService.queryMyPost(current,request);
    }

    @GetMapping("/hot")
    public BaseResponse<List<Post>> queryHotPost(@RequestParam(value = "current", defaultValue = "1") Integer current,HttpServletRequest request) {
        // 根据用户查询
        return postService.queryHotPost(current,request);
    }
    @GetMapping("/{id}")
    public BaseResponse<Post> queryPostById(@PathVariable("id") Long id,HttpServletRequest request){
        return postService.queryPostById(id,request);
    }
    @GetMapping("/likes/{id}")
    public BaseResponse<List<UserUpdateRequest>> queryPostLikes(@PathVariable("id") Long id){
        return postService.queryPostLikes(id);
    }
    // PostController
    @GetMapping("/of/user")
    public BaseResponse<List<Post> > queryPostByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id) {
        return postService.queryPostByUserId(current,id);
    }
    @GetMapping("/of/follow")
    public BaseResponse<ScrollVO> queryPostOfFollow(@RequestParam(value = "lastId") Long max,
                                                    @RequestParam(value = "offser",defaultValue ="0") Integer offset, HttpServletRequest request){
        return postService.queryPostOfFollow(max,offset,request);
    }
    @GetMapping("/of/user/follow")
    public BaseResponse<ScrollVO> queryPostOfFollow(@RequestParam(value = "lastId") Long max,
                                                    @RequestParam(value = "offser",defaultValue ="0") Integer offset, @RequestParam("id") Long id,HttpServletRequest request){
        return postService.queryPostOfFollowById(max,offset, id,request);
    }
}