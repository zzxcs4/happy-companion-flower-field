package com.zzx.yunpicturebackend.model.vo;

import lombok.Data;

import java.util.List;
@Data
public class ScrollVO {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}