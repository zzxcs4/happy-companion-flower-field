package com.zzx.flowerfieldfrienddisk.model.dto.space;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceQuitRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 空间 id（社区空间）
     */
    private Long spaceId;
}