package com.zzx.flowerfieldfrienddisk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan("com.zzx.flowerfieldfrienddisk.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)

public class FlowerFieldFriendDiskBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlowerFieldFriendDiskBackendApplication.class, args);
    }

}
