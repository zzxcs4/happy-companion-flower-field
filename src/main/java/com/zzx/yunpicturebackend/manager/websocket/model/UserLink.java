package com.zzx.yunpicturebackend.manager.websocket.model;

import lombok.Data;

@Data
public class UserLink {
    private int linkId;
    private Long from;
    private Long to;
}
