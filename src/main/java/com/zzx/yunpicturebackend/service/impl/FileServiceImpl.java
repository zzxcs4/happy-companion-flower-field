package com.zzx.yunpicturebackend.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzx.yunpicturebackend.exception.BusinessException;
import com.zzx.yunpicturebackend.exception.ErrorCode;
import com.zzx.yunpicturebackend.exception.ThrowUtils;
import com.zzx.yunpicturebackend.manager.CosManager;
import com.zzx.yunpicturebackend.manager.SpaceCapacityManager;
import com.zzx.yunpicturebackend.manager.upload.FileUpload;
import com.zzx.yunpicturebackend.mapper.FileMapper;
import com.zzx.yunpicturebackend.model.dto.file.UploadFileResult;
import com.zzx.yunpicturebackend.model.dto.picture.PictureUploadRequest;
import com.zzx.yunpicturebackend.model.entity.File;
import com.zzx.yunpicturebackend.model.entity.Space;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.enums.PictureReviewStatusEnum;
import com.zzx.yunpicturebackend.model.vo.FileVO;
import com.zzx.yunpicturebackend.service.AudioCompressService;
import com.zzx.yunpicturebackend.service.FileService;
import com.zzx.yunpicturebackend.service.VideoTranscodeService;
import com.zzx.yunpicturebackend.service.SpaceService;
import com.zzx.yunpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Date;

@Service
@Slf4j
public class FileServiceImpl extends ServiceImpl<FileMapper, File>
        implements FileService {

    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private FileUpload fileUpload;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private SpaceCapacityManager spaceCapacityManager;
    @Resource
    private AudioCompressService audioCompressService;
    @Resource
    private VideoTranscodeService videoTranscodeService;
    @Resource
    private CosManager cosManager;
    /** 私有视频签名 URL 有效期（秒）：12 小时 */
    private static final long SIGN_URL_EXPIRE_SECONDS = 12 * 60 * 60L;

    @Override
    public FileVO uploadFile(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        Long spaceId = pictureUploadRequest != null ? pictureUploadRequest.getSpaceId() : null;
        Space space = null;

        if (spaceId != null) {
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            if (space.getSpaceType() == 0 || space.getSpaceType() == 1) {
                if (space.getTotalCount() >= space.getMaxCount()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
                }
                if (space.getTotalSize() >= space.getMaxSize()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
                }
            }
            if (space.getSpaceType() == 2) {
                spaceCapacityManager.checkFieldSpaceCapacity(space);
            }
        }

        // 判断新增还是更新
        Long fileId = pictureUploadRequest != null ? pictureUploadRequest.getId() : null;
        if (fileId != null) {
            File oldFile = this.getById(fileId);
            ThrowUtils.throwIf(oldFile == null, ErrorCode.NOT_FOUND_ERROR, "文件不存在");
            if (spaceId == null && oldFile.getSpaceId() != null) {
                spaceId = oldFile.getSpaceId();
            } else if (spaceId != null && ObjUtil.notEqual(spaceId, oldFile.getSpaceId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
            }
        }

        // 上传路径
        String uploadPathPrefix = spaceId == null
                ? String.format("public/%s", loginUser.getId())
                : String.format("space/%s", spaceId);

        // 上传文件（通用，支持各种格式）
        UploadFileResult uploadResult = fileUpload.uploadFile(multipartFile, uploadPathPrefix);

        // 视频对象设为私有读（防下载），前端通过临时签名 URL 播放
        if (videoTranscodeService.isVideoFormat(uploadResult.getFormat())) {
            String host = cosManager.getCosClientConfig().getHost();
            String url = uploadResult.getUrl();
            String key = url.startsWith(host) ? url.substring(host.length()) : url;
            key = "/" + key.replaceAll("^/+", "");
            cosManager.setObjectPrivate(key);
        }

        // 构造入库对象
        File file = new File();
        file.setSpaceId(spaceId);
        file.setUrl(uploadResult.getUrl());
        String fileName = uploadResult.getName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            fileName = pictureUploadRequest.getPicName();
        }
        file.setName(fileName);
        file.setSize(uploadResult.getSize());
        file.setFormat(uploadResult.getFormat());
        file.setUserId(loginUser.getId());
        // 是否允许下载：0-不允许 1-允许（不传则保持默认允许）
        if (pictureUploadRequest != null && pictureUploadRequest.getCanDownload() != null) {
            file.setCanDownload(pictureUploadRequest.getCanDownload());
        }

        fillReviewParams(spaceId, file, loginUser);

        if (fileId != null) {
            file.setId(fileId);
            file.setEditTime(new Date());
        }

        Long finalSpaceId = spaceId;
        Space finalSpace = space;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(file);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "文件上传失败");
            if (finalSpace != null && (finalSpace.getSpaceType() == 0 || finalSpace.getSpaceType() == 1)) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + file.getSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return file;
        });

        // 音频上传成功后异步压缩为 MP3 128kbps（失败不影响上传结果）
        if (audioCompressService.isAudioFormat(file.getFormat())) {
            audioCompressService.compressAsync(file.getId());
        }
        // 视频上传成功后异步转码为 H.264 MP4（失败不影响上传结果）
        if (videoTranscodeService.isVideoFormat(file.getFormat())) {
            videoTranscodeService.transcodeAsync(file.getId());
        }

        FileVO fileVO = FileVO.objToVo(file);
        // 私有视频返回前端前转为临时签名 URL（12 小时有效期）
        if (fileVO != null && StrUtil.isNotBlank(fileVO.getUrl())
                && videoTranscodeService.isVideoFormat(file.getFormat())) {
            fileVO.setUrl(cosManager.signCosUrl(fileVO.getUrl(), SIGN_URL_EXPIRE_SECONDS));
        }
        return fileVO;
    }

    @Override
    public void fillReviewParams(Long spaceId, File file, User loginUser) {
        Space space = spaceId != null ? spaceService.getById(spaceId) : null;
        if (userService.isAdmin(loginUser) || (space != null && space.getUserId().equals(loginUser.getId()))) {
            file.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            file.setReviewerId(loginUser.getId());
            file.setReviewMessage("管理员或空间创建者自动过审");
            file.setReviewTime(new Date());
        } else {
            file.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }
}