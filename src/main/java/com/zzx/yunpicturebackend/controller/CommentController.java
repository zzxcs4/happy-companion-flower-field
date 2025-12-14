package com.zzx.yunpicturebackend.controller;


import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.common.DeleteRequest;
import com.zzx.yunpicturebackend.common.ResultUtils;
import com.zzx.yunpicturebackend.model.entity.Comment;
import com.zzx.yunpicturebackend.service.CommentService;
import com.zzx.yunpicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("comment")
@CrossOrigin
public class CommentController {
    @Autowired
    private CommentService commentService;
    @Resource
    private UserService userService;
    @PostMapping("publish")
    public BaseResponse<Boolean> publish(@RequestBody Comment comment, HttpServletRequest request){
        Long userId = userService.getLoginUser(request).getId();
        comment.setPublisher(userId);
        return commentService.publish(comment);
    }
//    @PostMapping("findHeadlineByHid")
//    public Result findHeadlineByHid(Integer hid){
//        Result result=headlineService.findHeadlineByHid(hid);
//        return result;
//    }
//    @PostMapping("update")
//    public Result update(@RequestBody Headline headline){
//        Result result=headlineService.updateHeadline(headline);
//        return result;
//    }
    @GetMapping("/listComments")
    public BaseResponse<List<Comment>> findAllComments(@RequestParam("id") Long postId, HttpServletRequest request) {

        return commentService.findAllComments(postId);
    }
    @PostMapping("removeByCid")
    public BaseResponse<Boolean> delete(@RequestBody DeleteRequest deleteRequest){
        commentService.removeById(deleteRequest.getId());
        return ResultUtils.success(true);
    }
}
