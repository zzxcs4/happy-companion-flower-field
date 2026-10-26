package com.zzx.flowerfieldfrienddisk.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.flowerfieldfrienddisk.exception.BusinessException;
import com.zzx.flowerfieldfrienddisk.exception.ErrorCode;
import com.zzx.flowerfieldfrienddisk.exception.ThrowUtils;
import com.zzx.flowerfieldfrienddisk.manager.auth.SpaceUserAuthManager;
import com.zzx.flowerfieldfrienddisk.manager.auth.SpaceUserPermissionConstant;
import com.zzx.flowerfieldfrienddisk.mapper.FileCommentMapper;
import com.zzx.flowerfieldfrienddisk.model.entity.File;
import com.zzx.flowerfieldfrienddisk.model.entity.FileComment;
import com.zzx.flowerfieldfrienddisk.model.entity.Space;
import com.zzx.flowerfieldfrienddisk.model.entity.SpaceUser;
import com.zzx.flowerfieldfrienddisk.model.entity.User;
import com.zzx.flowerfieldfrienddisk.model.enums.SpaceTypeEnum;
import com.zzx.flowerfieldfrienddisk.model.vo.UserVO;
import com.zzx.flowerfieldfrienddisk.service.FileCommentService;
import com.zzx.flowerfieldfrienddisk.service.FileMentionService;
import com.zzx.flowerfieldfrienddisk.service.FileService;
import com.zzx.flowerfieldfrienddisk.service.IFollowService;
import com.zzx.flowerfieldfrienddisk.service.SpaceService;
import com.zzx.flowerfieldfrienddisk.service.SpaceUserService;
import com.zzx.flowerfieldfrienddisk.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件评论服务实现
 * 评论范围：团队空间、社区空间、公共空间（spaceId 为 null）可评论，私有空间不支持
 */
@Slf4j
@Service
public class FileCommentServiceImpl extends ServiceImpl<FileCommentMapper, FileComment>
        implements FileCommentService {

    @Resource
    private FileCommentMapper fileCommentMapper;
    @Resource
    private FileService fileService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private UserService userService;
    @Resource
    private IFollowService followService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    @Resource
    private FileMentionService fileMentionService;

    /**
     * 校验文件是否允许评论/查看评论
     * 公共空间（spaceId 为空）：任何登录用户；团队/社区空间：需具备查看权限；私有空间：拒绝
     */
    private void checkCommentable(File file, User loginUser) {
        Long spaceId = file.getSpaceId();
        if (spaceId == null) {
            // 公共空间：任何登录用户均可评论
            return;
        }
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "私有空间不支持评论");
        }
        List<String> permissionList = spaceUserAuthManager.getPermissionList(file, space, loginUser);
        ThrowUtils.throwIf(!permissionList.contains(SpaceUserPermissionConstant.PICTURE_VIEW),
                ErrorCode.NO_AUTH_ERROR, "无权限评论该文件");
    }

    @Override
    public boolean addComment(Long fileId, String content, List<Long> atUserIds, User loginUser) {
        ThrowUtils.throwIf(fileId == null, ErrorCode.PARAMS_ERROR, "文件 id 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.PARAMS_ERROR, "评论内容不能为空");
        ThrowUtils.throwIf(content.length() > 1000, ErrorCode.PARAMS_ERROR, "评论内容过长（最多 1000 字）");

        File file = fileService.getById(fileId);
        ThrowUtils.throwIf(file == null, ErrorCode.NOT_FOUND_ERROR, "文件不存在");
        checkCommentable(file, loginUser);

        FileComment fileComment = new FileComment();
        fileComment.setFileId(fileId);
        fileComment.setContent(content);
        fileComment.setUserId(loginUser.getId());
        if (CollUtil.isNotEmpty(atUserIds)) {
            fileComment.setAtUserIds(JSONUtil.toJsonStr(atUserIds));
        }
        fileComment.setCreateTime(new Date());
        fileComment.setUpdateTime(new Date());
        boolean result = this.save(fileComment);
        // 写入 @ 提及记录（仅团队/社区空间，供「@我的」红点与列表使用）
        if (result) {
            try {
                fileMentionService.saveMentions(fileComment, file, atUserIds, loginUser);
            } catch (Exception e) {
                // 提及记录写入失败不影响评论发布
                log.error("写入 @ 提及记录失败, commentId={}", fileComment.getId(), e);
            }
        }
        return result;
    }

    @Override
    public List<FileComment> listComments(Long fileId, User loginUser) {
        ThrowUtils.throwIf(fileId == null, ErrorCode.PARAMS_ERROR, "文件 id 不能为空");
        File file = fileService.getById(fileId);
        ThrowUtils.throwIf(file == null, ErrorCode.NOT_FOUND_ERROR, "文件不存在");
        checkCommentable(file, loginUser);
        return fileCommentMapper.listCommentsWithUser(fileId);
    }

    @Override
    public boolean deleteComment(Long commentId, User loginUser) {
        ThrowUtils.throwIf(commentId == null, ErrorCode.PARAMS_ERROR, "评论 id 不能为空");
        FileComment fileComment = this.getById(commentId);
        ThrowUtils.throwIf(fileComment == null, ErrorCode.NOT_FOUND_ERROR, "评论不存在");
        // 仅评论本人或管理员可删除
        boolean canDelete = loginUser.getId().equals(fileComment.getUserId()) || userService.isAdmin(loginUser);
        ThrowUtils.throwIf(!canDelete, ErrorCode.NO_AUTH_ERROR, "无权限删除该评论");
        return this.removeById(commentId);
    }

    @Override
    public List<UserVO> listMentionableUsers(Long fileId, User loginUser) {
        ThrowUtils.throwIf(fileId == null, ErrorCode.PARAMS_ERROR, "文件 id 不能为空");
        File file = fileService.getById(fileId);
        ThrowUtils.throwIf(file == null, ErrorCode.NOT_FOUND_ERROR, "文件不存在");

        Long spaceId = file.getSpaceId();
        if (spaceId == null) {
            // 公共空间：可 @ 关注的用户
            List<User> follows = followService.follows(loginUser.getId());
            return userService.getUserVOList(follows);
        }

        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "私有空间不支持评论");
        }

        // 团队/社区空间：可 @ 空间成员（含空间创建者）
        List<SpaceUser> spaceUsers = spaceUserService.lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .list();
        Set<Long> userIds = spaceUsers.stream()
                .map(SpaceUser::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        userIds.add(space.getUserId());
        List<User> users = userService.listByIds(new ArrayList<>(userIds));
        return userService.getUserVOList(users);
    }
}
