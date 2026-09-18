package com.zzx.yunpicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzx.yunpicturebackend.model.entity.Space;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author zzx
* @description 针对表【space(空间)】的数据库操作Mapper
* @createDate 2025-09-23 20:14:09
* @Entity com.zzx.yunpicturebackend.model.entity.Space
*/
public interface SpaceMapper extends BaseMapper<Space> {

    /**
     * 按实时帖子数降序查询社区空间（用于冷启动推荐降级）
     */
    @Select("SELECT s.* FROM space s " +
            "LEFT JOIN post p ON p.spaceId = s.id " +
            "WHERE s.isDelete = 0 AND s.spaceType = 2 " +
            "GROUP BY s.id " +
            "ORDER BY COUNT(p.id) DESC " +
            "LIMIT #{topN}")
    List<Space> selectTopSpacesByPostCount(@Param("topN") int topN);

    @Select("SELECT s.spaceName " +
            "FROM space s " +
            "INNER JOIN space_user su ON s.id = su.spaceId " +
            "WHERE su.userId = #{userId} " +
            "AND s.spaceType = 1")
    List<String> getTeamSpaceNamesByUserId(@Param("userId") Long userId);

    // 判断是否创建过团队空间
    @Select("SELECT COUNT(1) > 0 FROM space WHERE userId = #{userId} AND spaceType = 1")
    boolean hasCreatedTeamSpace(@Param("userId") Long userId);

}