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
public class PictureVO implements Serializable {
  
    /**  
     * id  
     */  
    private Long id;  
  
    /**  
     * 图片 url  
     */  
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片主色调
     */
    private String picColor;

    /**  
     * 图片名称  
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
     * 图片格式  
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
     * 空间类型（0 私有 1 团队 2 社区；为空表示公共空间）
     */
    private Integer spaceType;

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

    /**
     * 视频时长
     */
    private  Integer duration;

    /**
     * ai摘要
     */
    private String summary;

    private static final long serialVersionUID = 1L;  
  
    /**  
     * 封装类转对象  
     */  
    public static File voToObj(PictureVO pictureVO) {
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
    public static PictureVO objToVo(File file) {
        if (file == null) {
            return null;  
        }  
        PictureVO pictureVO = new PictureVO();  
        BeanUtils.copyProperties(file, pictureVO);
        // 类型不同，需要转换  
        pictureVO.setTags(JSONUtil.toList(file.getTags(), String.class));
        return pictureVO;  
    }  
}
