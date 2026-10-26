package com.zzx.flowerfieldfrienddisk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzx.flowerfieldfrienddisk.model.entity.Comment;


import java.util.List;

/**
* @author Admin
* @description 针对表【news_headline】的数据库操作Mapper
* @createDate 2025-02-18 20:04:53
* @Entity com.example.pojo.Headline
*/
public interface CommentMapper extends BaseMapper<Comment> {

    List<Comment> getCommentWithUserName(Long pid);

    //自定义分页查询方法
//    IPage<Map> selectMyPage(IPage page,
//                             @Param("portalVo") PortalVo portalVo);
//
//
//    Map selectDetailMap(Integer hid);
}




