package com.zzx.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.yunpicturebackend.model.dto.space.SpaceAddRequest;
import com.zzx.yunpicturebackend.model.dto.space.SpaceEditRequest;
import com.zzx.yunpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zzx.yunpicturebackend.model.dto.space.analyze.*;
import com.zzx.yunpicturebackend.model.entity.Space;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.vo.SpaceVO;
import com.zzx.yunpicturebackend.model.vo.space.analyze.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author zzx
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-09-23 20:14:09
*/

public interface SpaceAnalyzeService extends IService<Space> {


    void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser);

    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
