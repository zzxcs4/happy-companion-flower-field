package com.zzx.yunpicturebackend.controller;

import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class MainController {

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }
}
