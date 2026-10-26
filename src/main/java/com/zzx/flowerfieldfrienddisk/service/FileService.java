package com.zzx.flowerfieldfrienddisk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.flowerfieldfrienddisk.model.dto.picture.PictureUploadRequest;
import com.zzx.flowerfieldfrienddisk.model.entity.File;
import com.zzx.flowerfieldfrienddisk.model.entity.User;
import com.zzx.flowerfieldfrienddisk.model.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileService  extends IService<File> {

    FileVO uploadFile(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);

    void fillReviewParams(Long spaceId, File file, User loginUser);
}
