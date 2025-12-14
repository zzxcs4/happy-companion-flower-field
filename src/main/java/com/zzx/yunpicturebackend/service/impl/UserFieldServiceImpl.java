package com.zzx.yunpicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.yunpicturebackend.model.entity.UserField;
import com.zzx.yunpicturebackend.service.UserFieldService;
import com.zzx.yunpicturebackend.mapper.UserFieldMapper;
import org.springframework.stereotype.Service;

/**
* @author zzx
* @description 针对表【user_field(用户花田关系)】的数据库操作Service实现
* @createDate 2025-10-19 17:41:20
*/
@Service
public class UserFieldServiceImpl extends ServiceImpl<UserFieldMapper, UserField>
    implements UserFieldService{

}




