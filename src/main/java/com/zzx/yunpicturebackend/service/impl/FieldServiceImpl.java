package com.zzx.yunpicturebackend.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.common.ResultUtils;
import com.zzx.yunpicturebackend.exception.BusinessException;
import com.zzx.yunpicturebackend.exception.ErrorCode;
import com.zzx.yunpicturebackend.exception.ThrowUtils;
import com.zzx.yunpicturebackend.mapper.FieldMapper;
import com.zzx.yunpicturebackend.model.dto.field.FieldAddRequest;
import com.zzx.yunpicturebackend.model.dto.field.FieldJoinRequest;
import com.zzx.yunpicturebackend.model.dto.field.FieldQueryRequest;
import com.zzx.yunpicturebackend.model.dto.field.FieldQuitRequest;
import com.zzx.yunpicturebackend.model.dto.space.SpaceAddRequest;
import com.zzx.yunpicturebackend.model.entity.Field;
import com.zzx.yunpicturebackend.model.entity.Space;
import com.zzx.yunpicturebackend.model.entity.SpaceUser;
import com.zzx.yunpicturebackend.model.entity.UserField;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.enums.FieldStatusEnum;
import com.zzx.yunpicturebackend.model.enums.SpaceLevelEnum;
import com.zzx.yunpicturebackend.model.enums.SpaceRoleEnum;
import com.zzx.yunpicturebackend.model.enums.SpaceTypeEnum;
import com.zzx.yunpicturebackend.model.vo.FieldUserVO;
import com.zzx.yunpicturebackend.model.vo.UserVO;
import com.zzx.yunpicturebackend.service.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author zzx
* @description 针对表【field(花田)】的数据库操作Service实现（已停用，社区概念已并入空间）
* @createDate 2025-10-17 18:48:45
* @Deprecated
*/
//@Service
public class FieldServiceImpl extends ServiceImpl<FieldMapper, Field>
    implements FieldService{
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private SpaceService spaceService;
    @Resource
    private UserService userService;
    @Resource
    private UserFieldService userFieldService;
    @Resource
    private PostService postService;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    @Lazy
    private UserFieldScoreService userFieldScoreService;
    @Resource
    private SpaceUserService spaceUserService;
    @Override
    public long addField(FieldAddRequest fieldAddRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Field field = new Field();
        BeanUtils.copyProperties(fieldAddRequest, field);
        // 默认值
        if (StrUtil.isBlank(fieldAddRequest.getName())) {
            field.setName(loginUser.getUserName()+"的花田"+ RandomUtil.randomNumber());
        }
        // 数据校验
        this.validField(field, true);
        Long userId = loginUser.getId();
        field.setUserId(userId);

        QueryWrapper<Space> spaceQueryWrapper = new QueryWrapper<>();
        spaceQueryWrapper.eq("userId", userId);
        // 针对用户进行加锁
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {

            Long newFieldId = transactionTemplate.execute(status -> {


                //如果创建花田时没有图库空间，默认创建。
                // 公开花田（status=0）使用花田空间（无限制），私有/加密花田使用团队空间（有容量限制）
                SpaceAddRequest sr = new SpaceAddRequest();
                if (field.getStatus() != null && field.getStatus() != 0) {
                    // 私有(1)或加密(2)花田 -> 团队空间，有容量限制
                    sr.setSpaceType(SpaceTypeEnum.TEAM.getValue());
                    sr.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
                } else {
                    // 公开花田 -> 花田空间，无限制
                    sr.setSpaceType(SpaceTypeEnum.FIELD.getValue());
                }
                sr.setSpaceName(fieldAddRequest.getName()+"的空间"+loginUser.getUserName());

                long SpaceId = spaceService.addSpace(sr, loginUser);
                field.setSpaceId(SpaceId);

//            if (SpaceId==0) {
//                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间创建失败");
//            }
                // 写入数据库
            boolean result = this.save(field);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
                UserField userField = new UserField();
                userField.setUserId(userId);
                if(field.getId()==null){
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }else {
                    userField.setFieldId(field.getId());
                    // 写入数据库
                    userFieldService.save(userField);
                }
                // 返回新写入的数据 id
                return field.getId();

            });
            // 返回结果是包装类，可以做一些处理
            return Optional.ofNullable(newFieldId).orElse(-1L);
        }

    }
    @Override
    public void validField(Field field, boolean add) {

        ThrowUtils.throwIf(field == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String fieldName = field.getName();
        // 要创建
        if (add) {
//            if (field.getMaxNum() == null) {
//                throw new BusinessException(ErrorCode.PARAMS_ERROR, "花田最大人数不能为空");
//            }
            if (StrUtil.isBlank(fieldName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "花田名称不能为空");
            }
            if (field.getStatus() == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "花田状态不能为空");
            }
            if(field.getStatus() == 2) {
                if(StrUtil.isBlank(field.getPassword())) {throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");}

            }
        }

        if (StrUtil.isNotBlank(fieldName) && fieldName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }

    }

    @Override
    public boolean deleteField(long id, User loginUser) {
        Field field = getFieldById(id);
        long fieldId = field.getId();
        if (!Objects.equals(field.getUserId(), loginUser.getId())&&!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        QueryWrapper<UserField> userFieldQueryWrapper = new QueryWrapper<>();
        userFieldQueryWrapper.eq("fieldId", fieldId);
        boolean result = userFieldService.remove(userFieldQueryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除花田关联信息失败");
        }

        Long spaceId = field.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            QueryWrapper<SpaceUser> spaceUserQueryWrapper = new QueryWrapper<>();
            spaceUserQueryWrapper.eq("spaceId", spaceId);
            spaceUserService.remove(spaceUserQueryWrapper);
            spaceService.removeById(spaceId);
        }

        return this.removeById(fieldId);
    }
    private Field getFieldById(Long fieldId) {
        if (fieldId == null || fieldId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Field field = this.getById(fieldId);
        if (field == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"队伍不存在");
        }
        return field;
    }
    private Space getSpaceById(Long spaceId) {
        if (spaceId == null || spaceId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Space space = spaceService.getById(spaceId);
        if (space == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"队伍不存在");
        }
        return space;
    }

    @Override
    public List<FieldUserVO>  listFields(FieldQueryRequest fieldQueryRequest,boolean isAdmin) {
        if (fieldQueryRequest == null) {
            return new ArrayList<>();
        }
        QueryWrapper<Field> queryWrapper = new QueryWrapper<>();
        // 从对象中取值

        Integer status = fieldQueryRequest.getStatus();
        String searchText = fieldQueryRequest.getSearchText();
        Integer maxNum = fieldQueryRequest.getMaxNum();
        List<Long> idList = fieldQueryRequest.getIdList();
        Long userId = fieldQueryRequest.getUserId();
        String sortOrder = fieldQueryRequest.getSortOrder();
        String sortField = fieldQueryRequest.getSortField();
        String firstCategoryName = fieldQueryRequest.getFirstCategoryName();
        String secondCategoryName = fieldQueryRequest.getSecondCategoryName();

        queryWrapper.in(CollectionUtils.isNotEmpty(idList),"id", idList);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId),"userId", userId);
        // 对 searchText 的多个条件用 or() 分组，避免影响其他条件
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw
                    .like("name", searchText)
                    .or().like("description", searchText)
                    .or().eq("firstCategory", searchText)
                    .or().eq("secondCategory", searchText)
            );
        }
        // 根据状态来查询
        if(status!=null){


        FieldStatusEnum statusEnum = FieldStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            statusEnum = FieldStatusEnum.PUBLIC;
        }

        if (!isAdmin && statusEnum.equals(FieldStatusEnum.PRIVATE)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        queryWrapper.eq("status", statusEnum.getValue());
        }

        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        List<Field> fieldlist = this.list(queryWrapper);

        List<FieldUserVO> fieldUserVOList = new ArrayList<>();
        // 关联查询创建人的用户信息
        for (Field Field : fieldlist) {
            Long userIdc = Field.getUserId();
            if (userIdc == null) {
                continue;
            }
            User user = userService.getById(userIdc);
            FieldUserVO fieldUserVO = new FieldUserVO();

            BeanUtils.copyProperties(Field, fieldUserVO);
            fieldUserVO.setPostNum(postService.query().eq("fieldId",Field.getId()).count().intValue());
            // postNum 已由 savePost/deletePost 维护，直接使用数据库字段
            // 脱敏用户信息
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                fieldUserVO.setCreateUser(userVO);
            }
            fieldUserVOList.add(fieldUserVO);
        }
        return fieldUserVOList;
    }



    @Override
    public boolean updateField(Field Field, User loginUser) {
        if (Field == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = Field.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Field oldField = this.getById(id);
        if (oldField == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "花田不存在");
        }
        // 只有管理员或者队伍的创建者可以修改
        if (!Objects.equals(oldField.getUserId(), loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        FieldStatusEnum statusEnum = FieldStatusEnum.getEnumByValue(Field.getStatus());
        if (statusEnum.equals(FieldStatusEnum.SECRET)) {
            if (StringUtils.isBlank(Field.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密花田必须要设置密码");
            }
        }
        Field updateField = new Field();
        BeanUtils.copyProperties(Field, updateField);
        return this.updateById(updateField);
    }

    @Override
    public boolean joinField(FieldJoinRequest FieldJoinRequest, User loginUser) {
        if (FieldJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long FieldId = FieldJoinRequest.getFieldId();
        Field Field = getFieldById(FieldId);
        Integer status = Field.getStatus();
        FieldStatusEnum FieldStatusEnum = com.zzx.yunpicturebackend.model.enums.FieldStatusEnum.getEnumByValue(status);
        if (FieldStatusEnum.PRIVATE.equals(FieldStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        String password = FieldJoinRequest.getPassword();
        if (FieldStatusEnum.SECRET.equals(FieldStatusEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(Field.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        // 该用户已加入的队伍数量
        long userId = loginUser.getId();
        // 只有一个线程能获取到锁
        RLock lock = redissonClient.getLock("happy:join_Field");
        try {
            // 抢到锁并执行
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    System.out.println("getLock: " + Thread.currentThread().getId());
                    QueryWrapper<UserField> userFieldQueryWrapper = new QueryWrapper<>();
                    userFieldQueryWrapper.eq("userId", userId);
                    long hasJoinNum = userFieldService.count(userFieldQueryWrapper);
                    if (hasJoinNum > 1000) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入 1000 个社区");
                    }
                    // 不能重复加入已加入的队伍
                    userFieldQueryWrapper = new QueryWrapper<>();
                    userFieldQueryWrapper.eq("userId", userId);
                    userFieldQueryWrapper.eq("fieldId", FieldId);
                    long hasUserJoinField = userFieldService.count(userFieldQueryWrapper);
                    if (hasUserJoinField > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该花田");
                    }
                    // 已加入队伍的人数
                    long FieldHasJoinNum = this.countUserFieldByFieldId(FieldId);
                    if(Field.getMaxNum() != null){
                    if (FieldHasJoinNum >= Field.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "花田已满");
                    }
                    }
                    // 修改队伍信息
                    UserField userField = new UserField();
                    userField.setUserId(userId);
                    userField.setFieldId(FieldId);
                    userField.setJoinTime(new Date());
                    boolean saved = userFieldService.save(userField);
                    // 加入花田 +1 分
                    if (saved) {
                        userFieldScoreService.addScore(userId, FieldId, 1.0);
                        
                        // 同时加入社区空间，权限为浏览者
                        Long spaceId = Field.getSpaceId();
                        if (spaceId != null && spaceId > 0) {
                            // 检查是否已经存在于空间中
                            QueryWrapper<SpaceUser> spaceUserQueryWrapper = new QueryWrapper<>();
                            spaceUserQueryWrapper.eq("spaceId", spaceId);
                            spaceUserQueryWrapper.eq("userId", userId);
                            long existCount = spaceUserService.count(spaceUserQueryWrapper);
                            
                            if (existCount == 0) {
                                // 添加到空间，角色为浏览者
                                SpaceUser spaceUser = new SpaceUser();
                                spaceUser.setSpaceId(spaceId);
                                spaceUser.setUserId(userId);
                                spaceUser.setSpaceRole(SpaceRoleEnum.VIEWER.getValue());
                                spaceUserService.save(spaceUser);
                            }
                        }
                    }
                    return saved;
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
            return false;
        } finally {
            // 只能释放自己的锁
            if (((RLock) lock).isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }
    @Override
    public boolean joinPrivateField(FieldJoinRequest FieldJoinRequest, User loginUser) {
        if (FieldJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long FieldId = FieldJoinRequest.getFieldId();
        Field Field = getFieldById(FieldId);
        Integer status = Field.getStatus();



        // 该用户已加入的队伍数量
        long userId = loginUser.getId();
        // 只有一个线程能获取到锁
        RLock lock = redissonClient.getLock("happy:join_Field");
        try {
            // 抢到锁并执行
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    System.out.println("getLock: " + Thread.currentThread().getId());
                    QueryWrapper<UserField> userFieldQueryWrapper = new QueryWrapper<>();
                    userFieldQueryWrapper.eq("userId", userId);
                    long hasJoinNum = userFieldService.count(userFieldQueryWrapper);
                    if (hasJoinNum > 1000) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入 1000 个社区");
                    }
                    // 不能重复加入已加入的队伍
                    userFieldQueryWrapper = new QueryWrapper<>();
                    userFieldQueryWrapper.eq("userId", userId);
                    userFieldQueryWrapper.eq("fieldId", FieldId);
                    long hasUserJoinField = userFieldService.count(userFieldQueryWrapper);
                    if (hasUserJoinField > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该花田");
                    }
                    // 已加入队伍的人数
                    long FieldHasJoinNum = this.countUserFieldByFieldId(FieldId);
                    if(Field.getMaxNum() != null){
                        if (FieldHasJoinNum >= Field.getMaxNum()) {
                            throw new BusinessException(ErrorCode.PARAMS_ERROR, "花田已满");
                        }
                    }
                    // 修改队伍信息
                    UserField userField = new UserField();
                    userField.setUserId(userId);
                    userField.setFieldId(FieldId);
                    userField.setJoinTime(new Date());
                    boolean saved = userFieldService.save(userField);
                    
                    // 同时加入社区空间，权限为浏览者
                    if (saved) {
                        Long spaceId = Field.getSpaceId();
                        if (spaceId != null && spaceId > 0) {
                            // 检查是否已经存在于空间中
                            QueryWrapper<SpaceUser> spaceUserQueryWrapper = new QueryWrapper<>();
                            spaceUserQueryWrapper.eq("spaceId", spaceId);
                            spaceUserQueryWrapper.eq("userId", userId);
                            long existCount = spaceUserService.count(spaceUserQueryWrapper);
                            
                            if (existCount == 0) {
                                // 添加到空间，角色为浏览者
                                SpaceUser spaceUser = new SpaceUser();
                                spaceUser.setSpaceId(spaceId);
                                spaceUser.setUserId(userId);
                                spaceUser.setSpaceRole(SpaceRoleEnum.VIEWER.getValue());
                                spaceUserService.save(spaceUser);
                            }
                        }
                    }
                    return saved;
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
            return false;
        } finally {
            // 只能释放自己的锁
            if (((RLock) lock).isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }
    @Override
    public boolean quitField(FieldQuitRequest FieldQuitRequest, User loginUser) {
        if (FieldQuitRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long FieldId = FieldQuitRequest.getFieldId();
        Field Field = getFieldById(FieldId);
        long userId = loginUser.getId();
        UserField queryUserField = new UserField();
        queryUserField.setUserId(userId);
        queryUserField.setFieldId(FieldId);
        QueryWrapper<UserField> queryWrapper = new QueryWrapper<>(queryUserField);
        long count = userFieldService.count(queryWrapper);
        if (count == 0 ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"未加入队伍");
        }
        long FieldHasJoinNum = this.countUserFieldByFieldId(FieldId);
        // 队伍只剩一人，解散
        if (FieldHasJoinNum == 1) {
            // 删除队伍
            this.removeById(FieldId);
        } else {
            // 队伍还剩至少两人
            // 是队长
            if (Field.getUserId() == userId) {
                // 把队伍转移给最早加入的用户
                // 1. 查询已加入队伍的所有用户和加入时间
                QueryWrapper<UserField> userFieldQueryWrapper = new QueryWrapper<>();
                userFieldQueryWrapper.eq("FieldId", FieldId);
                userFieldQueryWrapper.last("order by id asc limit 2");
                List<UserField> userFieldList = userFieldService.list(userFieldQueryWrapper);
                if (CollectionUtils.isEmpty(userFieldList) || userFieldList.size() <= 1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserField nextUserField = userFieldList.get(1);
                Long nextFieldLeaderId = nextUserField.getUserId();
                // 更新当前队伍的队长
                Field updateField = new Field();
                updateField.setId(FieldId);
                updateField.setUserId(nextFieldLeaderId);
                boolean result = this.updateById(updateField);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
                }
            }
        }
        // 移除花田关系
        boolean result = userFieldService.remove(queryWrapper);
        
        // 退出社区 -3.0 分
        if (result) {
            userFieldScoreService.addScore(userId, FieldId, -3.0);
        }
        
        // 同时移除社区空间关系
        if (result) {
            Long spaceId = Field.getSpaceId();
            if (spaceId != null && spaceId > 0) {
                // 查询并删除空间用户关系
                QueryWrapper<SpaceUser> spaceUserQueryWrapper = new QueryWrapper<>();
                spaceUserQueryWrapper.eq("spaceId", spaceId);
                spaceUserQueryWrapper.eq("userId", userId);
                spaceUserService.remove(spaceUserQueryWrapper);
            }
        }
        
        return result;
    }

    @Override
    public BaseResponse<List<User>> getFieldMemberListById(Long id , @RequestParam(required = false) String searchText) {
        if (id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<UserField> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("fieldId", id);
        List<UserField> userFieldlist = userFieldService.list(queryWrapper);
        List<Long> userIdList = userFieldlist.stream().map(UserField::getUserId).collect(Collectors.toList());
        QueryWrapper<User> queryWrapper1 = new QueryWrapper<>();
        if (userIdList == null || userIdList.isEmpty()) {
            return ResultUtils.success(new ArrayList<>());
        }
            queryWrapper1.in("id", userIdList); // 条件1：id 在 userIdList 中
            if (searchText != null && !searchText.isEmpty()) {
                // 条件2：用户名/账号/id 包含 searchText（注意 or 的逻辑）
                // 用 and() 嵌套，确保括号内的 or 是在条件A的范围内
                queryWrapper1.and(qw -> qw
                        .like("userName", searchText)
                        .or().like("userAccount", searchText)
                        .or().eq("id", searchText)
                );
            }

        List<User> userList = userService.list(queryWrapper1);
        if (userList == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(userList);
    }

    private long countUserFieldByFieldId(long fieldId) {
        QueryWrapper<UserField> UserFieldQueryWrapper = new QueryWrapper<>();
        UserFieldQueryWrapper.eq("fieldId", fieldId);
        return userFieldService.count(UserFieldQueryWrapper);
    }
}




