package com.zzx.yunpicturebackend.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.zzx.yunpicturebackend.annotation.AuthCheck;
import com.zzx.yunpicturebackend.api.aliyunai.AliyunVisionService;
import com.zzx.yunpicturebackend.api.aliyunai.LlmApiService;
import com.zzx.yunpicturebackend.api.siliconflow.SiliconFlowAsrService;
import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.common.ResultUtils;
import com.zzx.yunpicturebackend.config.CosClientConfig;
import com.zzx.yunpicturebackend.constant.UserConstant;
import com.zzx.yunpicturebackend.exception.BusinessException;
import com.zzx.yunpicturebackend.exception.ErrorCode;
import com.zzx.yunpicturebackend.exception.ThrowUtils;
import com.zzx.yunpicturebackend.manager.CosManager;
import com.zzx.yunpicturebackend.manager.VideoAiManager;
import com.zzx.yunpicturebackend.manager.VideoThumbnailManager;
import com.zzx.yunpicturebackend.manager.auth.SpaceUserAuthManager;
import com.zzx.yunpicturebackend.manager.auth.SpaceUserPermissionConstant;
import com.zzx.yunpicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.zzx.yunpicturebackend.model.dto.file.FileSummaryRequest;
import com.zzx.yunpicturebackend.model.dto.picture.PictureUploadRequest;
import com.zzx.yunpicturebackend.model.entity.File;

import com.zzx.yunpicturebackend.model.entity.Space;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.vo.FileVO;
import com.zzx.yunpicturebackend.model.vo.PictureVO;
import com.zzx.yunpicturebackend.service.DocumentTextExtractor;
import com.zzx.yunpicturebackend.service.FileService;
import com.zzx.yunpicturebackend.service.SpaceService;
import com.zzx.yunpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/file")
public class FileController {
    @Resource
    CosManager cosManager;
    @Resource
    UserService userService;
    @Resource
    FileService fileService;
    @Resource
    DocumentTextExtractor documentTextExtractor;
    @Resource
    LlmApiService llmApiService;
    @Resource
    SiliconFlowAsrService siliconFlowAsrService;
    @Resource
    AliyunVisionService aliyunVisionService;
    @Resource
    VideoAiManager videoAiManager;
    @Resource
    VideoThumbnailManager videoThumbnailManager;
    @Resource
    CosClientConfig cosClientConfig;
    @Resource
    SpaceService spaceService;
    @Resource
    SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 支持 AI 摘要的视频格式（与前端 isVideo 保持一致）
     */
    private static final List<String> VIDEO_FORMATS = Arrays.asList(
            "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm"
    );

    /**
     * 可直接以纯文本编辑的格式（与前端 isEditableText 保持一致）
     */
    private static final List<String> EDITABLE_TEXT_FORMATS = Arrays.asList(
            "txt", "md", "json", "xml", "csv", "rtf"
    );

    /**
     * 仅支持提取文本预览、不可编辑的格式（doc 为老格式，编辑会丢失样式，故只读预览）
     */
    private static final List<String> PREVIEW_ONLY_FORMATS = Arrays.asList(
            "doc"
    );

    /**
     * 支持设置封面的音频格式（与前端 isAudio 保持一致）
     */
    private static final List<String> AUDIO_FORMATS = Arrays.asList(
            "mp3", "wav", "flac", "m4a", "ogg", "wma", "aac"
    );

    /**
     * 封面图片允许的格式
     */
    private static final List<String> COVER_IMAGE_FORMATS = Arrays.asList("jpg", "jpeg", "png", "webp");
    /**
     * 测试文件上传
     *
     * @param multipartFile
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        // 文件目录
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);
        java.io.File file = null;
        try {
            // 上传文件
            file = java.io.File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath, file);
            // 返回可访问地址
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }

    /**
     * 测试文件下载
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            // 处理下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }
    /**
     * 上传文件（可重新上传）
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<FileVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        FileVO pictureVO = fileService.uploadFile(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 上传 / 更换文件封面图片（仅视频、音频，写入缩略图字段 thumbnailUrl）
     */
    @PostMapping("/cover/upload")
    public BaseResponse<String> uploadFileCover(@RequestPart("file") MultipartFile multipartFile,
                                                @RequestParam Long fileId,
                                                HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        File file = fileService.getById(fileId);
        ThrowUtils.throwIf(file == null, ErrorCode.NOT_FOUND_ERROR, "文件不存在");
        // 仅视频/音频支持设置封面，且需编辑权限（请求体的 fileId 不在上下文字段中，手动校验避免绕过）
        String format = file.getFormat() == null ? null : file.getFormat().toLowerCase();
        ThrowUtils.throwIf(format == null || (!VIDEO_FORMATS.contains(format) && !AUDIO_FORMATS.contains(format)),
                ErrorCode.PARAMS_ERROR, "仅视频和音频文件支持设置封面");
        checkFilePermission(file, loginUser, SpaceUserPermissionConstant.PICTURE_EDIT, "无权限修改该文件封面");
        // 校验封面图片（大小、格式）
        ThrowUtils.throwIf(multipartFile == null || multipartFile.isEmpty(), ErrorCode.PARAMS_ERROR, "封面图片不能为空");
        ThrowUtils.throwIf(multipartFile.getSize() > 2 * 1024 * 1024L, ErrorCode.PARAMS_ERROR, "封面图片大小不能超过 2MB");
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(StrUtil.isBlank(suffix) || !COVER_IMAGE_FORMATS.contains(suffix.toLowerCase()),
                ErrorCode.PARAMS_ERROR, "封面图片仅支持 jpg、jpeg、png、webp 格式");

        // 上传封面到 COS
        String coverKey = String.format("/cover/%s_%s.%s", fileId, RandomUtil.randomString(8), suffix.toLowerCase());
        java.io.File tempFile = null;
        try {
            tempFile = java.io.File.createTempFile("cover-", "." + suffix.toLowerCase());
            multipartFile.transferTo(tempFile);
            cosManager.putObject(coverKey, tempFile);
        } catch (Exception e) {
            log.error("封面上传失败, fileId={}", fileId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "封面上传失败");
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
        String newCoverUrl = cosClientConfig.getHost() + coverKey;

        // 清理旧封面（仅限本站封面类对象，避免误删；失败不阻断主流程）
        String oldCoverUrl = file.getThumbnailUrl();
        if (StrUtil.isNotBlank(oldCoverUrl) && oldCoverUrl.startsWith(cosClientConfig.getHost())
                && (oldCoverUrl.contains("/cover/") || oldCoverUrl.contains("_thumbnail."))) {
            try {
                cosManager.deleteObject(oldCoverUrl.substring(cosClientConfig.getHost().length()));
            } catch (Exception e) {
                log.warn("旧封面删除失败, url={}", oldCoverUrl, e);
            }
        }

        // 写入缩略图字段（数据库中的缩略图即封面）
        File updateFile = new File();
        updateFile.setId(fileId);
        updateFile.setThumbnailUrl(newCoverUrl);
        ThrowUtils.throwIf(!fileService.updateById(updateFile), ErrorCode.OPERATION_ERROR, "封面保存失败");
        return ResultUtils.success(newCoverUrl);
    }

    /**
     * 生成文件 AI 摘要
     * 支持 PDF、Word、TXT、Markdown 等文档类型、视频（FFmpeg 抽帧 + ASR 转写 + LLM 摘要），
     * 以及图片（阿里云百炼 qwen3.8-max 识图）
     */
    @PostMapping("/summary/generate")
    public BaseResponse<String> generateFileSummary(@RequestBody FileSummaryRequest fileSummaryRequest,
                                                    HttpServletRequest request) {
        ThrowUtils.throwIf(fileSummaryRequest == null || fileSummaryRequest.getFileId() == null,
                ErrorCode.PARAMS_ERROR, "文件 id 不能为空");
        User loginUser = userService.getLoginUser(request);

        // 获取文件信息
        File file = fileService.getById(fileSummaryRequest.getFileId());
        ThrowUtils.throwIf(file == null, ErrorCode.NOT_FOUND_ERROR, "文件不存在");
        // 手动校验空间查看权限（请求体的 fileId 不在上下文字段中，直接基于文件+空间对象校验，避免绕过）
        checkFilePermission(file, loginUser, SpaceUserPermissionConstant.PICTURE_VIEW, "无权限访问该文件");

        String format = file.getFormat();
        String summary;
        if (format != null && VIDEO_FORMATS.contains(format.toLowerCase())) {
            // 视频摘要管线：抽帧 + 音频提取 + ASR + LLM
            summary = generateVideoSummary(file);
        } else if (aliyunVisionService.isSupportedFormat(format)) {
            // 图片识图：直接传图片 URL 给阿里云百炼 qwen3.8-max（URL 需可公网访问）
            summary = aliyunVisionService.recognizeImage(file.getUrl());
        } else if (documentTextExtractor.isSupported(format)) {
            // 文档摘要：提取文本 + LLM 生成摘要
            String text = documentTextExtractor.extractText(file.getUrl(), format);
            if (text == null || text.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "无法从文件中提取文本内容");
            }
            summary = llmApiService.generateSummary(text);
        } else {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "当前文件类型不支持 AI 摘要，支持的格式：PDF、Word、TXT、Markdown、Excel、PPT、视频、图片");
        }

        // 保存摘要到数据库（支持后续 LIKE 检索）
        file.setSummary(summary);
        boolean updated = fileService.updateById(file);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "保存摘要失败");

        return ResultUtils.success(summary);
    }

    /**
     * 视频 AI 摘要生成管线：
     * 1. 从 COS 下载视频 → 2. ffprobe 探测时长 → 3. FFmpeg 抽帧（上传封面）
     * → 4. FFmpeg 提取音频 → 5. ASR 转写 → 6. LLM 生成摘要
     */
    private String generateVideoSummary(File file) {
        ThrowUtils.throwIf(!videoAiManager.isAvailable(), ErrorCode.OPERATION_ERROR,
                "服务器未安装 FFmpeg，无法生成视频摘要");

        java.io.File tempVideo = null;
        java.io.File tempAudio = null;
        java.io.File tempFrame = null;
        try {
            // 1. 从 COS 下载视频到临时文件（直接落盘，避免大文件占用堆内存）
            String url = file.getUrl();
            String host = cosClientConfig.getHost();
            String key = url.startsWith(host) ? url.substring(host.length()) : url;
            tempVideo = java.io.File.createTempFile("video-ai-", "." + file.getFormat().toLowerCase());
            COSObject cosObject = cosManager.getObject(key);
            try (COSObjectInputStream cosInput = cosObject.getObjectContent()) {
                java.nio.file.Files.copy(cosInput, tempVideo.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // 2. 探测时长（ASR 限制 1 小时）
            long duration = videoAiManager.probeDuration(tempVideo);
            ThrowUtils.throwIf(duration > 3600, ErrorCode.OPERATION_ERROR, "视频时长超过 1 小时，暂不支持生成摘要");
            log.info("视频摘要开始, fileId={}, duration={}s, size={}MB", file.getId(), duration, tempVideo.length() / 1024 / 1024);

            // 3. FFmpeg 抽帧（取 25% 进度处的关键帧，避开片头黑屏），上传作为封面
            if (videoThumbnailManager.isAvailable() && duration > 2) {
                try {
                    tempFrame = java.io.File.createTempFile("video-frame-", ".jpg");
                    videoThumbnailManager.generateThumbnail(tempVideo, tempFrame.getAbsolutePath(), (int) (duration * 0.25));
                    String frameKey = "/video/cover/" + file.getId() + ".jpg";
                    cosManager.putObject(frameKey, tempFrame);
                    file.setCoverUrl(host + frameKey);
                } catch (Exception e) {
                    // 抽帧失败不阻断摘要主流程
                    log.warn("视频抽帧失败, fileId={}", file.getId(), e);
                }
            }

            // 4. FFmpeg 提取音频轨道（16kHz 单声道 mp3）
            tempAudio = java.io.File.createTempFile("video-audio-", ".mp3");
            videoAiManager.extractAudio(tempVideo, tempAudio.getAbsolutePath());
            ThrowUtils.throwIf(!tempAudio.exists() || tempAudio.length() == 0,
                    ErrorCode.OPERATION_ERROR, "视频音频提取失败");

            // 5. ASR 语音转文字（复用硅基流动 SenseVoiceSmall）
            String transcript = siliconFlowAsrService.transcribe(tempAudio, file.getName());

            // 6. LLM 生成摘要（复用第一阶段 AI 摘要服务）
            return llmApiService.generateVideoSummary(transcript, duration);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("视频 AI 摘要生成失败, fileId={}", file.getId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "视频 AI 摘要生成失败");
        } finally {
            // 清理临时文件（视频可能较大，必须及时删除）
            if (tempVideo != null && tempVideo.exists()) {
                tempVideo.delete();
            }
            if (tempAudio != null && tempAudio.exists()) {
                tempAudio.delete();
            }
            if (tempFrame != null && tempFrame.exists()) {
                tempFrame.delete();
            }
        }
    }

    /**
     * 获取文件文本内容（用于协同编辑加载初始内容 / 编辑页内容编辑 / 文本预览）
     * 纯文本格式直接读取；doc 用 Tika 提取文本（仅供预览，不可编辑）；其余格式不支持
     */
    @GetMapping("/content/{fileId}")
    public BaseResponse<String> getFileContent(@PathVariable Long fileId,
                                                HttpServletRequest request) {
        File file = fileService.getById(fileId);
        ThrowUtils.throwIf(file == null, ErrorCode.NOT_FOUND_ERROR, "文件不存在");

        // 手动校验空间查看权限（上下文解析不识别 path 变量，直接基于文件+空间对象校验，避免绕过）
        User loginUser = userService.getLoginUser(request);
        checkFilePermission(file, loginUser, SpaceUserPermissionConstant.PICTURE_VIEW, "无权限查看该文件");

        // 校验文件类型：纯文本格式可读可编辑，doc 仅可读预览（docx 等办公格式不支持内容读取）
        String format = file.getFormat() == null ? null : file.getFormat().toLowerCase();
        boolean isPlainText = format != null && EDITABLE_TEXT_FORMATS.contains(format);
        boolean isPreviewOnly = format != null && PREVIEW_ONLY_FORMATS.contains(format);
        ThrowUtils.throwIf(!isPlainText && !isPreviewOnly, ErrorCode.OPERATION_ERROR,
                "当前文件类型不支持读取内容，支持：txt、md、json、xml、csv、rtf、doc（仅预览）");

        try {
            // 从 URL 中提取 COS key
            String url = file.getUrl();
            String host = cosClientConfig.getHost();
            String key = url.startsWith(host) ? url.substring(host.length()) : url;

            // 从 COS 下载文件内容
            COSObject cosObject = cosManager.getObject(key);
            COSObjectInputStream cosInput = cosObject.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(cosInput);
            cosInput.close();

            if (isPreviewOnly) {
                // doc 等老格式：Tika 提取纯文本预览（保留换行，样式不保留）
                String content = documentTextExtractor.extractTextForPreview(bytes);
                ThrowUtils.throwIf(content == null, ErrorCode.OPERATION_ERROR, "文档解析失败，请下载后查看");
                return ResultUtils.success(content);
            }
            return ResultUtils.success(new String(bytes, StandardCharsets.UTF_8));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取文件内容失败, fileId = {}", fileId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取文件内容失败");
        }
    }

    /**
     * 保存文件文本内容（用于协同编辑保存 / 编辑页内容编辑）
     * 仅支持纯文本格式直接写回；docx、doc 等办公格式不提供内容编辑（避免样式丢失）
     */
    @PostMapping("/content/save")
    public BaseResponse<Boolean> saveFileContent(@RequestBody Map<String, Object> body,
                                                  HttpServletRequest request) {
        Object fileIdObj = body.get("fileId");
        ThrowUtils.throwIf(fileIdObj == null, ErrorCode.PARAMS_ERROR, "文件 id 不能为空");
        Long fileId = Long.valueOf(fileIdObj.toString());
        User loginUser = userService.getLoginUser(request);

        File file = fileService.getById(fileId);
        ThrowUtils.throwIf(file == null, ErrorCode.NOT_FOUND_ERROR, "文件不存在");

        // 手动校验空间编辑权限（请求体的 fileId 不在上下文字段中，直接基于文件+空间对象校验，避免绕过）
        checkFilePermission(file, loginUser, SpaceUserPermissionConstant.PICTURE_EDIT, "无权限编辑该文件");

        String textContent = (String) body.get("content");
        ThrowUtils.throwIf(textContent == null, ErrorCode.PARAMS_ERROR, "content 不能为空");

        // 校验文件类型：仅纯文本格式可保存（与读取接口的可编辑范围一致）
        String format = file.getFormat() == null ? null : file.getFormat().toLowerCase();
        boolean isPlainText = format != null && EDITABLE_TEXT_FORMATS.contains(format);
        ThrowUtils.throwIf(!isPlainText, ErrorCode.OPERATION_ERROR,
                "当前文件类型不支持保存文本内容，支持：txt、md、json、xml、csv、rtf");

        try {
            // 从 URL 中提取 COS key
            String url = file.getUrl();
            String host = cosClientConfig.getHost();
            String key = url.startsWith(host) ? url.substring(host.length()) : url;

            // 写入临时文件并上传到 COS
            byte[] bytes = textContent.getBytes(StandardCharsets.UTF_8);
            java.io.File tempFile = java.io.File.createTempFile("doc-save-", ".tmp");
            try {
                java.nio.file.Files.write(tempFile.toPath(), bytes);
                cosManager.putObject(key, tempFile);
            } finally {
                tempFile.delete();
            }

            return ResultUtils.success(true);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("保存文件内容失败, fileId = {}", fileId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存文件内容失败");
        }
    }

    /**
     * 音频文件转文字（ASR）
     * 支持 mp3、wav、flac、m4a、ogg、wma、aac 格式
     */
    @PostMapping("/audio/transcribe")
    public BaseResponse<String> transcribeAudio(@RequestBody Map<String, Object> body,
                                                 HttpServletRequest request) {
        Object fileIdObj = body.get("fileId");
        ThrowUtils.throwIf(fileIdObj == null, ErrorCode.PARAMS_ERROR, "文件 id 不能为空");
        Long fileId = Long.valueOf(fileIdObj.toString());
        User loginUser = userService.getLoginUser(request);

        File file = fileService.getById(fileId);
        ThrowUtils.throwIf(file == null, ErrorCode.NOT_FOUND_ERROR, "文件不存在");

        // 手动校验空间查看权限（请求体的 fileId 不在上下文字段中，直接基于文件+空间对象校验，避免绕过）
        checkFilePermission(file, loginUser, SpaceUserPermissionConstant.PICTURE_VIEW, "无权限访问该文件");

        // 校验音频格式
        String format = file.getFormat();
        if (!siliconFlowAsrService.isSupportedFormat(format)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "当前文件类型不支持语音转文字，支持的格式：mp3、wav、flac、m4a、ogg、wma、aac");
        }

        java.io.File tempAudioFile = null;
        try {
            // 从 URL 中提取 COS key
            String url = file.getUrl();
            String host = cosClientConfig.getHost();
            String key = url.startsWith(host) ? url.substring(host.length()) : url;

            // 从 COS 下载音频文件
            COSObject cosObject = cosManager.getObject(key);
            COSObjectInputStream cosInput = cosObject.getObjectContent();
            byte[] audioBytes = IOUtils.toByteArray(cosInput);
            cosInput.close();

            // 写入临时文件（保留原始后缀）
            tempAudioFile = java.io.File.createTempFile("asr-", "." + format.toLowerCase());
            java.nio.file.Files.write(tempAudioFile.toPath(), audioBytes);

            // 调用硅基流动 ASR 转写
            String transcribedText = siliconFlowAsrService.transcribe(tempAudioFile, file.getName());

            // 将转写内容存入文件表的摘要字段（支持后续 LIKE 检索）
            file.setSummary(transcribedText);
            boolean updated = fileService.updateById(file);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "保存转写内容失败");

            return ResultUtils.success(transcribedText);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("音频转文字失败, fileId={}", fileId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "音频转文字失败");
        } finally {
            // 删除临时文件
            if (tempAudioFile != null) {
                tempAudioFile.delete();
            }
        }
    }

    /**
     * 校验用户对文件的空间权限（私有/团队/社区空间均适用，公共空间文件直接放行）
     */
    private void checkFilePermission(File file, User loginUser, String permission, String errorMessage) {
        Long spaceId = file.getSpaceId();
        if (spaceId == null) {
            return;
        }
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        List<String> permissionList = spaceUserAuthManager.getPermissionList(file, space, loginUser);
        ThrowUtils.throwIf(!permissionList.contains(permission), ErrorCode.NO_AUTH_ERROR, errorMessage);
    }


}
