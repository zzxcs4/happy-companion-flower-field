package com.zzx.flowerfieldfrienddisk.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.flowerfieldfrienddisk.mapper.CategoryMapper;
import com.zzx.flowerfieldfrienddisk.model.entity.Category;
import com.zzx.flowerfieldfrienddisk.service.CategoryService;
import org.springframework.stereotype.Service;

/**
* @author zzx
* @description 针对表【category(空间分类)】的数据库操作Service实现
*/
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category>
    implements CategoryService {

}