package com.zzx.flowerfieldfrienddisk.model.dto.space;

import com.zzx.flowerfieldfrienddisk.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * id 列表
     */
    private List<Long> idList;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间类型：0-私有 1-团队 2-社区
     */
    private Integer spaceType;

    /**
     * 可见状态：0-公开 1-私有 2-加密（仅社区空间）
     */
    private Integer status;

    /**
     * 分类id（仅社区空间）
     */
    private Long categoryId;

    /**
     * 搜索关键词（对空间名称和描述搜索）
     */
    private String searchText;

    /**
     * 描述
     */
    private String description;

    private static final long serialVersionUID = 1L;
}
