package com.zzx.flowerfieldfrienddisk.utils;




import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class ListToPageConverter {

    /**
     * 将List转换为Page对象
     * @param list 原始数据列表
     * @param pageable 分页参数（包含页号、每页大小等）
     * @param <T> 数据类型
     * @return 分页对象Page<T>
     */
    public static <T> Page<T> convertListToPage(List<T> list, Pageable pageable) {
        int total = list.size(); // 总条数
        int pageSize = pageable.getPageSize(); // 每页大小
        int currentPage = pageable.getPageNumber(); // 当前页号（从0开始）

        // 计算起始索引（避免越界）
        int start = Math.min(currentPage * pageSize, total);
        // 计算结束索引（避免越界）
        int end = Math.min(start + pageSize, total);

        // 截取当前页数据
        List<T> pageContent = list.subList(start, end);

        // 构造并返回Page对象
        return new PageImpl<>(pageContent, pageable, total);
    }
}