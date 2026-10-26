package com.zzx.flowerfieldfrienddisk.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.flowerfieldfrienddisk.annotation.AuthCheck;
import com.zzx.flowerfieldfrienddisk.common.BaseResponse;
import com.zzx.flowerfieldfrienddisk.common.DeleteRequest;
import com.zzx.flowerfieldfrienddisk.common.ResultUtils;
import com.zzx.flowerfieldfrienddisk.constant.UserConstant;
import com.zzx.flowerfieldfrienddisk.exception.BusinessException;
import com.zzx.flowerfieldfrienddisk.exception.ErrorCode;
import com.zzx.flowerfieldfrienddisk.exception.ThrowUtils;
import com.zzx.flowerfieldfrienddisk.manager.auth.SpaceUserAuthManager;
import com.zzx.flowerfieldfrienddisk.model.dto.space.SpaceAddRequest;
import com.zzx.flowerfieldfrienddisk.model.dto.space.SpaceEditRequest;
import com.zzx.flowerfieldfrienddisk.model.dto.space.SpaceJoinRequest;
import com.zzx.flowerfieldfrienddisk.model.dto.space.SpaceQueryRequest;
import com.zzx.flowerfieldfrienddisk.model.dto.space.SpaceQuitRequest;
import com.zzx.flowerfieldfrienddisk.model.dto.space.SpaceUpdateRequest;
import com.zzx.flowerfieldfrienddisk.model.entity.Category;
import com.zzx.flowerfieldfrienddisk.model.entity.Space;
import com.zzx.flowerfieldfrienddisk.model.entity.SpaceUser;
import com.zzx.flowerfieldfrienddisk.model.entity.User;
import com.zzx.flowerfieldfrienddisk.model.enums.SpaceLevelEnum;
import com.zzx.flowerfieldfrienddisk.model.enums.SpaceTypeEnum;
import com.zzx.flowerfieldfrienddisk.model.vo.SpaceLevel;
import com.zzx.flowerfieldfrienddisk.model.vo.SpaceVO;
import com.zzx.flowerfieldfrienddisk.service.CategoryService;
import com.zzx.flowerfieldfrienddisk.service.SpaceService;
import com.zzx.flowerfieldfrienddisk.service.SpaceUserService;
import com.zzx.flowerfieldfrienddisk.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;
@RestController
@Slf4j
@RequestMapping("/space")//s
public class SpaceController {
    @Resource
    private SpaceService spaceService;
    @Resource
    private UserService userService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private CategoryService categoryService;
    @PostMapping("/add")
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        long addSpace = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(addSpace);
    }
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        // 自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        // 数据校验
        spaceService.validSpace(space, false);
        // 判断是否存在
        long id = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values()) // 获取所有枚举
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }
    /**
     * 删除空间
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        Long id = deleteRequest.getId();
        // 判断是否存在
        if (id != null) {
            Space space = spaceService.getById(id);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 必须空间创建人（空间管理员）或管理员才能删除
            if (!loginUser.getId().equals(space.getUserId())&&!userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }
        }
        // 操作数据库
        boolean result = spaceService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 编辑空间（给用户使用）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        if (spaceEditRequest == null || spaceEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 在此处将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest, space);
        // 设置编辑时间
        space.setEditTime(new Date());
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        Long id = spaceEditRequest.getId();
        // 判断是否存在
        if (id != null) {
            Space oldSpace = spaceService.getById(id);
            ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 必须空间创建人（空间管理员）或管理员才能编辑
            if (!loginUser.getId().equals(oldSpace.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }
        }
        // 操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
    /**
     * 分页获取空间列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);
    }
    /**
     * 根据 id 获取空间（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(space);
    }

    /**
     * 根据 id 获取空间（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        //ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList =spaceUserAuthManager.getPermissionList(null,space, loginUser);
        if(permissionList.isEmpty() && space.getSpaceType() == SpaceTypeEnum.FIELD.getValue() && space.getStatus() == 0){
            permissionList.add("post:view");
        }
        spaceVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.success(spaceVO);
    }

    /**
     * 分页获取空间列表（封装类）
     */
    @PostMapping("/list/page/vo")    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                             HttpServletRequest request) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));
        // 获取封装类
        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage, request));
    }

    /**
     * 查询社区空间分类列表
     */
    @GetMapping("/category")
    public BaseResponse<List<Category>> listCategory() {
        List<Category> categories = categoryService.lambdaQuery()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort)
                .list();
        return ResultUtils.success(categories);
    }

    /**
     * 获取社区空间的成员列表
     */
    @GetMapping("/getMemberList")
    public BaseResponse<List<User>> getSpaceMemberListById(Long id, @RequestParam(required = false) String searchText) {
        return ResultUtils.success(spaceService.getSpaceMemberList(id, searchText));
    }

    /**
     * 社区空间列表（相当于花田列表）
     */
    @GetMapping("/list")
    public BaseResponse<List<SpaceVO>> listSpaces(SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        if (spaceQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        // 只查社区空间
        spaceQueryRequest.setSpaceType(SpaceTypeEnum.FIELD.getValue());
        List<SpaceVO> spaceVOList = spaceService.listCommunitySpaces(spaceQueryRequest, userService.isAdmin(loginUser));
        // 填充是否已加入 / 已加入人数
        fillJoinInfo(spaceVOList, loginUser);
        return ResultUtils.success(spaceVOList);
    }

    /**
     * 我创建的社区（花田）
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<SpaceVO>> listMyCreateSpaces(SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        if (spaceQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        spaceQueryRequest.setUserId(loginUser.getId());
        spaceQueryRequest.setSpaceType(SpaceTypeEnum.FIELD.getValue());
        List<SpaceVO> spaceVOList = spaceService.listCommunitySpaces(spaceQueryRequest, true);
        fillJoinInfo(spaceVOList, loginUser);
        return ResultUtils.success(spaceVOList);
    }

    /**
     * 我加入的社区（花田）
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<SpaceVO>> listMyJoinSpaces(SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        if (spaceQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        List<SpaceUser> spaceUserList = spaceUserService.list(queryWrapper);
        List<Long> idList = spaceUserList.stream()
                .map(SpaceUser::getSpaceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(idList)) {
            return ResultUtils.success(new ArrayList<>());
        }
        spaceQueryRequest.setIdList(idList);
        spaceQueryRequest.setSpaceType(SpaceTypeEnum.FIELD.getValue());
        List<SpaceVO> spaceVOList = spaceService.listCommunitySpaces(spaceQueryRequest, true);
        fillJoinInfo(spaceVOList, loginUser);
        return ResultUtils.success(spaceVOList);
    }

    /**
     * 加入社区（公开/加密）
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinSpace(@RequestBody SpaceJoinRequest spaceJoinRequest, HttpServletRequest request) {
        if (spaceJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = spaceService.joinSpace(spaceJoinRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 加入私有社区（邀请）
     */
    @PostMapping("/join/private")
    public BaseResponse<Boolean> joinPrivateSpace(@RequestBody SpaceJoinRequest spaceJoinRequest, HttpServletRequest request) {
        if (spaceJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = spaceService.joinPrivateSpace(spaceJoinRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 退出社区
     */
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitSpace(@RequestBody SpaceQuitRequest spaceQuitRequest, HttpServletRequest request) {
        if (spaceQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = spaceService.quitSpace(spaceQuitRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 填充社区列表的 hasJoin / hasJoinNum
     */
    private void fillJoinInfo(List<SpaceVO> spaceVOList, User loginUser) {
        if (spaceVOList == null || spaceVOList.isEmpty()) {
            return;
        }
        List<Long> spaceIdList = spaceVOList.stream().map(SpaceVO::getId).collect(Collectors.toList());
        // 判断当前用户是否已加入
        QueryWrapper<SpaceUser> joinWrapper = new QueryWrapper<>();
        try {
            joinWrapper.eq("userId", loginUser.getId());
            if (CollectionUtils.isNotEmpty(spaceIdList)) {
                joinWrapper.in("spaceId", spaceIdList);
            }
            List<SpaceUser> myJoinList = spaceUserService.list(joinWrapper);
            Set<Long> hasJoinSpaceIdSet = myJoinList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
            spaceVOList.forEach(vo -> vo.setHasJoin(hasJoinSpaceIdSet.contains(vo.getId())));
        } catch (Exception e) {
            log.error("fillJoinInfo hasJoin error", e);
        }
        // 查询每个社区的加入人数
        QueryWrapper<SpaceUser> countWrapper = new QueryWrapper<>();
        if (CollectionUtils.isNotEmpty(spaceIdList)) {
            countWrapper.in("spaceId", spaceIdList);
        }
        List<SpaceUser> joinUserList = spaceUserService.list(countWrapper);
        Map<Long, Long> countMap = joinUserList.stream()
                .collect(Collectors.groupingBy(SpaceUser::getSpaceId, Collectors.counting()));
        spaceVOList.forEach(vo -> vo.setHasJoinNum(countMap.getOrDefault(vo.getId(), 0L).intValue()));
    }
}
