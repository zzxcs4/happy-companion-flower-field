package com.zzx.yunpicturebackend.model.vo;

import com.zzx.yunpicturebackend.model.entity.Space;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class SpaceVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    private Long totalCount;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 空间类型：0-私有 1-团队 2-社区
     */
    private Integer spaceType;

    /**
     * 封面图
     */
    private String picUrl;

    /**
     * 可见状态：0-公开 1-私有 2-加密（仅社区空间）
     */
    private Integer status;

    /**
     * 社区密码（仅加密社区空间）
     */
    private String password;

    /**
     * 分类id（仅社区空间）
     */
    private Long categoryId;

    /**
     * 分类名称（冗余展示）
     */
    private String categoryName;

    /**
     * 最大人数（团队/社区空间）
     */
    private Integer maxNum;

    /**
     * 描述
     */
    private String description;

    /**
     * 帖子数量（计算冗余）
     */
    private Integer postNum;

    /**
     * 已加入的用户数
     */
    private Integer hasJoinNum;

    /**
     * 是否已加入（社区空间）
     */
    private boolean hasJoin = false;

    /**
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建用户信息
     */
    private UserVO user;

    private static final long serialVersionUID = 1L;

    /**
     * 封装类转对象
     *
     * @param spaceVO
     * @return
     */
    public static Space voToObj(SpaceVO spaceVO) {
        if (spaceVO == null) {
            return null;
        }
        Space space = new Space();
        BeanUtils.copyProperties(spaceVO, space);
        return space;
    }

    /**
     * 对象转封装类
     *
     * @param space
     * @return
     */
    public static SpaceVO objToVo(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVO spaceVO = new SpaceVO();
        BeanUtils.copyProperties(space, spaceVO);
        return spaceVO;
    }
}
