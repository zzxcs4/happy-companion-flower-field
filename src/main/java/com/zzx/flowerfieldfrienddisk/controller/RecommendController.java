package com.zzx.flowerfieldfrienddisk.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.zzx.flowerfieldfrienddisk.common.BaseResponse;
import com.zzx.flowerfieldfrienddisk.common.ResultUtils;
import com.zzx.flowerfieldfrienddisk.model.entity.Space;
import com.zzx.flowerfieldfrienddisk.model.entity.SpaceUser;
import com.zzx.flowerfieldfrienddisk.model.entity.User;
import com.zzx.flowerfieldfrienddisk.model.vo.SpaceVO;
import com.zzx.flowerfieldfrienddisk.service.PostService;
import com.zzx.flowerfieldfrienddisk.service.RecommendService;
import com.zzx.flowerfieldfrienddisk.service.SpaceUserService;
import com.zzx.flowerfieldfrienddisk.service.UserService;
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
    private SpaceUserService spaceUserService;
    @Resource
    private PostService postService;

    /**
     * 为当前登录用户推荐社区（花田）（用户基协同过滤）
     *
     * @param topN 推荐数量，默认 10
     */
    @GetMapping("/fields")
    public BaseResponse<List<SpaceVO>> recommendFields(
            @RequestParam(value = "topN", defaultValue = "10") int topN,
            HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        List<Space> spaces = recommendService.recommendFields(loginUser.getId(), topN);

        List<SpaceVO> spaceVOList = new ArrayList<>();
        if (spaces != null && !spaces.isEmpty()) {
            // 过滤掉私有社区（status=1），只保留公开和加密
            List<Space> filteredSpaces = spaces.stream()
                    .filter(space -> space.getStatus() == null || space.getStatus() != 1)
                    .collect(Collectors.toList());

            if (!filteredSpaces.isEmpty()) {
                List<Long> spaceIdList = filteredSpaces.stream().map(Space::getId).collect(Collectors.toList());

                for (Space space : filteredSpaces) {
                    SpaceVO spaceVO = SpaceVO.objToVo(space);
                    int postNum = postService.query().eq("spaceId", space.getId()).count().intValue();
                    spaceVO.setPostNum(postNum);

                    User createUser = userService.getById(space.getUserId());
                    if (createUser != null) {
                        spaceVO.setUser(userService.getUserVO(createUser));
                    }
                    spaceVOList.add(spaceVO);
                }

                // 查询当前用户已加入的社区
                QueryWrapper<SpaceUser> joinWrapper = new QueryWrapper<>();
                joinWrapper.eq("userId", loginUser.getId());
                if (CollectionUtils.isNotEmpty(spaceIdList)) {
                    joinWrapper.in("spaceId", spaceIdList);
                }
                List<SpaceUser> myJoinList = spaceUserService.list(joinWrapper);
                Set<Long> hasJoinSpaceIdSet = myJoinList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
                spaceVOList.forEach(vo -> vo.setHasJoin(hasJoinSpaceIdSet.contains(vo.getId())));

                // 查询每个社区已加入的人数
                QueryWrapper<SpaceUser> joinCountWrapper = new QueryWrapper<>();
                if (CollectionUtils.isNotEmpty(spaceIdList)) {
                    joinCountWrapper.in("spaceId", spaceIdList);
                }
                List<SpaceUser> allJoinList = spaceUserService.list(joinCountWrapper);
                Map<Long, Long> countMap = allJoinList.stream()
                        .collect(Collectors.groupingBy(SpaceUser::getSpaceId, Collectors.counting()));
                spaceVOList.forEach(vo -> vo.setHasJoinNum(countMap.getOrDefault(vo.getId(), 0L).intValue()));
            }
        }

        return ResultUtils.success(spaceVOList);
    }
}