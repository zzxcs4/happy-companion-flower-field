package com.zzx.yunpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.yunpicturebackend.exception.ErrorCode;
import com.zzx.yunpicturebackend.exception.ThrowUtils;
import com.zzx.yunpicturebackend.mapper.FileMentionMapper;
import com.zzx.yunpicturebackend.mapper.SpaceMapper;
import com.zzx.yunpicturebackend.model.entity.File;
import com.zzx.yunpicturebackend.model.entity.FileComment;
import com.zzx.yunpicturebackend.model.entity.FileMention;
import com.zzx.yunpicturebackend.model.entity.Space;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.enums.SpaceTypeEnum;
import com.zzx.yunpicturebackend.model.vo.FileMentionVO;
import com.zzx.yunpicturebackend.service.FileMentionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 文件评论 @ 提及服务实现
 * 「@我的」红点角标与提及列表的数据来源
 */
@Slf4j
@Service
public class FileMentionServiceImpl extends ServiceImpl<FileMentionMapper, FileMention>
        implements FileMentionService {

    @Resource
    private FileMentionMapper fileMentionMapper;
    @Resource
    private SpaceMapper spaceMapper;

    @Override
    public void saveMentions(FileComment fileComment, File file, List<Long> atUserIds, User loginUser) {
        if (fileComment == null || file == null || CollUtil.isEmpty(atUserIds)) {
            return;
        }
        Long spaceId = file.getSpaceId();
        if (spaceId == null) {
            // 公共空间文件无「@我的」入口，不产生提及记录
            return;
        }
        Space space = spaceMapper.selectById(spaceId);
        if (space == null) {
            return;
        }
        // 仅多人空间（团队/社区）产生提及
        boolean isMultiUserSpace = space.getSpaceType() == SpaceTypeEnum.TEAM.getValue()
                || space.getSpaceType() == SpaceTypeEnum.FIELD.getValue();
        if (!isMultiUserSpace) {
            return;
        }

        Date now = new Date();
        List<FileMention> mentions = new ArrayList<>();
        for (Long toUserId : atUserIds) {
            if (toUserId == null || toUserId.equals(loginUser.getId())) {
                // 排除 @ 自己
                continue;
            }
            FileMention mention = new FileMention();
            mention.setCommentId(fileComment.getId());
            mention.setFileId(file.getId());
            mention.setSpaceId(spaceId);
            mention.setFromUserId(loginUser.getId());
            mention.setToUserId(toUserId);
            mention.setIsRead(0);
            mention.setCreateTime(now);
            mention.setUpdateTime(now);
            mentions.add(mention);
        }
        if (!mentions.isEmpty()) {
            this.saveBatch(mentions);
        }
    }

    @Override
    public List<Long> listUnreadSpaceIds(User loginUser) {
        return fileMentionMapper.listUnreadSpaceIds(loginUser.getId());
    }

    @Override
    public List<FileMentionVO> listMyMentions(Long spaceId, User loginUser) {
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR, "空间 id 不能为空");
        return fileMentionMapper.listMentionsWithDetail(loginUser.getId(), spaceId);
    }

    @Override
    public boolean markRead(Long spaceId, User loginUser) {
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR, "空间 id 不能为空");
        return this.lambdaUpdate()
                .eq(FileMention::getToUserId, loginUser.getId())
                .eq(FileMention::getSpaceId, spaceId)
                .eq(FileMention::getIsRead, 0)
                .set(FileMention::getIsRead, 1)
                .set(FileMention::getUpdateTime, new Date())
                .update();
    }
}
