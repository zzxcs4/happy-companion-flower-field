package com.zzx.yunpicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.model.entity.Comment;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


/**
* @author Admin
* @description 针对表【news_headline】的数据库操作Service
* @createDate 2025-02-18 20:04:53
*/
public interface CommentService extends IService<Comment> {

//    Result findNewPage(PortalVo portalVo);

//    Result showHeadlineDetail(Integer hid);

    BaseResponse<List<Comment>> findAllComments(Long pid);

    BaseResponse<Boolean> publish(Comment comment);
    
    /**
     * 删除评论（含评分回滚）
     *
     * @param commentId 评论 id
     */
    BaseResponse<Boolean> deleteComment(Long commentId);


//    Result findHeadlineByHid(Integer hid);
//
//    Result updateHeadline(Headline headline);
}
