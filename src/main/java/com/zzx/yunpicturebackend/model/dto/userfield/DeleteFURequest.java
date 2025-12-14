package com.zzx.yunpicturebackend.model.dto.userfield;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteFURequest implements Serializable {

    /**
     * 花田id
     */
    private Long fieldId;

    /**
     * 用户id
     */
    private Long userId;
    private static final long serialVersionUID = 1L;
}
