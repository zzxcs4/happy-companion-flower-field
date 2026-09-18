package com.zzx.yunpicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.common.DeleteRequest;
import com.zzx.yunpicturebackend.common.ResultUtils;
import com.zzx.yunpicturebackend.exception.BusinessException;
import com.zzx.yunpicturebackend.exception.ErrorCode;
import com.zzx.yunpicturebackend.exception.ThrowUtils;
import com.zzx.yunpicturebackend.manager.auth.SpaceUserAuthManager;
import com.zzx.yunpicturebackend.manager.auth.SpaceUserPermissionConstant;
import com.zzx.yunpicturebackend.manager.auth.StpKit;
import com.zzx.yunpicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.zzx.yunpicturebackend.model.dto.picture.PictureUploadRequest;
import com.zzx.yunpicturebackend.model.dto.post.PostQueryRequest;
import com.zzx.yunpicturebackend.model.dto.post.PostVideoUploadRequest;
import com.zzx.yunpicturebackend.model.dto.user.UserUpdateRequest;
import com.zzx.yunpicturebackend.model.entity.Post;
import com.zzx.yunpicturebackend.model.entity.Space;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.vo.PictureVO;
import com.zzx.yunpicturebackend.model.vo.PostUploadVO;
import com.zzx.yunpicturebackend.model.vo.ScrollVO;
import com.zzx.yunpicturebackend.service.PictureService;
import com.zzx.yunpicturebackend.service.PostService;
import com.zzx.yunpicturebackend.service.SpaceService;
import com.zzx.yunpicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

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
    private SpaceService spaceService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    @Resource
    private PictureService pictureService;

    /**
     *
     * 上传帖子接口
     * @param post
     * @param request
     * @return
     */
    @PostMapping
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.POST_UPLOAD)
    public BaseResponse<Long> savePost(@RequestBody Post post , HttpServletRequest request ) {
        return postService.savePost(post,request);
    }
    /**
     * 上传帖子图片（可重新上传）
     */
    @PostMapping("/uploadPostImg")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.POST_UPLOAD)
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PostUploadVO> uploadPostPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);

        return ResultUtils.success(postService.uploadPostPicture(multipartFile, pictureUploadRequest, loginUser));
    }
    /**
     * 通过 URL 上传帖子图片（上传即建帖）
     */
    @PostMapping("/uploadPostImg/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.POST_UPLOAD)
    public BaseResponse<PostUploadVO> uploadPostPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);

        return ResultUtils.success(postService.uploadPostPictureByUrl(pictureUploadRequest, loginUser));
    }
    /**
     * 上传帖子视频（可重新上传）
     */
    @PostMapping("/uploadPostVideo")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.POST_UPLOAD)
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PostUploadVO> uploadPostVideo(
            @RequestPart("file") MultipartFile multipartFile,
            PostVideoUploadRequest postVideoUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);

        return ResultUtils.success(postService.uploadPostVideo(multipartFile, postVideoUploadRequest, loginUser));
    }
    /**
     * 删除头条业务接口 实现
     * @param id
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.POST_DELETE)
    public BaseResponse<Boolean> deletePost(long id, HttpServletRequest request){
        if (id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //long id = deleteRequest.getId();
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
     */

    @PostMapping("/update")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.POST_EDIT)
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
     * 发布帖子（补全标题/内容）：上传资源建帖后，由创建者补充文字
     */
    @PostMapping("/publish")
    public BaseResponse<Boolean> publishPost(@RequestBody Post post, HttpServletRequest request){
        if (post == null || post.getId() == null || post.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = postService.publishPost(post, loginUser);
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"发布失败");
        }
        return ResultUtils.success(true);
    }


    /**
     * 帖子统一分页查询入口
     * 通过 PostQueryRequest 组合条件，覆盖原多个查询接口：
     * - 花田动态：spaceId
     * - 花田/全局搜索：spaceId + searchText
     * - 用户帖子：userId
     * - 我的帖子：onlyMe
     * - 热门帖子：sortField = liked
     * - 关注动态流：onlyFollowed（保留原接口 /of/follow、/of/user/follow 供前端关注页使用）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<Post>> listPostByPage(@RequestBody PostQueryRequest postQueryRequest,
                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(postQueryRequest == null, ErrorCode.PARAMS_ERROR);
        boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.POST_VIEW);
        ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        Page<Post> postPage = postService.queryPostByPage(postQueryRequest, request);
        return ResultUtils.success(postPage);
    }

    @PutMapping("/like/{id}")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.POST_VIEW)
    public BaseResponse<Boolean> likePost(@PathVariable("id") Long id, HttpServletRequest request) {
        return postService.likePost(id,request);
    }
//    @GetMapping("/likes/{id}")
//    public BaseResponse<List<UserUpdateRequest>> queryPostLikes(@PathVariable("id") Long id){
//        return postService.queryPostLikes(id);
//    }
    @GetMapping("/{id}")
    public BaseResponse<Post> queryPostById(long id,HttpServletRequest request){

        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Post post = postService.queryPostById(id,request);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间的帖子，需要校验权限
        Space space = null;
        Long spaceId = post.getSpaceId();
        if (spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.POST_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
            // 获取权限列表
            User loginUser = userService.getLoginUser(request);
        // 如果是帖子的创建者，则拥有所有权限(除上传)
        List<String> permissionList = new ArrayList<>();
        if (post.getUserId().equals(loginUser.getId())) {

            permissionList.add("post:view");
            //permissionList.add("post:upload");
            permissionList.add("post:edit");
            permissionList.add("post:delete");
            //permissionList.addAll(spaceUserAuthManager.getPermissionsByRole());
        }else{
            permissionList = spaceUserAuthManager.getPermissionList(null,space, loginUser);
        }
            //List<String> permissionList = spaceUserAuthManager.getPermissionList(null,space, loginUser);
            post.setPermissionsList(permissionList);
            // 获取封装类
            return ResultUtils.success(post);
    }
    // 关注动态流（Redis zset 滚动分页）：全部关注
    @GetMapping("/of/follow")
    public BaseResponse<ScrollVO> queryPostOfFollow(@RequestParam(value = "lastId") Long max,
                                                    @RequestParam(value = "offser",defaultValue ="0") Integer offset, HttpServletRequest request){
        return postService.queryPostOfFollow(max,offset,request);
    }
    // 关注动态流（Redis zset 滚动分页）：按关注用户筛选
    @GetMapping("/of/user/follow")
    public BaseResponse<ScrollVO> queryPostOfFollow(@RequestParam(value = "lastId") Long max,
                                                    @RequestParam(value = "offser",defaultValue ="0") Integer offset, @RequestParam("id") Long id,HttpServletRequest request){
        return postService.queryPostOfFollowById(max,offset, id,request);
    }
}
