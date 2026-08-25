package com.zzx.yunpicturebackend.controller;

import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.common.ResultUtils;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.vo.UnreadChatVO;
import com.zzx.yunpicturebackend.service.ChatListService;
import com.zzx.yunpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 私信未读提醒接口（轮询红点）
 */
@RestController
@Slf4j
@RequestMapping("/chat")
public class ChatController {

    @Resource
    private ChatListService chatListService;
    @Resource
    private UserService userService;

    /**
     * 查询我的未读私信会话列表（按发送者聚合未读数，供菜单红点与聊天列表红点使用）
     */
    @GetMapping("/unread")
    public BaseResponse<List<UnreadChatVO>> listUnreadConversations(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(chatListService.selectUnreadConversations(loginUser.getId()));
    }

    /**
     * 将某会话的私信全部标记已读（点击聊天列表项后红点消失）
     */
    @PostMapping("/read")
    public BaseResponse<Boolean> markConversationRead(@RequestParam("fromId") Long fromId,
                                                      HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(chatListService.markRead(fromId, loginUser.getId()));
    }
}
