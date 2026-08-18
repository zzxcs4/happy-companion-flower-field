package com.zzx.yunpicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.yunpicturebackend.model.dto.picture.PictureUploadRequest;
import com.zzx.yunpicturebackend.model.entity.File;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.vo.FileVO;
import com.zzx.yunpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileService  extends IService<File> {

    FileVO uploadFile(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);

    void fillReviewParams(Long spaceId, File file, User loginUser);
}
