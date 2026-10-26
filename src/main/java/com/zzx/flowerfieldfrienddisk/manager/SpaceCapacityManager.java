package com.zzx.flowerfieldfrienddisk.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzx.flowerfieldfrienddisk.constant.RedisConstants;
import com.zzx.flowerfieldfrienddisk.exception.BusinessException;
import com.zzx.flowerfieldfrienddisk.exception.ErrorCode;
import com.zzx.flowerfieldfrienddisk.manager.websocket.OnlineUserManager;
import com.zzx.flowerfieldfrienddisk.manager.websocket.model.PrivateMessageResponseMessage;
import com.zzx.flowerfieldfrienddisk.model.entity.Space;
import com.zzx.flowerfieldfrienddisk.model.entity.SpaceUser;
import com.zzx.flowerfieldfrienddisk.model.enums.SpaceRoleEnum;
import com.zzx.flowerfieldfrienddisk.service.SpaceService;
import com.zzx.flowerfieldfrienddisk.service.SpaceUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
import java.util.List;

/**
 * 花田空间容量管理：满时通知管理员并给予 300MB 宽限，宽限用完后禁止上传
 */
@Component
@Slf4j
public class SpaceCapacityManager {

    private static final long GRACE_SIZE = 300L * 1024 * 1024; // 300 MB

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    @Lazy
    private SpaceService spaceService;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private OnlineUserManager onlineUserManager;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 检查花田空间容量。
     * - 未满：直接返回
     * - 首次满：通知管理员，扩展 300MB 宽限，标记 Redis
     * - 宽限已用完：抛出异常，禁止上传
     */
    public void checkFieldSpaceCapacity(Space space) {
        if (space == null || space.getMaxSize() == null) {
            return;
        }
        long totalSize = space.getTotalSize() != null ? space.getTotalSize() : 0L;
        long maxSize = space.getMaxSize();

        if (totalSize < maxSize) {
            return;
        }

        String graceKey = RedisConstants.SPACE_CAPACITY_GRACE_KEY + space.getId();
        Boolean graceGiven = (Boolean) redisTemplate.opsForValue().get(graceKey);

        if (Boolean.TRUE.equals(graceGiven)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间容量已满，无法上传文件");
        }

        // 首次满：通知管理员并扩展宽限
        notifySpaceAdmins(space);
        spaceService.lambdaUpdate()
                .eq(Space::getId, space.getId())
                .set(Space::getMaxSize, maxSize + GRACE_SIZE)
                .update();
        redisTemplate.opsForValue().set(graceKey, Boolean.TRUE);
    }

    private void notifySpaceAdmins(Space space) {
        List<SpaceUser> admins = spaceUserService.lambdaQuery()
                .eq(SpaceUser::getSpaceId, space.getId())
                .eq(SpaceUser::getSpaceRole, SpaceRoleEnum.ADMIN.getValue())
                .list();

        for (SpaceUser admin : admins) {
            WebSocketSession session = onlineUserManager.getState(admin.getUserId());
            if (session == null || !session.isOpen()) {
                continue;
            }
            try {
                PrivateMessageResponseMessage msg = new PrivateMessageResponseMessage();
                msg.setStatus(1);
                msg.setMessage("spaceCapacityAlert");
                msg.setFromusername(space.getSpaceName());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsBytes(msg)));
            } catch (Exception e) {
                log.error("通知空间管理员失败, spaceId={}, adminId={}", space.getId(), admin.getUserId(), e);
            }
        }
    }
}
