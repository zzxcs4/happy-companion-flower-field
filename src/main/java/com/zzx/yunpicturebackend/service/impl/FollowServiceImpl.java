package com.zzx.yunpicturebackend.service.impl;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Autowired
    private UserService userService;
    // 关注列表 Redis 缓存（follows:{userId}）相关操作已注释（暂未启用），字段一并注释
    // @Autowired
    // private StringRedisTemplate stringRedisTemplate;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Override
    @Transactional
    public BaseResponse<Boolean> follow(Long followUserId, Boolean isFollow, HttpServletRequest request) {
        //获取登录用户
        User loginUser= userService.getLoginUser(request);
        //1.判断是关注还是取关
        if (isFollow) {
            //2.关注，新增
            Follow follow=new Follow();
            follow.setUserId(loginUser.getId());
            follow.setFollowUserId(followUserId);

                boolean isSuccess = save(follow);

                if(isSuccess){
                    // 3. 更新被关注用户的“粉丝数”（有多少人关注他），原子自增避免并发丢失更新
                    userService.update().setSql("fansNum = fansNum + 1").eq("id", followUserId).update();
                    // 关注列表 Redis 缓存（follows:{userId}）仅被已注释的共同关注接口读取，暂未启用，
                    // 为避免无谓的 Redis 写入及与 DB 事务不一致，暂注释掉，待启用共同关注时再恢复并配合事务后置写入
                    // stringRedisTemplate.opsForSet().add(key,followUserId.toString());

                }

        }else {
            boolean isSuccess = remove(new QueryWrapper<Follow>()
                    .eq("user_id",loginUser.getId()).eq("follow_user_id",followUserId));
            //把关注的用户id从Redis中移除（同 add 一样，缓存暂未启用，一并注释）
            if(isSuccess){
                // stringRedisTemplate.opsForSet().remove(key,followUserId.toString());
            }
            //Integer fansNum = query().eq("follow_user_id", loginUser.getId()).count().intValue();
            //loginUser.setFansNum(fansNum);
            userService.update().setSql("fansNum = fansNum - 1").eq("id", followUserId).update(); ;
            //userService.updateById(loginUser);
        }
        return ResultUtils.success(true);
    }

    @Override
    public BaseResponse<Boolean> isFollow(Long followUserId,HttpServletRequest request) {
        //获取登录用户
        Long userId = userService.getLoginUser(request).getId();
        //1.查询是否关注
        Integer count = query().eq("user_id", userId)
                .eq("follow_user_id", followUserId).count().intValue();

        return ResultUtils.success(count>0);
    }

    /**
     * 共同关注：基于 Redis follows:{userId} 集合求交集。
     * 该 Redis 缓存目前仅此方法使用，且 Controller 入口已被注释（暂未启用），
     * 为清理无效缓存，方法体暂注释。待启用共同关注时，可改为直接查 DB 求交集（更可靠），
     * 或恢复 Redis 缓存并配套事务后置写入。
     */
    //@Override
    //public BaseResponse<List<User>> followCommons(Long id,HttpServletRequest request) {
    //    //1.获取当前登录用户
    //    Long userId = userService.getLoginUser(request).getId();
    //    String key="follows:"+userId;
    //    //2.求交集
    //    String key2="follows:"+id;
    //    Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
    //    if(intersect==null||intersect.isEmpty()){
    //        return ResultUtils.success(Collections.emptyList());
    //    }
    //    //3.解析id集合
    //    List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
    //    //4.查询用户
    //    List<User> users = userService.listByIds(ids)
    //            .stream()
    //            .map(user -> BeanUtil.copyProperties(user, User.class))
    //            .collect(Collectors.toList());
    //    return ResultUtils.success(users);
    //}

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
    public List<User> fans(Long followUserId) {
        //1.获取当前登录用户

        List<Follow> follows = query().eq("follow_user_id", followUserId).list();
        List<Long> fansId = follows.stream().map(Follow::getUserId).collect(Collectors.toList());
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if(fansId!=null&&fansId.size()>0){
            queryWrapper.in("id", fansId);
            return userService.list(queryWrapper);
        }else {
            return Collections.emptyList();
        }
    }
}
