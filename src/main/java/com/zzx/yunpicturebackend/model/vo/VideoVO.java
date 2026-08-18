package com.zzx.yunpicturebackend.model.vo;

import cn.hutool.json.JSONUtil;
import com.zzx.yunpicturebackend.model.entity.File;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class VideoVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 视频 url
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 名称
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
     * 视频格式
     */
    private String format;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();


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
     * 创建用户信息
     */
    private UserVO user;

    private static final long serialVersionUID = 1L;

    /**
     * 封装类转对象
     */
    public static File voToObj(com.zzx.yunpicturebackend.model.vo.PictureVO pictureVO) {
        if (pictureVO == null) {
            return null;
        }
        File file = new File();
        BeanUtils.copyProperties(pictureVO, file);
        // 类型不同，需要转换
        file.setTags(JSONUtil.toJsonStr(pictureVO.getTags()));
        return file;
    }

    /**
     * 对象转封装类
     */
    public static com.zzx.yunpicturebackend.model.vo.VideoVO objToVo(File file) {
        if (file == null) {
            return null;
        }
        com.zzx.yunpicturebackend.model.vo.VideoVO pictureVO = new com.zzx.yunpicturebackend.model.vo.VideoVO();
        BeanUtils.copyProperties(file, pictureVO);
        // 类型不同，需要转换
        pictureVO.setTags(JSONUtil.toList(file.getTags(), String.class));
        return pictureVO;
    }
}

