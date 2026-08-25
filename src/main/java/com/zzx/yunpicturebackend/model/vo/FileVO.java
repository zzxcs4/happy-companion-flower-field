package com.zzx.yunpicturebackend.model.vo;

import cn.hutool.json.JSONUtil;
import com.zzx.yunpicturebackend.model.entity.File;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class FileVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 文件 url
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 文件名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 分类
     */
    private String category;

    /**
     * 文件体积
     */
    private Long size;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片比例
     */
    private Double picScale;

    /**
     * 文件格式
     */
    private String format;

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 文件类型（image/video）
     */
    private String fileType;

    /**
     * 视频封面 url
     */
    private String coverUrl;

    /**
     * 视频时长（秒）
     */
    private Integer duration;

    /**
     * AI 生成的摘要
     */
    private String summary;

    /**
     * 创建用户信息
     */
    private UserVO user;

    private static final long serialVersionUID = 1L;

    /**
     * 封装类转对象
     */
    public static File voToObj(FileVO fileVO) {
        if (fileVO == null) {
            return null;
        }
        File file = new File();
        BeanUtils.copyProperties(fileVO, file);
        file.setTags(JSONUtil.toJsonStr(fileVO.getTags()));
        return file;
    }

    /**
     * 对象转封装类
     */
    public static FileVO objToVo(File file) {
        if (file == null) {
            return null;
        }
        FileVO fileVO = new FileVO();
        BeanUtils.copyProperties(file, fileVO);
        fileVO.setTags(JSONUtil.toList(file.getTags(), String.class));
        return fileVO;
    }
}
