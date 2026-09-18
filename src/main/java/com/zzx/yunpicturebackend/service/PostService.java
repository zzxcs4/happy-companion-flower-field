package com.zzx.yunpicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.model.dto.picture.PictureUploadRequest;
import com.zzx.yunpicturebackend.model.dto.post.PostQueryRequest;
import com.zzx.yunpicturebackend.model.dto.post.PostVideoUploadRequest;
import com.zzx.yunpicturebackend.model.dto.user.UserUpdateRequest;
import com.zzx.yunpicturebackend.model.entity.Post;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.vo.PictureVO;
import com.zzx.yunpicturebackend.model.vo.PostUploadVO;
import com.zzx.yunpicturebackend.model.vo.ScrollVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author zzx
* @description 针对表【post】的数据库操作Service
* @createDate 2025-10-19 20:25:48
*/
public interface PostService extends IService<Post> {

    BaseResponse<Long> savePost(Post post, HttpServletRequest request);

    BaseResponse<Boolean> likePost(Long id, HttpServletRequest request);

    BaseResponse<List<UserUpdateRequest>> queryPostLikes(Long id);

    Post queryPostById(Long id, HttpServletRequest request);

    BaseResponse<ScrollVO> queryPostOfFollow(Long max, Integer offset, HttpServletRequest request);

    BaseResponse<ScrollVO> queryPostOfFollowById(Long max, Integer offset, Long id, HttpServletRequest request);

    boolean updatePost(Post post, User loginUser);

    /**
     * 发布帖子（补全标题/内容）：仅帖子创建者可调用，用于上传资源建帖后补充文字
     */
    boolean publishPost(Post post, User loginUser);

    boolean deletePost(long id, User loginUser);

    /**
     * 帖子统一分页查询
     * 支持按 id / spaceId / userId / searchText / onlyMe / onlyFollowed 筛选与排序分页
     *
     * @param postQueryRequest 分页查询请求
     * @param request          请求
     * @return 分页结果（记录已补充用户信息、点赞状态、所属花田、视频签名 URL）
     */
    Page<Post> queryPostByPage(PostQueryRequest postQueryRequest, HttpServletRequest request);

    PostUploadVO uploadPostPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 通过 URL 上传帖子图片（上传即建帖）
     */
    PostUploadVO uploadPostPictureByUrl(PictureUploadRequest pictureUploadRequest, User loginUser);

    PostUploadVO uploadPostVideo(MultipartFile multipartFile, PostVideoUploadRequest postVideoUploadRequest, User loginUser);
}
