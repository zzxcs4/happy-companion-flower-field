package com.zzx.flowerfieldfrienddisk.mapper;

import com.zzx.flowerfieldfrienddisk.model.entity.File;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author zzx
* @description 针对表【picture(图片)】的数据库操作Mapper
* @createDate 2025-09-16 11:15:43
* @Entity com.zzx.yunpicturebackend.model.entity.Picture
*/
public interface FileMapper extends BaseMapper<File> {

        List<Object> selectPicSizeBySpaceType(@Param("spaceId")Long spaceId,@Param("queryAll") boolean queryAll);

}




