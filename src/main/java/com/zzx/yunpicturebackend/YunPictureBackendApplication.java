package com.zzx.yunpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan("com.zzx.yunpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)

public class YunPictureBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(YunPictureBackendApplication.class, args);
    }

}
