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
import com.zzx.yunpicturebackend.model.dto.space.SpaceJoinRequest;
import com.zzx.yunpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zzx.yunpicturebackend.model.dto.space.SpaceQuitRequest;
import com.zzx.yunpicturebackend.model.entity.Category;
import com.zzx.yunpicturebackend.model.entity.Post;
import com.zzx.yunpicturebackend.model.entity.Space;
import com.zzx.yunpicturebackend.model.entity.SpaceUser;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.enums.SpaceLevelEnum;
import com.zzx.yunpicturebackend.model.enums.SpaceRoleEnum;
import com.zzx.yunpicturebackend.model.enums.SpaceTypeEnum;
import com.zzx.yunpicturebackend.model.vo.SpaceVO;
import com.zzx.yunpicturebackend.model.vo.UserVO;
import com.zzx.yunpicturebackend.service.SpaceService;
import com.zzx.yunpicturebackend.mapper.PostMapper;
import com.zzx.yunpicturebackend.mapper.SpaceMapper;
import com.zzx.yunpicturebackend.service.SpaceUserService;
import com.zzx.yunpicturebackend.service.CategoryService;
import com.zzx.yunpicturebackend.service.UserFieldScoreService;
import com.zzx.yunpicturebackend.service.UserService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

    @Resource
    private PostMapper postMapper;

    @Resource
    @Lazy
    private UserFieldScoreService userFieldScoreService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private RedissonClient redissonClient;

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
        if(space.getSpaceType()==2){
            // 社区空间默认公开
            if (space.getStatus() == null) {
                space.setStatus(0);
            }
            // 社区空间不限容量
            space.setMaxSize(0L);
            space.setMaxCount(0L);
        }
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
        List<Long> idList = spaceQueryRequest.getIdList();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Long userId = spaceQueryRequest.getUserId();
        int pageSize = spaceQueryRequest.getPageSize();
        String sortOrder = spaceQueryRequest.getSortOrder();
        String sortField = spaceQueryRequest.getSortField();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        Integer status = spaceQueryRequest.getStatus();
        Long categoryId = spaceQueryRequest.getCategoryId();
        String searchText = spaceQueryRequest.getSearchText();
        queryWrapper.in(CollUtil.isNotEmpty(idList), "id", idList);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(status), "status", status);
        queryWrapper.eq(ObjUtil.isNotEmpty(categoryId), "categoryId", categoryId);
        // 对 searchText 的多个条件用 or() 分组
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw
                    .like("spaceName", searchText)
                    .or().like("description", searchText)
            );
        }

        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        if(space==null){
            return null;
        }
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        // 关联查询分类名
        Long categoryId = space.getCategoryId();
        if (categoryId != null && categoryId > 0) {
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                spaceVO.setCategoryName(category.getCategory());
            }
        }
        // 计算帖子数量
        if (space.getSpaceType() != null && space.getSpaceType() == 2) {
            Long count = postMapper.selectCount(new QueryWrapper<Post>().eq("spaceId", space.getId()));
            spaceVO.setPostNum(count == null ? 0 : count.intValue());
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
            // 社区空间（花田）校验
            if (spaceTypeEnum == SpaceTypeEnum.FIELD) {
                if (space.getStatus() == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "社区状态不能为空");
                }
                if (space.getStatus() == 2 && StrUtil.isBlank(space.getPassword())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密社区必须设置密码");
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
        // 修改社区加密状态时密码必填
        if (space.getStatus() != null && space.getStatus() == 2 && spaceTypeEnum == SpaceTypeEnum.FIELD) {
            if (StrUtil.isBlank(space.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密社区必须设置密码");
            }
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

    @Override
    public boolean joinSpace(SpaceJoinRequest spaceJoinRequest, User loginUser) {
        if (spaceJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long spaceId = spaceJoinRequest.getSpaceId();
        Space space = getCommunitySpaceById(spaceId);
        Integer status = space.getStatus();
        // 私有禁止加入
        if (status != null && status == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有社区");
        }
        // 加密需要密码
        String password = spaceJoinRequest.getPassword();
        if (status != null && status == 2) {
            if (StrUtil.isBlank(password) || !password.equals(space.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        return doJoinSpace(space, loginUser);
    }

    @Override
    public boolean joinPrivateSpace(SpaceJoinRequest spaceJoinRequest, User loginUser) {
        if (spaceJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long spaceId = spaceJoinRequest.getSpaceId();
        Space space = getCommunitySpaceById(spaceId);
        return doJoinSpace(space, loginUser);
    }

    /**
     * 加入社区空间的公共逻辑
     */
    private boolean doJoinSpace(Space space, User loginUser) {
        long userId = loginUser.getId();
        Long spaceId = space.getId();
        // 只有一个线程能获取到锁
        RLock lock = redissonClient.getLock("happy:join_space");
        try {
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    // 已加入的社区数量限制
                    QueryWrapper<SpaceUser> countWrapper = new QueryWrapper<>();
                    countWrapper.eq("userId", userId);
                    long hasJoinNum = spaceUserService.count(countWrapper);
                    if (hasJoinNum > 1000) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入 1000 个社区");
                    }
                    // 不能重复加入
                    countWrapper = new QueryWrapper<>();
                    countWrapper.eq("userId", userId);
                    countWrapper.eq("spaceId", spaceId);
                    long hasUserJoinSpace = spaceUserService.count(countWrapper);
                    if (hasUserJoinSpace > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该社区");
                    }
                    // 已加入的人数上限
                    long spaceHasJoinNum = countSpaceUserBySpaceId(spaceId);
                    if (space.getMaxNum() != null && spaceHasJoinNum >= space.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "社区已满");
                    }
                    // 写入空间用户关联（浏览者）
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceId(spaceId);
                    //若是公开空间
                    if(space.getStatus()==0){
                        spaceUser.setSpaceRole(SpaceRoleEnum.MEMBER.getValue());
                    }else {
                        spaceUser.setSpaceRole(SpaceRoleEnum.VIEWER.getValue());
                    }
                    boolean saved = spaceUserService.save(spaceUser);
                    // 加入社区 +1 分
                    if (saved) {
                        userFieldScoreService.addScore(userId, spaceId, 1.0);
                    }
                    return saved;
                }
            }
        } catch (InterruptedException e) {
            log.error("joinSpace error", e);
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public boolean quitSpace(SpaceQuitRequest spaceQuitRequest, User loginUser) {
        if (spaceQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long spaceId = spaceQuitRequest.getSpaceId();
        Space space = getCommunitySpaceById(spaceId);
        long userId = loginUser.getId();
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("spaceId", spaceId);
        long count = spaceUserService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入社区");
        }
        long spaceHasJoinNum = countSpaceUserBySpaceId(spaceId);
        // 社区只剩一人，解散
        if (spaceHasJoinNum == 1) {
            this.removeById(spaceId);
        } else if (Objects.equals(space.getUserId(), userId)) {
            // 是创建人（队长），把社区转移给最早加入的用户
            QueryWrapper<SpaceUser> userFieldQueryWrapper = new QueryWrapper<>();
            userFieldQueryWrapper.eq("spaceId", spaceId);
            userFieldQueryWrapper.last("order by id asc limit 2");
            List<SpaceUser> spaceUserList = spaceUserService.list(userFieldQueryWrapper);
            if (CollUtil.isEmpty(spaceUserList) || spaceUserList.size() <= 1) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            SpaceUser nextUser = spaceUserList.get(1);
            long nextLeaderId = nextUser.getUserId();
            Space updateSpace = new Space();
            updateSpace.setId(spaceId);
            updateSpace.setUserId(nextLeaderId);
            boolean updated = this.updateById(updateSpace);
            if (!updated) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新社区创建人失败");
            }
        }
        // 移除成员关系
        boolean result = spaceUserService.remove(queryWrapper);
        // 退出社区 -3.0 分
        if (result) {
            userFieldScoreService.addScore(userId, spaceId, -3.0);
        }
        return result;
    }

    @Override
    public List<SpaceVO> listCommunitySpaces(SpaceQueryRequest spaceQueryRequest, boolean isAdmin) {
        if (spaceQueryRequest == null) {
            return new ArrayList<>();
        }
        QueryWrapper<Space> queryWrapper = getQueryWrapper(spaceQueryRequest);
        // 只查询社区空间
        queryWrapper.eq("spaceType", SpaceTypeEnum.FIELD.getValue());
        // 非管理员只能看公开社区的翻看（私有/加密社区在 status 过滤中处理）
        List<Space> spaceList = this.list(queryWrapper);
        List<SpaceVO> spaceVOList = new ArrayList<>();
        for (Space space : spaceList) {
            Long userId = space.getUserId();
            if (userId == null) {
                continue;
            }
            SpaceVO spaceVO = this.getSpaceVO(space, null);
            spaceVOList.add(spaceVO);
        }
        return spaceVOList;
    }

    @Override
    public List<User> getSpaceMemberList(Long spaceId, String searchText) {
        if (spaceId == null || spaceId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spaceId", spaceId);
        List<SpaceUser> spaceUserList = spaceUserService.list(queryWrapper);
        List<Long> userIdList = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toList());
        if (userIdList.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<User> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.in("id", userIdList);
        if (searchText != null && !searchText.isEmpty()) {
            queryWrapper1.and(qw -> qw
                    .like("userName", searchText)
                    .or().like("userAccount", searchText)
                    .or().eq("id", searchText)
            );
        }
        List<User> userList = userService.list(queryWrapper1);
        if (userList == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return userList;
    }

    /**
     * 获取社区空间（花田）实体
     */
    private Space getCommunitySpaceById(Long spaceId) {
        if (spaceId == null || spaceId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Space space = this.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "社区不存在");
        }
        return space;
    }

    private long countSpaceUserBySpaceId(long spaceId) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spaceId", spaceId);
        return spaceUserService.count(queryWrapper);
    }


}




