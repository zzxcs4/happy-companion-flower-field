package com.zzx.flowerfieldfrienddisk.model.vo;

import com.zzx.flowerfieldfrienddisk.model.entity.File;
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
    public static File voToObj(com.zzx.flowerfieldfrienddisk.model.vo.PictureVO pictureVO) {
        if (pictureVO == null) {
            return null;
        }
        File file = new File();
        BeanUtils.copyProperties(pictureVO, file);
        return file;
    }

    /**
     * 对象转封装类
     */
    public static com.zzx.flowerfieldfrienddisk.model.vo.VideoVO objToVo(File file) {
        if (file == null) {
            return null;
        }
        com.zzx.flowerfieldfrienddisk.model.vo.VideoVO pictureVO = new com.zzx.flowerfieldfrienddisk.model.vo.VideoVO();
        BeanUtils.copyProperties(file, pictureVO);
        return pictureVO;
    }
}

