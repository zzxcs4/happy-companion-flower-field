package com.zzx.flowerfieldfrienddisk.controller;

import com.zzx.flowerfieldfrienddisk.common.BaseResponse;
import com.zzx.flowerfieldfrienddisk.common.DeleteRequest;
import com.zzx.flowerfieldfrienddisk.common.ResultUtils;
import com.zzx.flowerfieldfrienddisk.model.dto.filecomment.FileCommentAddRequest;
import com.zzx.flowerfieldfrienddisk.model.entity.FileComment;
import com.zzx.flowerfieldfrienddisk.model.entity.User;
import com.zzx.flowerfieldfrienddisk.model.vo.FileMentionVO;
import com.zzx.flowerfieldfrienddisk.model.vo.UserVO;
import com.zzx.flowerfieldfrienddisk.service.FileCommentService;
import com.zzx.flowerfieldfrienddisk.service.FileMentionService;
import com.zzx.flowerfieldfrienddisk.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 文件评论接口
 * 评论范围：团队空间、社区空间、公共空间（spaceId 为空）可评论，私有空间不支持
 */
@RestController
@Slf4j
@RequestMapping("/fileComment")
public class FileCommentController {

    @Resource
    private FileCommentService fileCommentService;
    @Resource
    private FileMentionService fileMentionService;
    @Resource
    private UserService userService;

    /**
     * 发布文件评论（支持 @ 用户）
     */
    @PostMapping("/add")
    public BaseResponse<Boolean> addComment(@RequestBody FileCommentAddRequest fileCommentAddRequest,
                                            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        boolean result = fileCommentService.addComment(
                fileCommentAddRequest.getFileId(),
                fileCommentAddRequest.getContent(),
                fileCommentAddRequest.getAtUserIds(),
                loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 查询文件评论列表
     */
    @GetMapping("/list")
    public BaseResponse<List<FileComment>> listComments(@RequestParam("fileId") Long fileId,
                                                        HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<FileComment> fileComments = fileCommentService.listComments(fileId, loginUser);
        return ResultUtils.success(fileComments);
    }

    /**
     * 删除评论（仅本人或管理员）
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteComment(@RequestBody DeleteRequest deleteRequest,
                                               HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        boolean result = fileCommentService.deleteComment(deleteRequest.getId(), loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 获取可 @ 的用户列表
     * 公共空间 → 关注的用户；团队/社区空间 → 空间成员
     */
    @GetMapping("/atUsers")
    public BaseResponse<List<UserVO>> listMentionableUsers(@RequestParam("fileId") Long fileId,
                                                           HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<UserVO> userVOList = fileCommentService.listMentionableUsers(fileId, loginUser);
        return ResultUtils.success(userVOList);
    }

    /**
     * 获取我有未读 @ 提及的空间 id 列表（菜单栏红点角标）
     */
    @GetMapping("/mentions/unreadSpaces")
    public BaseResponse<List<Long>> listUnreadMentionSpaces(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(fileMentionService.listUnreadSpaceIds(loginUser));
    }

    /**
     * 查询某空间内 @ 我的提及列表（「@我的」弹窗内容）
     */
    @GetMapping("/mentions/list")
    public BaseResponse<List<FileMentionVO>> listMyMentions(@RequestParam("spaceId") Long spaceId,
                                                            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(fileMentionService.listMyMentions(spaceId, loginUser));
    }

    /**
     * 将某空间内 @ 我的提及全部标记已读（红点消失）
     */
    @PostMapping("/mentions/read")
    public BaseResponse<Boolean> readMentions(@RequestParam("spaceId") Long spaceId,
                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(fileMentionService.markRead(spaceId, loginUser));
    }
}
