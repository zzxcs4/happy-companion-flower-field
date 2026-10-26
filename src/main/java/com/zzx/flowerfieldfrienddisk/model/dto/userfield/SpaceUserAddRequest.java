package com.zzx.flowerfieldfrienddisk.model.dto.userfield;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserAddRequest implements Serializable {

    /**
     * 空间 ID
     */
    private Long fieldId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 花田角色：viewer/editor/admin
     */
    //private String spaceRole;

    private static final long serialVersionUID = 1L;
}