package com.zzx.flowerfieldfrienddisk.model.dto.user;

import lombok.Data;

import java.io.Serializable;
@Data
public class UserAvatarUploadRequest implements Serializable {


    /**
     * 文件地址
     */
    private String fileUrl;



    /**
     * 用户 id
     */
    private Long userId;


    private static final long serialVersionUID = 1L;
}
