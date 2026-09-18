package com.zzx.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.yunpicturebackend.model.dto.space.SpaceAddRequest;
import com.zzx.yunpicturebackend.model.dto.space.SpaceEditRequest;
import com.zzx.yunpicturebackend.model.dto.space.SpaceJoinRequest;
import com.zzx.yunpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zzx.yunpicturebackend.model.dto.space.SpaceQuitRequest;
import com.zzx.yunpicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.vo.SpaceVO;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author zzx
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-09-23 20:14:09
*/

public interface SpaceService extends IService<Space> {

    boolean deleteSpace(long SpaceId, User loginUser);

    void editSpace(SpaceEditRequest SpaceEditRequest, User loginUser);

    void validSpace(Space space, boolean add);

    void fillSpaceBySpaceLevel(Space space);

    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    void checkSpaceAuth(User loginUser, Space space);

    /**
     * 加入社区空间
     */
    boolean joinSpace(SpaceJoinRequest spaceJoinRequest, User loginUser);

    /**
     * 加入私有社区空间（邀请）
     */
    boolean joinPrivateSpace(SpaceJoinRequest spaceJoinRequest, User loginUser);

    /**
     * 退出社区空间
     */
    boolean quitSpace(SpaceQuitRequest spaceQuitRequest, User loginUser);

    /**
     * 分页/查询社区空间列表（用于花田列表 / 我创建的 / 我加入的）
     */
    List<SpaceVO> listCommunitySpaces(SpaceQueryRequest spaceQueryRequest, boolean isAdmin);

    /**
     * 查询社区空间的成员列表
     */
    List<User> getSpaceMemberList(Long spaceId, String searchText);
}
