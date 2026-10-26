package com.zzx.flowerfieldfrienddisk.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.zzx.flowerfieldfrienddisk.config.CosClientConfig;
import com.zzx.flowerfieldfrienddisk.exception.BusinessException;
import com.zzx.flowerfieldfrienddisk.exception.ErrorCode;
import com.zzx.flowerfieldfrienddisk.exception.ThrowUtils;
import com.zzx.flowerfieldfrienddisk.manager.CosManager;
import com.zzx.flowerfieldfrienddisk.model.dto.file.UploadFileResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 通用文件上传（支持各种格式）
 */
@Service
@Slf4j
public class FileUpload {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    // 允许的文件格式
    private static final List<String> ALLOWED_FORMATS = Arrays.asList(
            // 图片
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg",
            // 文档
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "json", "xml", "csv", "rtf",
            // 压缩包
            "zip", "rar", "7z", "tar", "gz",
            // 视频
            "mp4", "avi", "mov", "wmv", "flv", "mkv",
            // 音频
            "mp3", "wav", "flac", "aac", "ogg","m4a"
    );

    // 最大文件大小：100MB
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024L;

    /**
     * 上传文件
     */
    public UploadFileResult uploadFile(MultipartFile multipartFile, String uploadPathPrefix) {
        // 1. 校验文件
        validFile(multipartFile);

        // 2. 生成上传路径
        String originFilename = multipartFile.getOriginalFilename();
        String fileSuffix = FileUtil.getSuffix(originFilename);
        String uuid = RandomUtil.randomString(16);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, fileSuffix);
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

        File tempFile = null;
        try {
            // 3. 创建临时文件
            tempFile = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(tempFile);

            // 4. 上传到 COS
            cosManager.putObject(uploadPath, tempFile);

            // 5. 构造返回结果
            UploadFileResult result = new UploadFileResult();
            result.setUrl(cosClientConfig.getHost() + uploadPath);
            result.setName(FileUtil.mainName(originFilename));
            result.setSize(multipartFile.getSize());
            result.setFormat(fileSuffix);

            return result;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 6. 清理临时文件
            deleteTempFile(tempFile);
        }
    }

    /**
     * 校验文件
     */
    private void validFile(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null || multipartFile.isEmpty(),
                ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 校验文件大小
        long fileSize = multipartFile.getSize();
        ThrowUtils.throwIf(fileSize > MAX_FILE_SIZE,
                ErrorCode.PARAMS_ERROR, "文件大小不能超过 100MB");

        // 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!ALLOWED_FORMATS.contains(fileSuffix.toLowerCase()),
                ErrorCode.PARAMS_ERROR, "不支持的文件格式");
    }

    /**
     * 删除临时文件
     */
    private void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("临时文件删除失败, filepath = {}", file.getAbsolutePath());
        }
    }
}
