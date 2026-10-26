package com.zzx.flowerfieldfrienddisk.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.flowerfieldfrienddisk.model.dto.spaceuser.SpaceUserAddRequest;
import com.zzx.flowerfieldfrienddisk.model.dto.spaceuser.SpaceUserQueryRequest;
import com.zzx.flowerfieldfrienddisk.model.entity.SpaceUser;
import com.zzx.flowerfieldfrienddisk.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author zzx
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-10-03 15:51:21
*/
public interface SpaceUserService extends IService<SpaceUser> {

    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    void validSpaceUser(SpaceUser spaceUser, boolean add);
}
