package com.zzx.flowerfieldfrienddisk.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.flowerfieldfrienddisk.common.BaseResponse;
import com.zzx.flowerfieldfrienddisk.common.ResultUtils;
import com.zzx.flowerfieldfrienddisk.exception.BusinessException;
import com.zzx.flowerfieldfrienddisk.exception.ErrorCode;
import com.zzx.flowerfieldfrienddisk.exception.ThrowUtils;
import com.zzx.flowerfieldfrienddisk.manager.CosManager;
import com.zzx.flowerfieldfrienddisk.manager.upload.FilePictureUpload;
import com.zzx.flowerfieldfrienddisk.manager.upload.PictureUploadTemplate;
import com.zzx.flowerfieldfrienddisk.manager.upload.UrlPictureUpload;
import com.zzx.flowerfieldfrienddisk.model.dto.file.UploadPictureResult;
import com.zzx.flowerfieldfrienddisk.model.dto.picture.PictureUploadRequest;
import com.zzx.flowerfieldfrienddisk.model.dto.post.PostQueryRequest;
import com.zzx.flowerfieldfrienddisk.model.dto.post.PostVideoUploadRequest;
import com.zzx.flowerfieldfrienddisk.model.dto.user.UserUpdateRequest;
import com.zzx.flowerfieldfrienddisk.model.entity.*;
import com.zzx.flowerfieldfrienddisk.model.enums.PictureReviewStatusEnum;
import com.zzx.flowerfieldfrienddisk.model.vo.PostUploadVO;
import com.zzx.flowerfieldfrienddisk.model.vo.ScrollVO;
import com.zzx.flowerfieldfrienddisk.model.vo.VideoVO;
import com.zzx.flowerfieldfrienddisk.service.*;
import com.zzx.flowerfieldfrienddisk.mapper.PostMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zzx.flowerfieldfrienddisk.constant.RedisConstants.*;

/**
* @author zzx
* @description 针对表【post】的数据库操作Service实现
* @createDate 2025-10-19 20:25:48
*/
@Slf4j
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post>
    implements PostService{
    @Resource
    private UserService userService;
    @Resource
    @Lazy
    private SpaceService spaceService;
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
    @Resource
    private CosManager cosManager;
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private VideosService videosService;
    /** 私有视频签名 URL 有效期（秒）：12 小时 */
    private static final long SIGN_URL_EXPIRE_SECONDS = 12 * 60 * 60L;
    /** 视频扩展名（与前端 getPostVideo / FileController.VIDEO_FORMATS 保持一致） */
    private static final List<String> VIDEO_FORMATS = Arrays.asList(
            "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm"
    );
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
        if (post.getSpaceId() != null) {
            String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String dailyCountKey = DAILY_POST_COUNT_KEY + loginUser.getId() + ":" + post.getSpaceId() + ":" + today;
            Long count = stringRedisTemplate.opsForValue().increment(dailyCountKey);
            if (count != null && count == 1) {
                stringRedisTemplate.expire(dailyCountKey, 2, TimeUnit.DAYS);
            }
            if (count != null && count <= 1) {
                userFieldScoreService.addScore(loginUser.getId(), post.getSpaceId(), 0.8);
            }
            // 更新花田帖子数 +1
           // fieldService.update().setSql("postNum = postNum + 1").eq("id", post.getSpaceId()).update();
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
            // 推送时续期：feed 集合 30 天无活动自动过期（配合惰性删除兜底清理幽灵数据）
            stringRedisTemplate.expire(key, FEED_EXPIRE_DAYS, TimeUnit.DAYS);
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
                if (likedPost != null && likedPost.getSpaceId() != null) {
                    String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
                    String dailyCountKey = DAILY_LIKE_COUNT_KEY + loginUserId + ":" + likedPost.getSpaceId() + ":" + today;
                    Long count = stringRedisTemplate.opsForValue().increment(dailyCountKey);
                    if (count != null && count == 1) {
                        stringRedisTemplate.expire(dailyCountKey, 2, TimeUnit.DAYS);
                    }
                    if (count != null && count <= 2) {
                        userFieldScoreService.addScore(loginUserId, likedPost.getSpaceId(), 0.1);
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
                if (likedPost != null && likedPost.getSpaceId() != null) {
                    userFieldScoreService.addScore(loginUserId, likedPost.getSpaceId(), -0.1);
                }
            }
        }
    
        return ResultUtils.success(true);
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
//        if (!Objects.equals(oldPost.getUserId(), loginUser.getId()) && !userService.isAdmin(loginUser)) {
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//        }

        Post updatePost = new Post();
        BeanUtils.copyProperties(post, updatePost);
        return this.updateById(updatePost);
    }

    @Override
    public boolean deletePost(long id, User loginUser) {
        // 校验帖子是否存在
        Post post = getPostById(id);
        long postId = post.getId();
        // 只有管理员或者帖子的创建者可以修改
//        if (!Objects.equals(post.getUserId(), loginUser.getId()) && !userService.isAdmin(loginUser)) {
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//        }
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
                    throw new BusinessException(ErrorCode.OPERATION_ERROR);
                }
            }
        }
        //清除粉丝zSet
//        List<User> fans = followService.fans(post.getUserId());
//        for (User fan : fans){
//            stringRedisTemplate.opsForZSet().remove(FEED_KEY + fan.getId(), post.getId().toString());
//        }
        //clearFansFeedAsync(postId, post.getUserId());

        // 删除帖子
        boolean result = this.removeById(postId);
            
        // 删除帖子 -0.8 分（仅回滚）
        if (result && post.getSpaceId() != null) {
            userFieldScoreService.addScore(loginUser.getId(), post.getSpaceId(), -0.8);
        }
            
        return result;
    }
    @Async("taskExecutor") // 使用异步线程池
    public void clearFansFeedAsync(Long postId, Long userId) {
        int pageSize = 1000;
        int pageNum = 1;

        while (true) {
            // 分页获取粉丝ID（只查ID，减少数据传输）
            List<User> fans = followService.fans(userId);
            if (CollectionUtils.isEmpty(fans)) {
                break;
            }
            // 获取所有粉丝ID列表（只查ID，不查完整User对象）
            List<Long> fanIds = fans.stream().map(User::getId).collect(Collectors.toList());
            // 使用 Pipeline 批量删除
            stringRedisTemplate.executePipelined(
                    (RedisCallback<Object>) connection -> {
                        for (Long fanId : fanIds) {
                            String key = FEED_KEY + fanId;
                            connection.zSetCommands()
                                    .zRem(key.getBytes(), postId.toString().getBytes());
                        }
                        return null;
                    }
            );

            log.info("已清理第 {} 批粉丝（{} 人）的 feed 流", pageNum, fanIds.size());
            pageNum++;
        }

        log.info("帖子 {} 已从所有粉丝的 feed 流中移除", postId);
    }

    @Override
    public Post queryPostById(Long id,HttpServletRequest request) {
        //1.查询blog
        Post post = getById(id);
        if(post==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"笔记不存在");
        }

        //2.查询blog有关的用户
        queryPostUser(post);
        isPostLiked(post,request);
        queryPostField(post);
        return post;
    }

    @Override
    public BaseResponse<ScrollVO> queryPostOfFollow(Long max, Integer offset,HttpServletRequest request) {
        //1.获取当前用户
        Long userId = userService.getLoginUser(request).getId();
        //2.查询收件箱
        String key=FEED_KEY+userId;
        //2.1 读取时续期：feed 集合 30 天无活动自动过期（配合惰性删除兜底清理）
        stringRedisTemplate.expire(key, FEED_EXPIRE_DAYS, TimeUnit.DAYS);
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
        //5.根据id查询blog（in 查询天然过滤已删除的帖子）
        String idStr = StrUtil.join(",", ids);
        List<Post> posts = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        //5.2 惰性清理：把已删除帖子的 id 从 feed 集合中顺手移除，下次读取即干净
        Set<Long> aliveIds = posts.stream().map(Post::getId).collect(Collectors.toSet());
        List<String> ghostIds = ids.stream()
                .filter(feedId -> !aliveIds.contains(feedId))
                .map(String::valueOf)
                .collect(Collectors.toList());
        if (!ghostIds.isEmpty()) {
            stringRedisTemplate.opsForZSet().remove(key, ghostIds.toArray(new String[0]));
            log.info("feed 惰性清理：用户 {} 移除了 {} 条已删除帖子的记录", userId, ghostIds.size());
        }
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
    public BaseResponse<ScrollVO> queryPostOfFollowById(Long max, Integer offset, Long id, HttpServletRequest request) {
        //1.获取当前用户
        Long userId = userService.getLoginUser(request).getId();
        //2.查询收件箱
        String key=FEED_KEY+userId;
        //2.1 读取时续期：feed 集合 30 天无活动自动过期（配合惰性删除兜底清理）
        stringRedisTemplate.expire(key, FEED_EXPIRE_DAYS, TimeUnit.DAYS);
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
        //5.根据id查询blog（只看某位作者的帖子）
        String idStr = StrUtil.join(",", ids);
        List<Post> posts = query().in("id", ids).eq("userId",id).last("ORDER BY FIELD(id," + idStr + ")").list();
        //5.2 惰性清理：posts 已被 userId 过滤，不能直接拿它判断幽灵，
        // 需按"帖子是否存在"（不区分作者）判断，避免误删其他作者仍存在的帖子
        Set<Long> aliveIds = query().in("id", ids).list().stream()
                .map(Post::getId)
                .collect(Collectors.toSet());
        List<String> ghostIds = ids.stream()
                .filter(feedId -> !aliveIds.contains(feedId))
                .map(String::valueOf)
                .collect(Collectors.toList());
        if (!ghostIds.isEmpty()) {
            stringRedisTemplate.opsForZSet().remove(key, ghostIds.toArray(new String[0]));
            log.info("feed 惰性清理：用户 {} 移除了 {} 条已删除帖子的记录", userId, ghostIds.size());
        }
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
    public Page<Post> queryPostByPage(PostQueryRequest postQueryRequest, HttpServletRequest request) {
        // 查询数据库
        Page<Post> postPage = this.page(new Page<>(postQueryRequest.getCurrent(), postQueryRequest.getPageSize()),
                getQueryWrapper(postQueryRequest, request));
        // 补充帖子关联信息：作者、是否点赞、所属花田、私有视频签名 URL
        postPage.getRecords().forEach(post -> {
            this.queryPostUser(post);
            this.isPostLiked(post, request);
            this.queryPostField(post);
        });
        return postPage;
    }

    @Override
    public PostUploadVO uploadPostPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        if (inputSource == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片为空");
        }
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 空间权限校验
        Long spaceId = pictureUploadRequest.getSpaceId();
        Space space = spaceService.getById(spaceId);
        if (spaceId != null) {

            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 必须空间创建人（管理员）才能上传
//            if (!loginUser.getId().equals(space.getUserId())) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
//            }
            // 校验额度
            if(space.getSpaceType()==0||space.getSpaceType()==1){
                if (space.getTotalCount() >= space.getMaxCount()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
                }
                if (space.getTotalSize() >= space.getMaxSize()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
                }
            }
            // 花田空间容量检查（满时通知管理员并给予300MB宽限，宽限用完后禁止上传）
//            if (space.getSpaceType() == 2) {
//                spaceCapacityManager.checkFieldSpaceCapacity(space);
//            }
        }


        // 用于判断是新增还是更新图片
        Long pictureId = null;

        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            File oldFile = pictureService.getById(pictureId);
            ThrowUtils.throwIf(oldFile == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅本人或管理员可编辑
//            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//            }
            // 校验空间是否一致
            // 没传 spaceId，则复用原有图片的 spaceId
            if (spaceId == null) {
                if (oldFile.getSpaceId() != null) {
                    spaceId = oldFile.getSpaceId();
                }
            } else {
                // 传了 spaceId，必须和原有图片一致
                if (ObjUtil.notEqual(spaceId, oldFile.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
                }
            }
        }

        // 上传图片，得到信息




        // 按照用户 id 划分目录 => 按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }

        // 根据 inputSource 类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的文件信息
        File file = new File();
        // 补充设置 spaceId
        file.setSpaceId(spaceId);
        file.setUrl(uploadPictureResult.getUrl());
        file.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();

        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        file.setName(picName);
        file.setSize(uploadPictureResult.getPicSize());
        file.setPicWidth(uploadPictureResult.getPicWidth());
        file.setPicHeight(uploadPictureResult.getPicHeight());
        file.setPicScale(uploadPictureResult.getPicScale());
        file.setPicColor(uploadPictureResult.getPicColor());
        file.setFormat(uploadPictureResult.getPicFormat());
        file.setUserId(loginUser.getId());
        // 帖子图片：应用「允许他人下载」标记（0-不允许 1-允许）
        if (pictureUploadRequest.getCanDownload() != null) {
            file.setCanDownload(pictureUploadRequest.getCanDownload());
        }
        // 补充审核参数（帖子图片不需要审核，直接通过）
        file.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        file.setReviewerId(null);
        file.setReviewMessage("帖子资源自动过审");
        file.setReviewTime(new Date());
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            file.setId(pictureId);
            file.setEditTime(new Date());
        }
        // 构造要入库的帖子信息
        Post post = new Post();
        post.setImages(file.getUrl());
        post.setSpaceId(pictureUploadRequest.getSpaceId());
        post.setUserId(loginUser.getId());

        // 开启事务
        Long finalSpaceId = spaceId;
        PostUploadVO postUploadVO = transactionTemplate.execute(status -> {
            boolean result = pictureService.saveOrUpdate(file);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            if( space != null&&(space.getStatus()!=0)){
                if (finalSpaceId != null) {
                    boolean update = spaceService.lambdaUpdate()
                            .eq(Space::getId, finalSpaceId)
                            .setSql("totalSize = totalSize + " + file.getSize())
                            .setSql("totalCount = totalCount + 1")
                            .update();
                    ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
                }}
            boolean postResult = saveOrUpdate(post);
            ThrowUtils.throwIf(!postResult, ErrorCode.OPERATION_ERROR, "帖子保存失败");
            // 返回帖子 id + 资源 url（前端回显，可继续填写标题/内容后更新帖子）
            PostUploadVO vo = new PostUploadVO();
            vo.setPostId(post.getId());
            vo.setUrl(file.getUrl());
            return vo;
        });
        //PictureVO pictureVO = PictureVO.objToVo(file);
        // 私有视频返回前端前转为临时签名 URL（图片保持公有直链）
        //pictureService.signVideoUrl(pictureVO);
        return postUploadVO;
    }

    @Override
    public PostUploadVO uploadPostPictureByUrl(PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(pictureUploadRequest == null || StrUtil.isBlank(pictureUploadRequest.getFileUrl()),
                ErrorCode.PARAMS_ERROR, "图片 URL 不能为空");
        return uploadPostPicture(pictureUploadRequest.getFileUrl(), pictureUploadRequest, loginUser);
    }

    @Override
    public PostUploadVO uploadPostVideo(MultipartFile multipartFile, PostVideoUploadRequest postVideoUploadRequest, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(multipartFile == null || multipartFile.isEmpty(), ErrorCode.PARAMS_ERROR, "视频文件不能为空");
        ThrowUtils.throwIf(postVideoUploadRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        Long spaceId = postVideoUploadRequest.getSpaceId();
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

        // 2. 原始文件名后缀（用于创建临时文件）
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = "";
        if (StrUtil.isNotBlank(originalFilename) && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 3. MultipartFile 转本地临时文件
        java.io.File tempFile;
        try {
            tempFile = java.io.File.createTempFile("post_video_", suffix);
            multipartFile.transferTo(tempFile);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "视频临时文件创建失败");
        }

        try {
            // 4. MD5：优先使用前端计算值，为空则后端流式计算兜底
            String md5 = postVideoUploadRequest.getMd5();
            if (StrUtil.isBlank(md5)) {
                md5 = calculateFileMd5(tempFile);
            }

            // 5. 事务：视频入库 + 空间容量更新（原逻辑，内部 REQUIRED 传播加入本事务）+ 帖子入库
            String finalMd5 = md5;
            return transactionTemplate.execute(status -> {
                // 5.1 上传视频并入库（含空间容量更新），返回的 url 为临时签名 URL
                VideoVO videoVO = videosService.IdempotentMultipartUploader(tempFile, spaceId, finalMd5, null, loginUser).getData();
                ThrowUtils.throwIf(videoVO == null || StrUtil.isBlank(videoVO.getUrl()), ErrorCode.OPERATION_ERROR, "视频上传失败");
                // 5.1.1 应用「允许他人下载」标记（0-不允许；默认允许则不处理），同一事务内更新文件
                if (postVideoUploadRequest.getCanDownload() != null && postVideoUploadRequest.getCanDownload() == 0) {
                    File fileUpdate = new File();
                    fileUpdate.setId(videoVO.getId());
                    fileUpdate.setCanDownload(0);
                    boolean fileResult = pictureService.updateById(fileUpdate);
                    ThrowUtils.throwIf(!fileResult, ErrorCode.OPERATION_ERROR, "下载权限设置失败");
                }
                // 5.2 去掉签名参数入库原始 URL（签名过期后帖子里的视频仍可播放，浏览时后端会重新签名）
                String rawUrl = stripUrlQuery(videoVO.getUrl());
                // 5.3 帖子入库：仅存资源 url、空间 id、用户 id，其他字段默认
                Post post = new Post();
                post.setImages(rawUrl);
                post.setSpaceId(spaceId);
                post.setUserId(loginUser.getId());
                boolean postResult = save(post);
                ThrowUtils.throwIf(!postResult, ErrorCode.OPERATION_ERROR, "帖子保存失败");
                // 5.4 返回帖子 id + 签名 URL（前端回显播放）+ 缩略图，可继续填写标题/内容后更新帖子
                PostUploadVO vo = new PostUploadVO();
                vo.setPostId(post.getId());
                vo.setUrl(videoVO.getUrl());
                vo.setThumbnailUrl(videoVO.getThumbnailUrl());
                return vo;
            });
        } finally {
            // 清理临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * 流式计算文件 MD5（避免大视频一次性读入内存）
     */
    private String calculateFileMd5(java.io.File file) {
        try (java.io.InputStream inputStream = new java.io.FileInputStream(file)) {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }
            return cn.hutool.core.util.HexUtil.encodeHexStr(digest.digest());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件 MD5 计算失败");
        }
    }

    /**
     * 去掉 URL 上的签名参数（? 之后的部分），得到原始 URL
     */
    private String stripUrlQuery(String url) {
        if (StrUtil.isBlank(url)) {
            return url;
        }
        int idx = url.indexOf('?');
        return idx >= 0 ? url.substring(0, idx) : url;
    }

    @Override
    public boolean publishPost(Post post, User loginUser) {
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
        // 仅帖子创建者可补全自己的帖子
        if (!Objects.equals(oldPost.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        Post updatePost = new Post();
        updatePost.setId(id);
        // 只更新标题/内容（传了才更新，避免覆盖为空）
        if (StrUtil.isNotBlank(post.getTitle())) {
            updatePost.setTitle(post.getTitle());
        }
        if (StrUtil.isNotBlank(post.getContent())) {
            updatePost.setContent(post.getContent());
        }
        return this.updateById(updatePost);
    }

    /**
     * 构建帖子统一分页查询条件
     */
    private QueryWrapper<Post> getQueryWrapper(PostQueryRequest postQueryRequest, HttpServletRequest request) {
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        Long spaceId = postQueryRequest.getSpaceId();
        Long userId = postQueryRequest.getUserId();
        String searchText = postQueryRequest.getSearchText();
        String sortField = postQueryRequest.getSortField();
        String sortOrder = postQueryRequest.getSortOrder();
        boolean onlyMe = postQueryRequest.isOnlyMe();
        boolean onlyFollowed = postQueryRequest.isOnlyFollowed();

        // 空间（花田）筛选
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        // 用户筛选：onlyMe 优先（服务端取登录用户），其次显式 userId
        if (onlyMe) {
            User loginUser = userService.getLoginUser(request);
            queryWrapper.eq("userId", loginUser.getId());
        } else {
            queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        }
        // 关注动态流：只看当前登录用户关注的人的帖子
        if (onlyFollowed) {
            User loginUser = userService.getLoginUser(request);
            List<Long> followIds = followService.query()
                    .eq("follow_user_id", loginUser.getId())
                    .list()
                    .stream()
                    .map(Follow::getUserId)
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(followIds)) {
                // 没有关注任何人：返回空结果
                queryWrapper.eq("id", -1L);
            } else {
                queryWrapper.in("userId", followIds);
            }
        }
        // 搜索词（标题 / 内容模糊搜索）
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("title", searchText)
                    .or()
                    .like("content", searchText));
        }
        // 排序：指定 sortField 时按该字段排序，否则默认按创建时间倒序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), "ascend".equals(sortOrder), sortField);
        queryWrapper.orderByDesc(StrUtil.isBlank(sortField), "createTime");
        return queryWrapper;
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
        // 私有视频转临时签名 URL（图片保持公有直链）
        signPostVideoUrls(post);
    }

    /**
     * 将帖子 images 中的视频 URL 转为临时签名 URL（防直接下载）。
     * 仅处理本站 COS 视频对象（对象已设为私有读）；图片/外链保持原样。
     */
    private void signPostVideoUrls(Post post) {
        if (post == null || StrUtil.isBlank(post.getImages())) {
            return;
        }
        String[] urls = post.getImages().split(",");
        boolean changed = false;
        for (int i = 0; i < urls.length; i++) {
            String url = urls[i].trim();
            if (StrUtil.isBlank(url) || !isVideoUrl(url)) {
                continue;
            }
            String signed = cosManager.signCosUrl(url, SIGN_URL_EXPIRE_SECONDS);
            if (!signed.equals(url)) {
                urls[i] = signed;
                changed = true;
            }
        }
        if (changed) {
            post.setImages(String.join(",", urls));
        }
    }

    /**
     * 判断 URL 是否为视频（按扩展名，与前端 getPostVideo 保持一致）
     */
    private boolean isVideoUrl(String url) {
        int qIdx = url.indexOf('?');
        String path = qIdx >= 0 ? url.substring(0, qIdx) : url;
        int dotIdx = path.lastIndexOf('.');
        if (dotIdx < 0 || dotIdx == path.length() - 1) {
            return false;
        }
        String ext = path.substring(dotIdx + 1).toLowerCase();
        return VIDEO_FORMATS.contains(ext);
    }
    private void queryPostField(Post post){
        Long spaceId = post.getSpaceId();
        if (spaceId == null || spaceId <= 0) {
            return;
        }
        Space space = spaceService.getById(spaceId);
        if (space != null) {
            post.setField(space.getSpaceName());
        }
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
}




