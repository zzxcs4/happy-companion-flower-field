package com.zzx.yunpicturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.zzx.yunpicturebackend.manager.auth.SpaceUserAuthManager;
import com.zzx.yunpicturebackend.manager.auth.SpaceUserPermissionConstant;
import com.zzx.yunpicturebackend.model.entity.File;
import com.zzx.yunpicturebackend.model.entity.Space;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.enums.SpaceTypeEnum;
import com.zzx.yunpicturebackend.service.FileService;
import com.zzx.yunpicturebackend.service.SpaceService;
import com.zzx.yunpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 文档协同编辑 WebSocket 握手拦截器
 * 校验用户登录、文件存在性、文件类型、编辑权限
 */
@Component
@Slf4j
public class DocumentEditHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private FileService fileService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 支持协同编辑的文本文件扩展名
     */
    private static final List<String> EDITABLE_EXTENSIONS = Arrays.asList(
            "txt", "md", "json", "xml", "csv", "rtf"
    );

    @Override
    public boolean beforeHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response,
                                   @NotNull WebSocketHandler wsHandler, @NotNull Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

            // 1. 获取文件 ID
            String fileId = servletRequest.getParameter("fileId");
            if (StrUtil.isBlank(fileId)) {
                log.error("缺少文件参数 fileId，拒绝握手");
                return false;
            }

            // 2. 校验用户登录
            User loginUser = userService.getLoginUser(servletRequest);
            if (ObjUtil.isEmpty(loginUser)) {
                log.error("用户未登录，拒绝握手");
                return false;
            }

            // 3. 校验文件存在
            File file = fileService.getById(fileId);
            if (file == null) {
                log.error("文件不存在，拒绝握手");
                return false;
            }

            // 4. 校验文件类型（仅允许文本文件）
            String format = file.getFormat();
            if (format == null || !EDITABLE_EXTENSIONS.contains(format.toLowerCase())) {
                log.error("文件类型 {} 不支持协同编辑，拒绝握手", format);
                return false;
            }

            // 5. 校验空间权限
            Long spaceId = file.getSpaceId();
            if (spaceId != null) {
                Space space = spaceService.getById(spaceId);
                if (space == null) {
                    log.error("空间不存在，拒绝握手");
                    return false;
                }
                if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
                    log.info("不是团队空间，拒绝握手");
                    return false;
                }
                List<String> permissionList = spaceUserAuthManager.getPermissionList(file, space, loginUser);
                if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                    log.error("没有编辑权限，拒绝握手");
                    return false;
                }
            }

            // 设置 attributes
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());
            attributes.put("fileId", Long.valueOf(fileId));
        }
        return true;
    }

    @Override
    public void afterHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response,
                               @NotNull WebSocketHandler wsHandler, Exception exception) {
    }
}
