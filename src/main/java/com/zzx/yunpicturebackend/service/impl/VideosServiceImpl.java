package com.zzx.yunpicturebackend.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.cos.model.*;
import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.common.ResultUtils;
import com.zzx.yunpicturebackend.exception.BusinessException;
import com.zzx.yunpicturebackend.exception.ErrorCode;
import com.zzx.yunpicturebackend.exception.ThrowUtils;
import com.zzx.yunpicturebackend.config.CosClientConfig;
import com.zzx.yunpicturebackend.manager.CosManager;
import com.zzx.yunpicturebackend.manager.SpaceCapacityManager;
import com.zzx.yunpicturebackend.manager.VideoThumbnailManager;
import com.zzx.yunpicturebackend.mapper.FileMapper;
import com.zzx.yunpicturebackend.model.dto.picture.PictureEditRequest;
import com.zzx.yunpicturebackend.model.entity.Space;
import com.zzx.yunpicturebackend.model.entity.User;
import com.zzx.yunpicturebackend.model.enums.PictureReviewStatusEnum;
import com.zzx.yunpicturebackend.model.vo.VideoVO;
import com.zzx.yunpicturebackend.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* @author zzx
* @description 针对表【videos(视频信息表)】的数据库操作Service实现
* @createDate 2026-04-04 12:03:16
*/
@Slf4j
@Service
public class VideosServiceImpl extends ServiceImpl<FileMapper, com.zzx.yunpicturebackend.model.entity.File>
    implements VideosService {
    // 定义常量
    private static final long PART_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_PARTS = 10000; // COS最大分块数限制
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private CosManager cosManager;
    @Resource
    private VideoThumbnailManager videoThumbnailManager;
    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private SpaceCapacityManager spaceCapacityManager;
    @Resource
    private VideoTranscodeService videoTranscodeService;
    @Resource
    private AudioCompressService audioCompressService;

    /**
     * 文件上传前检查文件
     * 返回当前以上传到哪个分块
     * @param fileMd5
     * @return
     */
    @Override
    public BaseResponse<Boolean> checkFile(String fileMd5) {
        // 1. 先查询数据库
        com.zzx.yunpicturebackend.model.entity.File file = getById(fileMd5);

        if (file != null) {
            String fileUrl = file.getUrl(); // 假设 getFilePath 返回的是完整的 URL

            try {
                // 2. 解析 URL 获取 Bucket 和 Key
                // 腾讯云 URL 格式示例: https://<Bucket>.cos.<Region>.myqcloud.com/<Key>
                String[] bucketAndKey = parseBucketAndKey(fileUrl);

                String bucketName = bucketAndKey[0];
                String key = bucketAndKey[1];

                // 3. 判断文件是否存在
                boolean doesObjectExist = cosManager.doesObjectExist(bucketName, key);

                // 如果没抛异常且获取到结果，说明文件存在
                if (doesObjectExist) {
                    return ResultUtils.success(true); // 文件已存在
                }

            } catch (Exception e) {
                log.error("腾讯云COS系统异常", e);
                throw new BusinessException(ErrorCode.OUTSIDE_SYSTEM_ERROR,"腾讯云COS系统异常");
            }
        }

        // 文件不存在（数据库无记录 或 COS 中无文件）
        return ResultUtils.success(false);
    }

    /**
     * 解析腾讯云 COS URL
     * 输入: https://example-123456.cos.ap-beijing.myqcloud.com/group1/M00/00/01/test.mp4
     * 输出: ["example-123456", "group1/M00/00/01/test.mp4"]
     */
    private   String[] parseBucketAndKey(String cosUrl) {
        if (cosUrl == null || cosUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("COS URL cannot be null or empty");
        }

        try {
            URI uri = new URI(cosUrl);
            String host = uri.getHost();
            String path = uri.getPath();

            if (host == null || path == null) {
                throw new IllegalArgumentException("Invalid COS URL format");
            }

            String bucketName;
            String key;

            // 方式1: 虚拟主机风格 (bucket-name.cos.region.myqcloud.com/path/to/file)
            // 例如: https://mybucket-1250000000.cos.ap-guangzhou.myqcloud.com/images/photo.jpg
            Pattern virtualHostedPattern = Pattern.compile("^([^.]+)\\.cos\\.[^.]+\\.[^.]+$");
            Matcher virtualMatcher = virtualHostedPattern.matcher(host);

            if (virtualMatcher.matches()) {
                bucketName = virtualMatcher.group(1);
                key = path.startsWith("/") ? path.substring(1) : path;
            } else {
                // 方式2: 路径风格 (cos.region.myqcloud.com/bucket-name/path/to/file)
                // 例如: https://cos.ap-guangzhou.myqcloud.com/mybucket-1250000000/images/photo.jpg
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                int firstSlashIndex = path.indexOf("/");
                if (firstSlashIndex > 0) {
                    bucketName = path.substring(0, firstSlashIndex);
                    key = path.substring(firstSlashIndex + 1);
                } else {
                    throw new IllegalArgumentException("Cannot extract bucket name from path: " + path);
                }
            }

            // URL解码key（处理特殊字符如空格、中文等）
            if (key != null && !key.isEmpty()) {
                key = java.net.URLDecoder.decode(key, "UTF-8");
            }

            return new String[]{bucketName, key};

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid COS URL format: " + cosUrl, e);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("URL decoding failed", e);
        }
    }

    @Override
    public BaseResponse<Boolean> checkChunk(String fileMd5, int chunk) {
        return null;
    }



    /**
     * 分块上传（幂等，支持断点续传）
     * @param file 本地待上传文件
     * @param spaceId 空间 id
     * @param md5 文件 MD5
     * @param videoId 文件 id（非空表示更新）
     * @param request 请求
     * @return 上传结果
     */
    @Override
    public BaseResponse<VideoVO> IdempotentMultipartUploader(File file, Long spaceId, String md5, Long videoId,HttpServletRequest request) {
        // 花田空间容量检查（满时通知管理员并给予300MB宽限，宽限用完后禁止上传）
        if (spaceId != null) {
            Space capacitySpace = spaceService.getById(spaceId);
            if (capacitySpace != null && capacitySpace.getSpaceType() == 2) {
                spaceCapacityManager.checkFieldSpaceCapacity(capacitySpace);
            }
        }
        //设定key
        // 按照用户 id 划分目录 => 按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%s", userService.getLoginUser(request).getId());
        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        // 2. 图片上传地址
        String uploadFilename = String.format("%s_%s.%s", file.getName(), md5,
                FileUtil.getSuffix(file.getName()));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

        long fileSize = file.length();
        int totalParts = calculateTotalParts(fileSize);

        //初始化分片上传
        String uploadId = cosManager.initiateMultipartUpload(uploadPath);

        //获取已上传分片
        PartListing partListing = cosManager.listParts(uploadPath, uploadId);
        Integer nextPartNumberMarker = partListing.getNextPartNumberMarker();

        if (nextPartNumberMarker == null){
            nextPartNumberMarker = 1;
        }

        List<PartETag> partETags = new ArrayList<>();

        for(int i = nextPartNumberMarker; i <= totalParts; i++){
            long offset = (i - 1) * PART_SIZE;
            long partSize = Math.min(PART_SIZE, fileSize - offset);

            UploadPartResult uploadPartResult = cosManager.uploadPart(uploadPath, uploadId, i, file, offset, partSize);
            PartETag partETag = uploadPartResult.getPartETag();
            partETags.add(partETag);

            log.info("Uploaded part {}/{} for file: {}", i, totalParts, uploadFilename);
        }

        //完成上传
        partETags.sort(Comparator.comparingInt(PartETag::getPartNumber));
        String url = cosManager.completeMultipartUpload(uploadPath, uploadId, partETags);

        // 生成视频缩略图
        String thumbnailUrl = null;
        if (videoThumbnailManager.isAvailable()) {
            try {
                thumbnailUrl = generateAndUploadThumbnail(file, uploadPathPrefix, md5);
            } catch (Exception e) {
                log.error("Failed to generate thumbnail for video: {}", uploadFilename, e);
            }
        } else {
            log.warn("FFmpeg not available, skipping thumbnail generation for: {}", uploadFilename);
        }

        //构造返回对象
        Space space = spaceService.getById(spaceId);
        com.zzx.yunpicturebackend.model.entity.File file1 = new com.zzx.yunpicturebackend.model.entity.File();
        file1.setUrl(url);
        file1.setThumbnailUrl(thumbnailUrl);
        file1.setName(uploadFilename);
        file1.setSize(fileSize);
        file1.setFormat(FileUtil.getSuffix(file.getName()));
        file1.setUserId(userService.getLoginUser(request).getId());
        file1.setSpaceId(spaceId);
        //视频时长怎么获取？
        //file1.setDuration();
        // 补充审核参数
        fillReviewParams(spaceId, file1, userService.getLoginUser(request));
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (videoId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            file1.setId(videoId);
            file1.setEditTime(new Date());
        }
        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(file1);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            if( space != null&&(space.getSpaceType()==0||space.getSpaceType()==1)){
                if (finalSpaceId != null) {
                    boolean update = spaceService.lambdaUpdate()
                            .eq(Space::getId, finalSpaceId)
                            .setSql("totalSize = totalSize + " + file1.getSize())
                            .setSql("totalCount = totalCount + 1")
                            .update();
                    ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
                }}
            return file1;
        });
        // 视频上传成功后异步转码为 H.264 MP4（解决浏览器无法播放非 mp4 格式的问题，失败不影响上传结果）
        if (videoTranscodeService.isVideoFormat(file1.getFormat())) {
            videoTranscodeService.transcodeAsync(file1.getId());
        }
        if (audioCompressService.isAudioFormat(file1.getFormat())){
            audioCompressService.compressAsync(file1.getId());
        }
        return ResultUtils.success(VideoVO.objToVo(file1));
    }

    /**
     * 编辑文件
     * @param pictureEditRequest
     * @param loginUser
     */
    @Override
    public void editFile(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        com.zzx.yunpicturebackend.model.entity.File file = new com.zzx.yunpicturebackend.model.entity.File();
        BeanUtils.copyProperties(pictureEditRequest, file);
        // 注意将 list 转为 string
        file.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        file.setEditTime(new Date());
        // 数据校验
        this.validFile(file);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        com.zzx.yunpicturebackend.model.entity.File oldFile = this.getById(id);
        ThrowUtils.throwIf(oldFile == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        //checkPictureAuth(loginUser, oldPicture);
        // 补充审核参数
        //this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(file);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }
    @Override
    public void fillReviewParams(Long spaceId, com.zzx.yunpicturebackend.model.entity.File file, User loginUser) {
        Space space = spaceService.getById(spaceId);
        if (userService.isAdmin(loginUser)|| (space!=null && space.getUserId().equals(loginUser.getId()))) {
            // 管理员或空间创建者自动过审
            file.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            file.setReviewerId(loginUser.getId());
            file.setReviewMessage("管理员或空间创建者自动过审");
            file.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            file.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }
    @Override
    public void validFile(com.zzx.yunpicturebackend.model.entity.File file) {
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = file.getId();
        String url = file.getUrl();
        String introduction = file.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }
    /**
     * 计算分块数量（正确实现）
     */
    private int calculateTotalParts(long fileSize) {
        if (fileSize == 0) {
            return 1; // 空文件也算一个分块
        }
        // 正确公式：ceil(文件大小 / 分块大小)
        return (int) Math.ceil((double) fileSize / PART_SIZE);
    }

    /**
     * 生成视频缩略图并上传到 COS
     * @param videoFile 视频文件
     * @param uploadPathPrefix 上传路径前缀
     * @param md5 文件MD5
     * @return 缩略图URL
     */
    private String generateAndUploadThumbnail(File videoFile, String uploadPathPrefix, String md5) throws Exception {
        // 1. 生成临时缩略图文件路径
        String thumbnailFileName = String.format("%s_thumbnail_%s.jpg", FileUtil.mainName(videoFile.getName()), md5);
        String tempThumbnailPath = System.getProperty("java.io.tmpdir") + File.separator + thumbnailFileName;

        try {
            // 2. 使用 FFmpeg 生成缩略图
            File thumbnailFile = videoThumbnailManager.generateThumbnail(videoFile, tempThumbnailPath);

            // 3. 上传缩略图到 COS
            String thumbnailUploadPath = String.format("/%s/thumbnails/%s", uploadPathPrefix, thumbnailFileName);
            cosManager.putObject(thumbnailUploadPath, thumbnailFile);
            String thumbnailUrl = cosClientConfig.getHost() + thumbnailUploadPath;

            // 4. 删除临时缩略图文件
            FileUtil.del(thumbnailFile);

            log.info("Successfully generated and uploaded thumbnail: {}", thumbnailUrl);
            return thumbnailUrl;
        } catch (Exception e) {
            // 清理临时文件
            FileUtil.del(tempThumbnailPath);
            throw e;
        }
    }


}




