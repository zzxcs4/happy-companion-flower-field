package com.zzx.yunpicturebackend.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.common.ResultUtils;
import com.zzx.yunpicturebackend.constant.SystemConstants;
import com.zzx.yunpicturebackend.mapper.CommentMapper;
import com.zzx.yunpicturebackend.model.entity.Comment;
import com.zzx.yunpicturebackend.model.entity.Post;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.service.CommentService;
import com.zzx.yunpicturebackend.service.PostService;
import com.zzx.yunpicturebackend.service.UserFieldScoreService;
import com.zzx.yunpicturebackend.service.UserService;
import jodd.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
* @author Admin
* @description 针对表【news_headline】的数据库操作Service实现
* @createDate 2025-02-18 20:04:52
*/
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
    implements CommentService {
    @Autowired
    private CommentMapper commentMapper;
    @Resource
    private UserService userService;
    @Resource
    private PostService postService;
    @Resource
    private UserFieldScoreService userFieldScoreService;

    @Override
    public BaseResponse<List<Comment>> findAllComments(Long pid) {

        List<Comment> comments = commentMapper.getCommentWithUserName(pid);
        return ResultUtils.success(comments);
    }





    @Override
    public synchronized BaseResponse<Boolean> publish(Comment comment) {
        comment.setCreateTime(new Date());
        comment.setUpdateTime(new Date());
        Post post = postService.getById(comment.getPost());
        post.setComments(post.getComments() + 1);
        postService.updateById(post);
        commentMapper.insert(comment);
        // 评论 +0.34 分（3次约 +1 分）
        if (comment.getPublisher() != null && post.getFieldId() != null) {
            userFieldScoreService.addScore(comment.getPublisher(), post.getFieldId(), 0.34);
        }
        return ResultUtils.success(true);
    }



}




