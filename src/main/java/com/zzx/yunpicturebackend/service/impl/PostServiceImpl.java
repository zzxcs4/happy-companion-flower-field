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
import com.zzx.yunpicturebackend.service.*;
import com.zzx.yunpicturebackend.mapper.PostMapper;
import jodd.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zzx.yunpicturebackend.constant.RedisConstants.*;

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
    @Resource
    private UserFieldScoreService userFieldScoreService;
    @Resource
    private UserFieldService userFieldService;
    @Resource
    private PictureService pictureService;
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
        // 发帖 +0.8 分（每天 1 次）
        if (post.getFieldId() != null) {
            String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String dailyCountKey = DAILY_POST_COUNT_KEY + loginUser.getId() + ":" + post.getFieldId() + ":" + today;
            Long count = stringRedisTemplate.opsForValue().increment(dailyCountKey);
            if (count != null && count == 1) {
                stringRedisTemplate.expire(dailyCountKey, 2, TimeUnit.DAYS);
            }
            if (count != null && count <= 1) {
                userFieldScoreService.addScore(loginUser.getId(), post.getFieldId(), 0.8);
            }
            // 更新花田帖子数 +1
           // fieldService.update().setSql("postNum = postNum + 1").eq("id", post.getFieldId()).update();
        }
        //3.查询笔记作者的所有粉丝 select * from tb_follow where follow_user=userId
        List<Follow> follows = followService.query().eq("follow_user_id", loginUser.getId()).list();
        //4.推送笔记 id 给所有粉丝
        for(Follow follow : follows){
            //4.1获取粉丝 id
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
            //3.2 保存用户到 Redis 的 set 集合 zadd key value score
            if(isSuccess){
                stringRedisTemplate.opsForZSet().add(key,loginUserId.toString(),System.currentTimeMillis());
                // 点赞 +0.1 分（每天 2 次）
                Post likedPost = getById(id);
                if (likedPost != null && likedPost.getFieldId() != null) {
                    String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
                    String dailyCountKey = DAILY_LIKE_COUNT_KEY + loginUserId + ":" + likedPost.getFieldId() + ":" + today;
                    Long count = stringRedisTemplate.opsForValue().increment(dailyCountKey);
                    if (count != null && count == 1) {
                        stringRedisTemplate.expire(dailyCountKey, 2, TimeUnit.DAYS);
                    }
                    if (count != null && count <= 2) {
                        userFieldScoreService.addScore(loginUserId, likedPost.getFieldId(), 0.1);
                    }
                }
            }
        }else{
            //4.如果已点赞，取消点赞
            boolean isSuccess = update().setSql("liked=liked-1").eq("id",id).update();
            if(isSuccess){
                stringRedisTemplate.opsForZSet().remove(key,loginUserId.toString());
                // 取消点赞 -0.1 分（仅回滚）
                Post likedPost = getById(id);
                if (likedPost != null && likedPost.getFieldId() != null) {
                    userFieldScoreService.addScore(loginUserId, likedPost.getFieldId(), -0.1);
                }
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
                .reverseRangeByScoreWithScores(key, 0, max, offset, 10);
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
        // 校验帖子是否存在
        Post post = getPostById(id);
        long postId = post.getId();
        // 校验你是不是帖子的创建人
        if (!Objects.equals(post.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
            
        // 删除帖子关联的图片资源
        String images = post.getImages();
        if (StrUtil.isNotBlank(images)) {
            // 分割图片 URL（多张图片以逗号分隔）
            String[] imageUrlArray = images.split(",");
            for (String imageUrl : imageUrlArray) {
                if (StrUtil.isBlank(imageUrl)) {
                    continue;
                }
                try {
                    // 根据 URL 查询图片记录
                    QueryWrapper<File> fileQueryWrapper = new QueryWrapper<>();
                    fileQueryWrapper.eq("url", imageUrl.trim());
                    File file = pictureService.getOne(fileQueryWrapper);
                        
                    if (file != null) {
                        // 删除图片（会自动清理空间资源和 COS 文件）
                        pictureService.deletePicture(file.getId(), loginUser);
                        // 异步清理文件
                        pictureService.clearPictureFile(file);
                    }
                } catch (Exception e) {
                    // 记录错误但不影响帖子删除
                    e.printStackTrace();
                }
            }
        }
            
        // 删除帖子
        boolean result = this.removeById(postId);
            
        // 删除帖子 -0.8 分（仅回滚）
        if (result && post.getFieldId() != null) {
            userFieldScoreService.addScore(loginUser.getId(), post.getFieldId(), -0.8);
        }
            
        return result;
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




