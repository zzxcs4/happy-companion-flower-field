package com.zzx.flowerfieldfrienddisk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.flowerfieldfrienddisk.model.dto.space.analyze.*;
import com.zzx.flowerfieldfrienddisk.model.entity.Space;
import com.zzx.flowerfieldfrienddisk.model.entity.User;
import com.zzx.flowerfieldfrienddisk.model.vo.space.analyze.*;

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

    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
