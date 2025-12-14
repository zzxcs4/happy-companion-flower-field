package com.zzx.yunpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.common.ResultUtils;
import com.zzx.yunpicturebackend.mapper.FollowMapper;
import com.zzx.yunpicturebackend.model.entity.Follow;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.service.IFollowService;
import com.zzx.yunpicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private UserService userService;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Override
    @Transactional
    public BaseResponse<Boolean> follow(Long followUserId, Boolean isFollow, HttpServletRequest request) {
        //获取登录用户
        User loginUser= userService.getLoginUser(request);
        String key="follows:"+loginUser.getId();
        //1.判断是关注还是取关
        if (isFollow) {
            //2.关注，新增
            Follow follow=new Follow();
            follow.setUserId(loginUser.getId());
            follow.setFollowUserId(followUserId);

                boolean isSuccess = save(follow);

                if(isSuccess){
                    // 3. 更新被关注用户的“粉丝数”（有多少人关注他）
                    User followUser = userService.getById(followUserId); // 查询被关注用户
                    if (followUser != null) {
                        followUser.setFansNum(followUser.getFansNum() + 1); // 粉丝数+1
                        userService.updateById(followUser);
                    }
                    stringRedisTemplate.opsForSet().add(key,followUserId.toString());

                }

        }else {
            boolean isSuccess = remove(new QueryWrapper<Follow>()
                    .eq("user_id",loginUser.getId()).eq("follow_user_id",followUserId));
            //把关注的用户id从Redis中移除
            if(isSuccess){
            stringRedisTemplate.opsForSet().remove(key,followUserId.toString());
            }
            Integer fansNum = query().eq("follow_user_id", loginUser.getId()).count().intValue();
            loginUser.setFansNum(fansNum);
            userService.updateById(loginUser);
        }
        return ResultUtils.success(true);
    }

    @Override
    public BaseResponse<Boolean> isFollow(Long followUserId,HttpServletRequest request) {
        //获取登录用户
        Long userId = userService.getLoginUser(request).getId();
        //1.查询是否关注
        Integer count = query().eq("user_id", followUserId)
                .eq("follow_user_id", followUserId).count().intValue();

        return ResultUtils.success(count>0);
    }

    @Override
    public BaseResponse<List<User>> followCommons(Long id,HttpServletRequest request) {
        //1.获取当前登录用户
        Long userId = userService.getLoginUser(request).getId();
        String key="follows:"+userId;
        //2.求交集
        String key2="follows:"+id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
        if(intersect==null||intersect.isEmpty()){
            return ResultUtils.success(Collections.emptyList());
        }
        //3.解析id集合
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        //4.查询用户
        List<User> users = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, User.class))
                .collect(Collectors.toList());
        return ResultUtils.success(users);
    }

    @Override
    public List<User> follows(Long userId) {
        //1.获取当前登录用户

        List<Follow> follows = query().eq("user_id", userId).list();
        List<Long> followsId = follows.stream().map(Follow::getFollowUserId).collect(Collectors.toList());
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if(followsId!=null&&followsId.size()>0){
            queryWrapper.in("id", followsId);
            return userService.list(queryWrapper);
        }else {
            return Collections.emptyList();
        }

    }

    @Override
    public BaseResponse<List<User>> fans(Long followUserId, HttpServletRequest request) {
        //1.获取当前登录用户

        List<Follow> follows = query().eq("follow_user_id", followUserId).list();
        List<Long> fansId = follows.stream().map(Follow::getUserId).collect(Collectors.toList());
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if(fansId!=null&&fansId.size()>0){
            queryWrapper.in("id", fansId);
            return ResultUtils.success(userService.list(queryWrapper));
        }else {
            return ResultUtils.success(Collections.emptyList());
        }
    }
}
