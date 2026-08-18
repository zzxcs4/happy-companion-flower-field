package com.zzx.yunpicturebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.common.ResultUtils;
import com.zzx.yunpicturebackend.model.entity.Field;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.entity.UserField;
import com.zzx.yunpicturebackend.model.vo.FieldUserVO;
import com.zzx.yunpicturebackend.model.vo.UserVO;
import com.zzx.yunpicturebackend.service.PostService;
import com.zzx.yunpicturebackend.service.RecommendService;
import com.zzx.yunpicturebackend.service.UserFieldService;
import com.zzx.yunpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/recommend")
public class RecommendController {

    @Resource
    private RecommendService recommendService;
    @Resource
    private UserService userService;
    @Resource
    private UserFieldService userFieldService;
    @Resource
    private PostService postService;

    /**
     * 为当前登录用户推荐花田（用户基协同过滤）
     *
     * @param topN 推荐数量，默认 10
     */
    @GetMapping("/fields")
    public BaseResponse<List<FieldUserVO>> recommendFields(
            @RequestParam(value = "topN", defaultValue = "10") int topN,
            HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        List<Field> fields = recommendService.recommendFields(loginUser.getId(), topN);

        // 转换为 FieldUserVO 并填充数据
        List<FieldUserVO> fieldUserVOList = new ArrayList<>();
        if (fields != null && !fields.isEmpty()) {
            // 过滤掉私有花田（status=1），只保留公开和加密花田
            List<Field> filteredFields = fields.stream()
                    .filter(field -> field.getStatus() == null || field.getStatus() != 1)
                    .collect(Collectors.toList());

            if (!filteredFields.isEmpty()) {
                List<Long> fieldIdList = filteredFields.stream().map(Field::getId).collect(Collectors.toList());

                for (Field field : filteredFields) {
                    FieldUserVO fieldUserVO = new FieldUserVO();
                    BeanUtils.copyProperties(field, fieldUserVO);

                    // 设置帖子数
                    int postNum = postService.query().eq("fieldId", field.getId()).count().intValue();
                    fieldUserVO.setPostNum(postNum);

                    // 设置创建人信息
                    User createUser = userService.getById(field.getUserId());
                    if (createUser != null) {
                        UserVO userVO = new UserVO();
                        BeanUtils.copyProperties(createUser, userVO);
                        fieldUserVO.setCreateUser(userVO);
                    }

                    fieldUserVOList.add(fieldUserVO);
                }

                // 查询当前用户已加入的花田
                QueryWrapper<UserField> userFieldQueryWrapper = new QueryWrapper<>();
                userFieldQueryWrapper.eq("userId", loginUser.getId());
                userFieldQueryWrapper.in("fieldId", fieldIdList);
                List<UserField> userFieldList = userFieldService.list(userFieldQueryWrapper);
                Set<Long> hasJoinFieldIdSet = userFieldList.stream()
                        .map(UserField::getFieldId)
                        .collect(Collectors.toSet());

                // 设置是否已加入标记
                fieldUserVOList.forEach(fieldUserVO -> {
                    boolean hasJoin = hasJoinFieldIdSet.contains(fieldUserVO.getId());
                    fieldUserVO.setHasJoin(hasJoin);
                });

                // 查询已加入的人数
                QueryWrapper<UserField> joinCountQueryWrapper = new QueryWrapper<>();
                joinCountQueryWrapper.in("fieldId", fieldIdList);
                List<UserField> allUserFieldList = userFieldService.list(joinCountQueryWrapper);
                Map<Long, List<UserField>> fieldUserMap = allUserFieldList.stream()
                        .collect(Collectors.groupingBy(UserField::getFieldId));

                fieldUserVOList.forEach(fieldUserVO -> {
                    int hasJoinNum = fieldUserMap.getOrDefault(fieldUserVO.getId(), new ArrayList<>()).size();
                    fieldUserVO.setHasJoinNum(hasJoinNum);
                });
            }
        }

        return ResultUtils.success(fieldUserVOList);
    }
}

