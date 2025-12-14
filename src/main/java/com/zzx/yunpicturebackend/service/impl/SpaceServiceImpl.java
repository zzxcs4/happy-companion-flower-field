package com.zzx.yunpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.yunpicturebackend.exception.BusinessException;
import com.zzx.yunpicturebackend.exception.ErrorCode;
import com.zzx.yunpicturebackend.exception.ThrowUtils;

import com.zzx.yunpicturebackend.model.dto.space.SpaceAddRequest;
import com.zzx.yunpicturebackend.model.dto.space.SpaceEditRequest;
import com.zzx.yunpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zzx.yunpicturebackend.model.entity.Space;
import com.zzx.yunpicturebackend.model.entity.SpaceUser;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.enums.SpaceLevelEnum;
import com.zzx.yunpicturebackend.model.enums.SpaceRoleEnum;
import com.zzx.yunpicturebackend.model.enums.SpaceTypeEnum;
import com.zzx.yunpicturebackend.model.vo.SpaceVO;
import com.zzx.yunpicturebackend.model.vo.UserVO;
import com.zzx.yunpicturebackend.service.SpaceService;
import com.zzx.yunpicturebackend.mapper.SpaceMapper;
import com.zzx.yunpicturebackend.service.SpaceUserService;
import com.zzx.yunpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author zzx
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-09-23 20:14:09
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private SpaceUserService spaceUserService;

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        // 默认值
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceType() == null) {
            spaceAddRequest.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 填充数据
        if(spaceAddRequest.getSpaceType()==0||spaceAddRequest.getSpaceType()==1){
            if (spaceAddRequest.getSpaceLevel() == null&&(space.getSpaceType()==0||space.getSpaceType()==1)) {
                space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
            }
            this.fillSpaceBySpaceLevel(space);
        }
        // 数据校验
        this.validSpace(space, true);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 权限校验
        if(spaceAddRequest.getSpaceType()==0||spaceAddRequest.getSpaceType()==1){
        if (SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        }
        // 针对用户进行加锁
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                if(spaceAddRequest.getSpaceType()==0||spaceAddRequest.getSpaceType()==1){
                if (!userService.isAdmin(loginUser)) {
                    boolean exists = this.lambdaQuery()
                            .eq(Space::getUserId, userId)
                            .eq(Space::getSpaceType, spaceAddRequest.getSpaceType())
                            .exists();
                    ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户除创建花田时创建的花田空间外每类空间仅能创建一个");
                }}
                // 写入数据库
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
                // 如果是团队空间，关联新增团队成员记录
                if (SpaceTypeEnum.TEAM.getValue() == spaceAddRequest.getSpaceType()||SpaceTypeEnum.FIELD.getValue() == spaceAddRequest.getSpaceType()) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    result = spaceUserService.save(spaceUser);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
                }
                // 返回新写入的数据 id
                return space.getId();

            });
            // 返回结果是包装类，可以做一些处理
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Long userId = spaceQueryRequest.getUserId();
        int pageSize = spaceQueryRequest.getPageSize();
        String sortOrder = spaceQueryRequest.getSortOrder();
        String sortField = spaceQueryRequest.getSortField();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);

        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    /**
     * 分页获取空间封装
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> SpaceList = spacePage.getRecords();
        Page<SpaceVO> SpaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(SpaceList)) {
            return SpaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> SpaceVOList = SpaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = SpaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        SpaceVOList.forEach(SpaceVO -> {
            Long userId = SpaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            SpaceVO.setUser(userService.getUserVO(user));
        });
        SpaceVOPage.setRecords(SpaceVOList);
        return SpaceVOPage;
    }
    @Override
    public boolean deleteSpace(long SpaceId, User loginUser) {
        ThrowUtils.throwIf(SpaceId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在
        Space oldSpace = this.getById(SpaceId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        //checkSpaceAuth(loginUser, oldSpace);
        // 操作数据库
        boolean result = this.removeById(SpaceId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }
    @Override
    public void editSpace(SpaceEditRequest SpaceEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Space Space = new Space();
        BeanUtils.copyProperties(SpaceEditRequest, Space);

        // 设置编辑时间
        Space.setEditTime(new Date());
        // 数据校验
        this.validSpace(Space,false);
        // 判断是否存在
        long id = SpaceEditRequest.getId();
        Space oldSpace = this.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        //checkSpaceAuth(loginUser, oldSpace);
        // 操作数据库
        boolean result = this.updateById(Space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void validSpace(Space space, boolean add) {

        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        // 要创建
        if (add) {
            if (spaceType == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不能为空");
            }
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if(spaceTypeEnum==SpaceTypeEnum.PRIVATE||spaceTypeEnum==SpaceTypeEnum.TEAM){
                if (spaceLevel == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
                }
            }
        }
        // 修改数据时，如果要改空间级别
        if(spaceTypeEnum==SpaceTypeEnum.PRIVATE||spaceTypeEnum==SpaceTypeEnum.TEAM){
            if (spaceLevel != null && spaceLevelEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
            }
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        // 修改数据时，如果要改空间级别
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不存在");
        }
    }



    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 空间权限校验
     *
     * @param loginUser
     * @param space
     */
    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        // 仅本人或管理员可访问
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }


}




