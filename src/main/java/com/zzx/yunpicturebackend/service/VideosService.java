package com.zzx.yunpicturebackend.service;

import com.qcloud.cos.model.CompleteMultipartUploadResult;
import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.model.dto.picture.PictureEditRequest;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.entity.Video;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.yunpicturebackend.model.vo.VideoVO;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

/**
* @author zzx
* @description 针对表【videos(视频信息表)】的数据库操作Service
* @createDate 2026-04-04 12:03:16
*/
public interface VideosService extends IService<com.zzx.yunpicturebackend.model.entity.File> {

    BaseResponse<Boolean> checkFile(String fileMd5);

    BaseResponse<Boolean> checkChunk(String fileMd5, int chunk);

    BaseResponse<VideoVO> IdempotentMultipartUploader(File file, Long spaceId, String md5, Long videoId,HttpServletRequest request);

    void editFile(PictureEditRequest pictureEditRequest, User loginUser);

    void fillReviewParams(Long spaceId, com.zzx.yunpicturebackend.model.entity.File file, User loginUser);

    void validFile(com.zzx.yunpicturebackend.model.entity.File file);
}
