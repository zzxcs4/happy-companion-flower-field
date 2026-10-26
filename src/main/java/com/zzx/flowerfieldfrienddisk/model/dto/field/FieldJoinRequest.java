package com.zzx.flowerfieldfrienddisk.model.dto.field;

import lombok.Data;

import java.io.Serializable;

@Data
public class FieldJoinRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * id
     */
    private Long fieldId;

    /**
     * 密码
     */
    private String password;
}