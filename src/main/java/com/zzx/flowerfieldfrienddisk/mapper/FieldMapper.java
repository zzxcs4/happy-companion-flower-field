package com.zzx.flowerfieldfrienddisk.mapper;

import com.zzx.flowerfieldfrienddisk.model.entity.Field;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author zzx
* @description 针对表【field(花田)】的数据库操作Mapper
* @createDate 2025-10-21 21:19:14
* @Entity com.zzx.yunpicturebackend.model.entity.Field
*/
public interface FieldMapper extends BaseMapper<Field> {

    /**
     * 按实时帖子数降序查询花田（用于冷启动推荐降级）
     */
    @Select("SELECT f.* FROM field f " +
            "LEFT JOIN post p ON p.fieldId = f.id " +
            "WHERE f.isDelete = 0 " +
            "GROUP BY f.id " +
            "ORDER BY COUNT(p.id) DESC " +
            "LIMIT #{topN}")
    List<Field> selectTopFieldsByPostCount(@Param("topN") int topN);
}




