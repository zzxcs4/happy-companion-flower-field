package com.zzx.yunpicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.model.dto.user.UserUpdateRequest;
import com.zzx.yunpicturebackend.model.entity.Post;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.vo.ScrollVO;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author zzx
* @description 针对表【post】的数据库操作Service
* @createDate 2025-10-19 20:25:48
*/
public interface PostService extends IService<Post> {

    BaseResponse<Long> savePost(Post post, HttpServletRequest request);

    BaseResponse<Boolean> likePost(Long id,HttpServletRequest request);

    BaseResponse<List<Post>> queryHotPost(Integer current, HttpServletRequest request);

    BaseResponse<Post> queryPostById(Long id, HttpServletRequest request);

    BaseResponse<List<UserUpdateRequest>> queryPostLikes(Long id);

    BaseResponse<ScrollVO> queryPostOfFollow(Long max, Integer offset, HttpServletRequest request);

    BaseResponse<List<Post>> queryFieldPost(Long fieldId, HttpServletRequest request);



    boolean updatePost(Post post, User loginUser);

    boolean deletePost(long id, User loginUser);

    BaseResponse<List<Post>> queryPost(Integer current, String searchText, Long fieldId,HttpServletRequest request);

    BaseResponse<List<Post>> queryMyPost(@RequestParam(value = "current", defaultValue = "1") Integer current, HttpServletRequest request);

    BaseResponse<List<Post>> queryPostByUserId(Integer current, Long id);

    BaseResponse<ScrollVO> queryPostOfFollowById(Long max, Integer offset, Long id, HttpServletRequest request);
}
