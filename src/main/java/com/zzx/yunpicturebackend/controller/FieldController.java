package com.zzx.yunpicturebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.common.DeleteRequest;
import com.zzx.yunpicturebackend.common.ResultUtils;
import com.zzx.yunpicturebackend.exception.BusinessException;
import com.zzx.yunpicturebackend.exception.ErrorCode;
import com.zzx.yunpicturebackend.exception.ThrowUtils;
import com.zzx.yunpicturebackend.model.dto.field.FieldAddRequest;
import com.zzx.yunpicturebackend.model.dto.field.FieldJoinRequest;
import com.zzx.yunpicturebackend.model.dto.field.FieldQueryRequest;
import com.zzx.yunpicturebackend.model.dto.field.FieldQuitRequest;
import com.zzx.yunpicturebackend.model.dto.userfield.DeleteFURequest;
import com.zzx.yunpicturebackend.model.entity.Field;
import com.zzx.yunpicturebackend.model.entity.UserField;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.vo.FieldCategoryVO;
import com.zzx.yunpicturebackend.model.vo.FieldUserVO;
import com.zzx.yunpicturebackend.service.FieldService;
import com.zzx.yunpicturebackend.service.PostService;
import com.zzx.yunpicturebackend.service.UserFieldService;
import com.zzx.yunpicturebackend.service.UserService;
import com.zzx.yunpicturebackend.utils.ListToPageConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 花田接口（已并入空间接口，废弃停用，请使用 SpaceController）
 * @Deprecated 社区(花田)概念已融入空间，原 /field/* 接口不再暴露
 */
@Deprecated
//@RestController
//@Slf4j
//@RequestMapping("/field")
public class FieldController {

    @Resource
    private UserService userService;
    @Resource
    private FieldService fieldService;
    @Resource
    private UserFieldService UserFieldService;
    @Resource
    private PostService postService;
    /**
     * 新建花田
     * @param fieldAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addField(FieldAddRequest fieldAddRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        long addField = fieldService.addField(fieldAddRequest, loginUser);
        return ResultUtils.success(addField);
    }
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteField(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        if (deleteRequest == null || deleteRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        boolean result = fieldService.deleteField(id, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/category")
    public BaseResponse<FieldCategoryVO> listFieldCategory() {
        FieldCategoryVO fieldCategoryVO = new FieldCategoryVO();
        HashMap<String,List<String>> categoryHashMap = new HashMap<>();
        categoryHashMap.put("程序员", Arrays.asList("后端开发","前端开发","运维", "测试"));
        categoryHashMap.put("动漫宅", Arrays.asList( "动漫","轻小说","漫画","声优"));
        categoryHashMap.put("游戏", Arrays.asList("热门网游", "3A单机", "二游","手游","主机游戏"));
        categoryHashMap.put("生活", Arrays.asList("美食", "旅行", "摄影","变美"));
        categoryHashMap.put("娱乐",Arrays.asList("娱乐明星","导演","CP","网络红人"));
        fieldCategoryVO.setCategoryHashMap(categoryHashMap);
        return ResultUtils.success(fieldCategoryVO);
    }
    @PostMapping("/update")
    public BaseResponse<Boolean> updateField(@RequestBody Field Field,HttpServletRequest request){
        if (Field == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = fieldService.updateField(Field,loginUser);
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<Field> getFieldById(Long id){
        if (id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Field Field = fieldService.getById(id);
        Field.setPostNum( postService.query().eq("fieldId",Field.getId()).count().intValue());
        if (Field == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(Field);
    }
    @GetMapping("/getMemberList")
    public BaseResponse<List<User>> getFieldMemberListById(Long id, @RequestParam(required = false) String searchText){
        return fieldService.getFieldMemberListById(id,searchText);


    }
    @GetMapping("/list")
    public BaseResponse< List<FieldUserVO>> listFields(FieldQueryRequest fieldQueryRequest, HttpServletRequest request) {
        if (fieldQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        List<FieldUserVO> fieldUserVOList = fieldService.listFields(fieldQueryRequest, userService.isAdmin(loginUser));

         List<Long> FieldIdList = fieldUserVOList.stream().map(FieldUserVO::getId).collect(Collectors.toList());
        // 2、判断当前用户是否已加入队伍
        QueryWrapper<UserField> userFieldQueryWrapper = new QueryWrapper<>();
        try {
            userFieldQueryWrapper.eq("userId", loginUser.getId());
            if (FieldIdList != null && !FieldIdList.isEmpty()) {
                userFieldQueryWrapper.in("fieldId", FieldIdList);
            }

            List<UserField> userFieldList = UserFieldService.list(userFieldQueryWrapper);
            // 已加入的队伍 id 集合
            Set<Long> hasJoinFieldIdSet = userFieldList.stream().map(UserField::getFieldId).collect(Collectors.toSet());
            fieldUserVOList.forEach(Field -> {
                boolean hasJoin = hasJoinFieldIdSet.contains(Field.getId());
                Field.setHasJoin(hasJoin);
            });
        } catch (Exception e) {}
        //3.查询已加入的队伍的人数
        QueryWrapper<UserField> userFieldJoinQueryWrapper = new QueryWrapper<>();
        if (FieldIdList != null && !FieldIdList.isEmpty()) {
            userFieldJoinQueryWrapper.in("fieldId", FieldIdList);
        }
        List<UserField> userFieldList = UserFieldService.list(userFieldJoinQueryWrapper);
        //队伍id=>加入这个队伍的用户列表
        Map<Long, List<UserField>> FieldIdUserFieldList = userFieldList.stream().collect(Collectors.groupingBy(UserField::getFieldId));
        fieldUserVOList.forEach(Field -> Field.setHasJoinNum(FieldIdUserFieldList.getOrDefault(Field.getId(), new ArrayList<>()).size()));
        // 分页参数：第1页（page=0），每页3条
//        Pageable pageable = PageRequest.of(fieldQueryRequest.getCurrent(),fieldQueryRequest.getPageSize());
//        org.springframework.data.domain.Page<FieldUserVO> fieldUserVOS = ListToPageConverter.convertListToPage(fieldUserVOList, pageable);
        return ResultUtils.success(fieldUserVOList);

    }
    @GetMapping("/list/my/create")
    public BaseResponse<List<FieldUserVO>> listMyCreateFields(FieldQueryRequest fieldQueryRequest,HttpServletRequest request) {
        if (fieldQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        fieldQueryRequest.setUserId(loginUser.getId());

        List<FieldUserVO> FieldList = fieldService.listFields(fieldQueryRequest,true);
        List<Long> FieldIdList = FieldList.stream().map(FieldUserVO::getId).collect(Collectors.toList());
        // 2、判断当前用户是否已加入队伍
        QueryWrapper<UserField> userFieldQueryWrapper = new QueryWrapper<>();
        try {
            userFieldQueryWrapper.eq("userId", loginUser.getId());
            if (FieldIdList != null && !FieldIdList.isEmpty()) {
                userFieldQueryWrapper.in("fieldId", FieldIdList);
            }

            List<UserField> userFieldList = UserFieldService.list(userFieldQueryWrapper);
            // 已加入的队伍 id 集合
            Set<Long> hasJoinFieldIdSet = userFieldList.stream().map(UserField::getFieldId).collect(Collectors.toSet());
            FieldList.forEach(Field -> {
                boolean hasJoin = hasJoinFieldIdSet.contains(Field.getId());
                Field.setHasJoin(hasJoin);
            });
        } catch (Exception e) {}
        //3.查询已加入的队伍的人数
        QueryWrapper<UserField> userFieldJoinQueryWrapper = new QueryWrapper<>();
        if (FieldIdList != null && !FieldIdList.isEmpty()) {
            userFieldJoinQueryWrapper.in("fieldId", FieldIdList);
        }
        List<UserField> userFieldList = UserFieldService.list(userFieldJoinQueryWrapper);
        //队伍id=>加入这个队伍的用户列表
        Map<Long, List<UserField>> FieldIdUserFieldList = userFieldList.stream().collect(Collectors.groupingBy(UserField::getFieldId));
        FieldList.forEach(Field -> Field.setHasJoinNum(FieldIdUserFieldList.getOrDefault(Field.getId(), new ArrayList<>()).size()));
        // 分页参数：第1页（page=0），每页3条
//        Pageable pageable = PageRequest.of(fieldQueryRequest.getCurrent(),fieldQueryRequest.getPageSize());
//        org.springframework.data.domain.Page<FieldUserVO> fieldListVO = ListToPageConverter.convertListToPage(FieldList, pageable);
        return ResultUtils.success(FieldList);
    }
    @GetMapping("/list/my/join")
    public BaseResponse<List<FieldUserVO> > listMyJoinFields(FieldQueryRequest fieldQueryRequest,HttpServletRequest request) {
        if (fieldQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserField> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("userId",loginUser.getId());
        List<UserField> userFieldList = UserFieldService.list(queryWrapper);
        Map<Long, List<UserField>> listMap = userFieldList.stream()
                .collect(Collectors.groupingBy(UserField::getFieldId));
        ArrayList<Long> idList = new ArrayList<>(listMap.keySet());
        fieldQueryRequest.setIdList(idList);
        List<FieldUserVO> FieldList = fieldService.listFields(fieldQueryRequest,true);
        if(CollectionUtils.isNotEmpty(FieldList)&&CollectionUtils.isEmpty(idList)){
            return ResultUtils.success(new ArrayList<>());
        }
        List<Long> FieldIdList = FieldList.stream().map(FieldUserVO::getId).collect(Collectors.toList());
        // 2、判断当前用户是否已加入队伍
        try {
            if (FieldIdList != null && !FieldIdList.isEmpty()) {
                queryWrapper.in("fieldId", FieldIdList);
            }

            userFieldList = UserFieldService.list(queryWrapper);
            // 已加入的队伍 id 集合
            Set<Long> hasJoinFieldIdSet = userFieldList.stream().map(UserField::getFieldId).collect(Collectors.toSet());
            FieldList.forEach(Field -> {
                boolean hasJoin = hasJoinFieldIdSet.contains(Field.getId());
                Field.setHasJoin(hasJoin);
            });
        } catch (Exception e) {}
        //3.查询已加入的队伍的人数
        QueryWrapper<UserField> userFieldJoinQueryWrapper = new QueryWrapper<>();
        if (FieldIdList != null && !FieldIdList.isEmpty()) {
            userFieldJoinQueryWrapper.in("fieldId", FieldIdList);
        }
        List<UserField> userFieldJoinList = UserFieldService.list(userFieldJoinQueryWrapper);
        //队伍id=>加入这个队伍的用户列表
        Map<Long, List<UserField>> FieldIdUserFieldList = userFieldJoinList.stream().collect(Collectors.groupingBy(UserField::getFieldId));
        FieldList.forEach(Field -> Field.setHasJoinNum(FieldIdUserFieldList.getOrDefault(Field.getId(), new ArrayList<>()).size()));
        // 分页参数：第1页（page=0），每页3条
//        Pageable pageable = PageRequest.of(fieldQueryRequest.getCurrent(),fieldQueryRequest.getPageSize());
//        org.springframework.data.domain.Page<FieldUserVO> fieldListVO = ListToPageConverter.convertListToPage(FieldList, pageable);
        return ResultUtils.success(FieldList);
    }

    @GetMapping("/list/page")
    public BaseResponse<Page<Field>> listPageFields(FieldQueryRequest FieldQuery) {
        if (FieldQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Field Field = new Field();
        BeanUtils.copyProperties(FieldQuery, Field);
        Page<Field> page = new Page<>(FieldQuery.getCurrent(),FieldQuery.getPageSize());
        QueryWrapper<Field> queryWrapper = new QueryWrapper<>(Field);
        Page<Field> resultPage = fieldService.page(page,queryWrapper);
        return ResultUtils.success(resultPage);
    }
    @PostMapping("/join")
    public BaseResponse<Boolean> joinField(@RequestBody FieldJoinRequest FieldJoinRequest, HttpServletRequest request){
        if (FieldJoinRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = fieldService.joinField(FieldJoinRequest,loginUser);

        return ResultUtils.success(result);
    }
    @PostMapping("/join/private")
    public BaseResponse<Boolean> joinPrivateField(@RequestBody FieldJoinRequest FieldJoinRequest, HttpServletRequest request){
        if (FieldJoinRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = fieldService.joinPrivateField(FieldJoinRequest,loginUser);

        return ResultUtils.success(result);
    }
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitField(@RequestBody FieldQuitRequest FieldQuitRequest, HttpServletRequest request){
        if (FieldQuitRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = fieldService.quitField(FieldQuitRequest,loginUser);

        return ResultUtils.success(result);
    }
    /**
     * 从空间移除成员
     */
    @PostMapping("/deleteFieldUser")
    //@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteFieldUser(@RequestBody DeleteFURequest deleteRequest,
                                                 HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getFieldId()==null||deleteRequest.getFieldId()==0||deleteRequest.getUserId()==null||deleteRequest.getUserId()==0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long fieldId = deleteRequest.getFieldId();
        Long userId = deleteRequest.getUserId();
        QueryWrapper<UserField> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",userId);
        queryWrapper.eq("fieldId",fieldId);
        // 判断是否存在
        UserField oldUserField = UserFieldService.getOne(queryWrapper);
        ThrowUtils.throwIf(oldUserField == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = UserFieldService.removeById(oldUserField);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
}
