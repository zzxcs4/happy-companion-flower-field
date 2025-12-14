package com.zzx.yunpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.common.ResultUtils;
import com.zzx.yunpicturebackend.constant.SystemConstants;
import com.zzx.yunpicturebackend.exception.BusinessException;
import com.zzx.yunpicturebackend.exception.ErrorCode;
import com.zzx.yunpicturebackend.model.dto.user.UserUpdateRequest;
import com.zzx.yunpicturebackend.model.entity.*;
import com.zzx.yunpicturebackend.model.enums.FieldStatusEnum;
import com.zzx.yunpicturebackend.model.vo.ScrollVO;
import com.zzx.yunpicturebackend.service.FieldService;
import com.zzx.yunpicturebackend.service.IFollowService;
import com.zzx.yunpicturebackend.service.PostService;
import com.zzx.yunpicturebackend.mapper.PostMapper;
import com.zzx.yunpicturebackend.service.UserService;
import jodd.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.zzx.yunpicturebackend.constant.RedisConstants.BLOG_LIKED_KEY;
import static com.zzx.yunpicturebackend.constant.RedisConstants.FEED_KEY;

/**
* @author zzx
* @description 针对表【post】的数据库操作Service实现
* @createDate 2025-10-19 20:25:48
*/
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post>
    implements PostService{
    @Resource
    private UserService userService;
    @Resource
    @Lazy
    private FieldService fieldService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IFollowService followService;
    @Override
    public BaseResponse<Long> savePost(Post post, HttpServletRequest request) {
        //1.获取登录用户
        User loginUser = userService.getLoginUser(request);
        post.setUserId(loginUser.getId());
        //2.保存探店笔记
        boolean isSuccess = save(post);
        if(!isSuccess){
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        //3.查询笔记作者的所有粉丝 select * from tb_follow where follow_user=userId
        List<Follow> follows = followService.query().eq("follow_user_id", loginUser.getId()).list();
        //4.推送笔记id给所有粉丝
        for(Follow follow : follows){
            //4.1获取粉丝id
            Long userId = follow.getUserId();
            //4.2推送
            String key=FEED_KEY+userId;
            stringRedisTemplate.opsForZSet().add(key,post.getId().toString(),System.currentTimeMillis());
        }
        return ResultUtils.success(post.getId());
    }

    @Override
    public BaseResponse<Boolean> likePost(Long id,HttpServletRequest request) {
        //1.判断当前登录用户是否点赞
        Long loginUserId = userService.getLoginUser(request).getId();
        //2.判断当前登录用户是否已经点赞
        String key=BLOG_LIKED_KEY+id;
        Double score = stringRedisTemplate.opsForZSet().score(key, loginUserId.toString());
        if(score==null) {
            //3.如果未点赞，可以点赞
            boolean isSuccess = update().setSql("liked=liked+1").eq("id", id).update();
            //3.2保存用户到Redis的set集合 zadd key value score
            if(isSuccess){
                stringRedisTemplate.opsForZSet().add(key,loginUserId.toString(),System.currentTimeMillis());
            }
        }else{
            //4.如果已点赞，取消点赞
            boolean isSuccess = update().setSql("liked=liked-1").eq("id",id).update();
            if(isSuccess){
                stringRedisTemplate.opsForZSet().remove(key,loginUserId.toString());
            }
        }

        return ResultUtils.success(true);
    }

    @Override
    public BaseResponse<List<Post>> queryHotPost(Integer current, HttpServletRequest request) {
        Page<Post> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Post> records = page.getRecords();
        // 查询用户
        records.forEach(post -> {
            this.queryPostUser(post);
            this.isPostLiked(post,request);
        });
        return ResultUtils.success(records);
    }
    @Override
    public BaseResponse<Post> queryPostById(Long id,HttpServletRequest request) {
        //1.查询blog
        Post post = getById(id);
        if(post==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"笔记不存在");
        }

        //2.查询blog有关的用户
        queryPostUser(post);
        isPostLiked(post,request);
        queryPostField(post);
        return ResultUtils.success(post);
    }

    @Override
    public BaseResponse<List<UserUpdateRequest>> queryPostLikes(Long id) {
        String key = BLOG_LIKED_KEY+id;
        //1.查询top5点赞用户 zrange key 0 4
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        //2.解析用户id
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids);
        //3.根据用户id查询用户

        List<UserUpdateRequest> userDTOS = userService.query().in("id",ids).last("ORDER BY FIELD(id,"+idStr+")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserUpdateRequest.class))
                .collect(Collectors.toList());
        return ResultUtils.success(userDTOS);
    }

    @Override
    public BaseResponse<ScrollVO> queryPostOfFollow(Long max, Integer offset,HttpServletRequest request) {
        //1.获取当前用户
        Long userId = userService.getLoginUser(request).getId();
        //2.查询收件箱
        String key=FEED_KEY+userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 3);
        //3.非空判断
        if(typedTuples==null||typedTuples.isEmpty()){
            return ResultUtils.success(null);
        }
        //4.解析数据：blogId、minTime(时间戳)、offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime=0;
        int os=1;
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples){
            //4.1获取id
            ids.add(Long.valueOf(tuple.getValue()));
            //4.2获取分数（时间戳）
            long time = tuple.getScore().longValue();
            if(time==minTime){
                os++;
            }else {
                minTime=time;
                os=1;
            }

        }
        //5.根据id查询blog
        String idStr = StrUtil.join(",", ids);
        List<Post> posts = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for(Post post:posts){
            queryPostUser(post);
            isPostLiked(post,request);
        }
        //6.封装并返回
        ScrollVO r=new ScrollVO();
        r.setList(posts);
        r.setOffset(os);
        r.setMinTime(minTime);
        return ResultUtils.success(r);
    }

    @Override
    public BaseResponse<List<Post>> queryFieldPost(Long fieldId, HttpServletRequest request) {
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(fieldId),"fieldId", fieldId);
        List<Post> postList = this.list(queryWrapper);
        // 查询用户
        postList.forEach(post -> {
            this.queryPostUser(post);
            this.isPostLiked(post,request);
        });
        return  ResultUtils.success(postList);
    }

    @Override
    public boolean updatePost(Post post, User loginUser) {
        if (post == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = post.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Post oldPost = this.getById(id);
        if (oldPost == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        }
        // 只有管理员或者帖子的创建者可以修改
        if (!Objects.equals(oldPost.getUserId(), loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        Post updatePost = new Post();
        BeanUtils.copyProperties(post, updatePost);
        return this.updateById(updatePost);
    }

    @Override
    public boolean deletePost(long id, User loginUser) {
        // 校验花田是否存在
        Post post = getPostById(id);
        long postId = post.getId();
        // 校验你是不是帖子的创建人,后面这句话为什么不行Field.getUserId() != loginUser.getId()
        if (!Objects.equals(post.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 移除所有加入队伍的关联信息
//        QueryWrapper<UserField> userFieldQueryWrapper = new QueryWrapper<>();
//        userFieldQueryWrapper.eq("fieldId", postId);
//        boolean result = userFieldService.remove(userFieldQueryWrapper);
//        if (!result) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除花田关联信息失败");
//        }
        // 删除队伍
        return this.removeById(postId);
    }

    @Override
    public BaseResponse<List<Post>> queryPost(Integer current, String searchText, Long fieldId,HttpServletRequest request) {
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(fieldId),"fieldId", fieldId);
        queryWrapper.like(StringUtil.isNotBlank(searchText),"title", searchText).or();
        queryWrapper.like(StringUtil.isNotBlank(searchText),"content", searchText);

        // 根据查询
        Page<Post> page = this.page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE),queryWrapper);
        // 获取当前页数据
        List<Post> records = page.getRecords();
        // 查询用户
        records.forEach(post -> {
            this.queryPostUser(post);
            this.isPostLiked(post,request);
        });
        return ResultUtils.success(records);
    }

    @Override
    public BaseResponse<List<Post>> queryMyPost(@RequestParam(value = "current", defaultValue = "1") Integer current, HttpServletRequest request) {
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 根据用户查询
        Page<Post> page = query()
                .eq("userId", loginUser.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Post> records = page.getRecords();
        // 查询用户
        records.forEach(post -> {
            this.queryPostUser(post);
            this.isPostLiked(post,request);
        });
        return ResultUtils.success(records);
    }

    @Override
    public BaseResponse<List<Post>> queryPostByUserId(Integer current, Long id) {
        // 根据用户查询
        Page<Post> page = query()
                .eq("userId", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Post> records = page.getRecords();
        // 查询用户
        records.forEach(post -> {
            this.queryPostUser(post);
            this.isPostLiked(post,id);
        });
        return ResultUtils.success(records);
    }

    @Override
    public BaseResponse<ScrollVO> queryPostOfFollowById(Long max, Integer offset, Long id, HttpServletRequest request) {
        //1.获取当前用户
        Long userId = userService.getLoginUser(request).getId();
        //2.查询收件箱
        String key=FEED_KEY+userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 3);
        //3.非空判断
        if(typedTuples==null||typedTuples.isEmpty()){
            return ResultUtils.success(null);
        }
        //4.解析数据：blogId、minTime(时间戳)、offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime=0;
        int os=1;
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples){
            //4.1获取id
            ids.add(Long.valueOf(tuple.getValue()));
            //4.2获取分数（时间戳）
            long time = tuple.getScore().longValue();
            if(time==minTime){
                os++;
            }else {
                minTime=time;
                os=1;
            }

        }
        //5.根据id查询blog
        String idStr = StrUtil.join(",", ids);

        List<Post> posts = query().in("id", ids).eq("userId",id).last("ORDER BY FIELD(id," + idStr + ")").list();
        for(Post post:posts){
            queryPostUser(post);
            isPostLiked(post,request);
        }
        //6.封装并返回
        ScrollVO r=new ScrollVO();
        r.setList(posts);
        r.setOffset(os);
        r.setMinTime(minTime);
        return ResultUtils.success(r);
    }

    private Post getPostById(Long postId) {
        if (postId == null || postId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Post post = this.getById(postId);
        if (post == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"队伍不存在");
        }
        return post;
    }

    private void queryPostUser(Post post){
        Long userId = post.getUserId();
        User user = userService.getById(userId);
        post.setName(user.getUserName());
        post.setIcon(user.getUserAvatar());
    }
    private void queryPostField(Post post){
        Long fieldId = post.getFieldId();
        post.setField(fieldService.getById(fieldId).getName());
    }
    private  void isPostLiked(Post post,HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        if(loginUser==null){
            return;
        }
        Long userId = loginUser.getId();
        String key=BLOG_LIKED_KEY+post.getId();
        Double score=stringRedisTemplate.opsForZSet().score(key,userId.toString());
        post.setIsLike(score!=null);
    }
    private  void isPostLiked(Post post,Long userId){
        if(userId==null){
            return;
        }
        String key=BLOG_LIKED_KEY+post.getId();
        Double score=stringRedisTemplate.opsForZSet().score(key,userId.toString());
        post.setIsLike(score!=null);
    }
}




