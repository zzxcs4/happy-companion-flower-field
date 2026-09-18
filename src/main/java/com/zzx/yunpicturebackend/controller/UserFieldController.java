package com.zzx.yunpicturebackend.controller;

import cn.hutool.core.util.ObjectUtil;
import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.common.DeleteRequest;
import com.zzx.yunpicturebackend.common.ResultUtils;
import com.zzx.yunpicturebackend.exception.BusinessException;
import com.zzx.yunpicturebackend.exception.ErrorCode;
import com.zzx.yunpicturebackend.exception.ThrowUtils;
import com.zzx.yunpicturebackend.manager.auth.SpaceUserPermissionConstant;
import com.zzx.yunpicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.zzx.yunpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.zzx.yunpicturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.zzx.yunpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.zzx.yunpicturebackend.model.entity.SpaceUser;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.entity.UserField;
import com.zzx.yunpicturebackend.model.vo.SpaceUserVO;
import com.zzx.yunpicturebackend.service.SpaceUserService;
import com.zzx.yunpicturebackend.service.UserFieldService;
import com.zzx.yunpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户社区关系接口（已并入 SpaceUserController，废弃停用）
 * @Deprecated 社区成员关系统一由 space_user 承载
 */
@Deprecated
//@RestController
//@RequestMapping("/UserField")
//@Slf4j
public class UserFieldController {

    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private UserFieldService userFieldService;

    @Resource
    private UserService userService;


    /**
     * 添加成员到空间
     */
//    @PostMapping("/add")
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
//    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
//        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
//        long id = spaceUserService.addSpaceUser(spaceUserAddRequest);
//        userFieldService.addUserField();
//        return ResultUtils.success(id);
//    }

    /**
     * 从空间移除成员
     */
    @PostMapping("/delete")
    //@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteFieldUser(@RequestBody DeleteRequest deleteRequest,
                                                 HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        UserField oldUserField = userFieldService.getById(id);
        ThrowUtils.throwIf(oldUserField == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = userFieldService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询某个成员在某个空间的信息
     */
//    @PostMapping("/get")
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
//    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
//        // 参数校验
//        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
//        Long spaceId = spaceUserQueryRequest.getSpaceId();
//        Long userId = spaceUserQueryRequest.getUserId();
//        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
//        // 查询数据库
//        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
//        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
//        return ResultUtils.success(spaceUser);
//    }

    /**
     * 查询成员信息列表
     */
//    @PostMapping("/list")
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
//    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest,
//                                                         HttpServletRequest request) {
//        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
//        List<SpaceUser> spaceUserList = spaceUserService.list(
//                spaceUserService.getQueryWrapper(spaceUserQueryRequest)
//        );
//        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
//    }

    /**
     * 编辑成员信息（设置权限）
     */
//    @PostMapping("/edit")
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
//    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest,
//                                               HttpServletRequest request) {
//        if (spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        // 将实体类和 DTO 进行转换
//        SpaceUser spaceUser = new SpaceUser();
//        BeanUtils.copyProperties(spaceUserEditRequest, spaceUser);
//        // 数据校验
//        spaceUserService.validSpaceUser(spaceUser, false);
//        // 判断是否存在
//        long id = spaceUserEditRequest.getId();
//        SpaceUser oldSpaceUser = spaceUserService.getById(id);
//        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
//        // 操作数据库
//        boolean result = spaceUserService.updateById(spaceUser);
//        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
//        return ResultUtils.success(true);
//    }

    /**
     * 查询我加入的团队空间列表
     */
//    @PostMapping("/list/my")
//    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
//        User loginUser = userService.getLoginUser(request);
//        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
//        spaceUserQueryRequest.setUserId(loginUser.getId());
//        List<SpaceUser> spaceUserList = spaceUserService.list(
//                spaceUserService.getQueryWrapper(spaceUserQueryRequest)
//        );
//        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
//    }
}

