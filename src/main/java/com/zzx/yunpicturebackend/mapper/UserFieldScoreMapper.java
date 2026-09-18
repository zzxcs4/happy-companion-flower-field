package com.zzx.yunpicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzx.yunpicturebackend.model.entity.UserFieldScore;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户-花田评分表 Mapper
 * 基于唯一索引 uk_user_field(user_id, space_id) 提供原子累加的 UPSERT 能力，
 * 避免业务侧"读-改-写"竞争导致的积分丢失更新。
 */
@Mapper
public interface UserFieldScoreMapper extends BaseMapper<UserFieldScore> {

    /**
     * 原子新增或累加评分：
     * - 不存在 user_id+space_id 记录时插入，score = delta
     * - 已存在时 score = score + delta
     * - 无论新增还是累加，最终裁剪到 [0, 10]，四舍五入保留 4 位小数
     *
     * @return 影响行数
     */
    @Insert("INSERT INTO user_field_score (user_id, space_id, score) VALUES (#{userId}, #{spaceId}, #{delta}) " +
            "ON DUPLICATE KEY UPDATE score = LEAST(GREATEST(ROUND(score + VALUES(score), 4), 0), 10)")
    int addScoreAtomic(@Param("userId") Long userId, @Param("spaceId") Long spaceId, @Param("delta") double delta);
}
