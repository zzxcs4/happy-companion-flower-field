package com.zzx.yunpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.zzx.yunpicturebackend.config.CosClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
@Slf4j
@Component
public class CosManager {  
  
    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 获取 COS 客户端配置
     */
    public CosClientConfig getCosClientConfig() {
        return cosClientConfig;
    }
  
    @Resource  
    private COSClient cosClient;
    private static final String UPLOAD_ID_PREFIX = "cos:upload:";

    private static final long UPLOAD_ID_EXPIRE_SECONDS = 86400; // 24小时


    @Resource
    private RedisTemplate<String, String> redisTemplate;
  
    // ... 一些操作 COS 的方法
    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);

        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传对象（附带图片信息）
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 图片压缩（转成 webp 格式）
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setFileId(webpKey);
        rules.add(compressRule);
        // 缩略图处理，仅对 > 20 KB 的图片生成缩略图
        if (file.length() > 2 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            // 缩放规则 /thumbnail/<Width>x<Height>>（如果大于原图宽高，则不处理）
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 300, 300));
            rules.add(thumbnailRule);
        }
        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }


    /**
     * 删除对象
     *
     * @param key 文件 key
     */
    public void deleteObject(String key) throws CosClientException {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

    /**
     * 将对象设置为私有读（仅视频等需防下载的媒体使用）
     *
     * @param key 文件 key（与上传时一致）
     */
    public void setObjectPrivate(String key) {
        try {
            cosClient.setObjectAcl(cosClientConfig.getBucket(), key, CannedAccessControlList.Private);
        } catch (CosClientException e) {
            log.warn("设置对象私有权限失败, key={}, 原因={}", key, e.getMessage());
        }
    }

    /**
     * 生成对象临时签名 URL（私有对象可通过签名 URL 访问，有效期后失效）
     *
     * @param key          对象 key（如 /space/xxx/xxx.mp4）
     * @param expireSeconds 有效期（秒）
     * @return 带签名的可访问 URL
     */
    public String generatePresignedUrl(String key, long expireSeconds) {
        Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000);
        URL url = cosClient.generatePresignedUrl(cosClientConfig.getBucket(), key, expiration);
        return url.toString();
    }

    /**
     * 将本站 COS 完整 URL 转为临时签名 URL（仅对本 COS 域名生效，其他 URL 原样返回）。
     * 用于私有读对象（视频）返回给前端播放；图片等公有对象不受影响，原样返回。
     *
     * @param url           完整 COS URL（如 https://host//space/xxx/xxx.mp4）
     * @param expireSeconds 有效期（秒）
     * @return 签名 URL 或原 URL
     */
    public String signCosUrl(String url, long expireSeconds) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        String host = cosClientConfig.getHost();
        if (!url.startsWith(host)) {
            return url;
        }
        // 剥离 query 参数（防止对已签名 URL 二次签名时把 query 误当作 key 的一部分）
        int qIdx = url.indexOf('?');
        String path = qIdx >= 0 ? url.substring(0, qIdx) : url;
        // 提取 key：去掉 host 前缀后，将前导斜杠压缩为单个（与上传 key 格式一致）
        String key = path.substring(host.length());
        key = "/" + key.replaceAll("^/+", "");
        return generatePresignedUrl(key, expireSeconds);
    }
    /**
     * 判断对象是否存在
     *
     * @param bucketName 存储桶名称
     * @param key        文件 key
     * @return
     */
    public boolean doesObjectExist(String bucketName, String key)
            throws CosClientException, CosServiceException {

            return cosClient.doesObjectExist(bucketName, key);

    }

    /**
     *
     * 初始化分片上传(使用redis保证幂等性)
     * @param key
     * @return
     * @throws CosClientException
     * @throws CosServiceException
     */
    public String initiateMultipartUpload(String key) throws CosClientException, CosServiceException{

        /**
         * 幂等的初始化分片上传
         *
         * @param bucketName 存储桶名称
         * @param key 对象键
         * @param objectMetadata 对象元数据（仅在首次创建时生效）
         * @return uploadId
         */
        // 生成幂等键
        String idempotentKey = generateIdempotentKey(cosClientConfig.getBucket(), key);

        // 1. 先检查Redis中是否已有uploadId
        String cachedUploadId = redisTemplate.opsForValue().get(idempotentKey);
        if (cachedUploadId != null && !cachedUploadId.isEmpty()) {
            // 验证uploadId是否仍然有效
            if (isUploadIdValid(cosClientConfig.getBucket(), key, cachedUploadId)) {
                log.info("Reusing existing uploadId: {} for bucket: {}, key: {}",
                        cachedUploadId, cosClientConfig.getBucket(), key);
                return cachedUploadId;
            } else {
                // uploadId已失效，删除缓存
                redisTemplate.delete(idempotentKey);
                log.warn("UploadId {} is invalid, removed from cache", cachedUploadId);
            }
        }

        // 2. 加分布式锁，防止并发初始化


        // 3. 首次调用，创建新的uploadId
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(cosClientConfig.getBucket(), key);


        log.info("Creating new multipart upload for bucket: {}, key: {}", cosClientConfig.getBucket(), key);
        InitiateMultipartUploadResult result = cosClient.initiateMultipartUpload(request);
        String uploadId = result.getUploadId();

        // 4. 缓存uploadId
        redisTemplate.opsForValue().set(idempotentKey, uploadId,
                UPLOAD_ID_EXPIRE_SECONDS, TimeUnit.SECONDS);

        log.info("Created new uploadId: {} for bucket: {}, key: {}", uploadId, cosClientConfig.getBucket(), key);
        return uploadId;

    }
// 分块上传的过程中，仅能通过初始化分块指定文件上传之后的 metadata
// 需要的头部可以在这里指定
//        ObjectMetadata objectMetadata = new ObjectMetadata();
//        request.setObjectMetadata(objectMetadata);

    /**
     * 列出上传的分块信息
     * @param key
     * @param uploadId
     * @return
     * @throws CosClientException
     * @throws CosServiceException
     */

    public PartListing listParts(String key,String uploadId)
            throws CosClientException, CosServiceException{

        ListPartsRequest listPartsRequest = new ListPartsRequest(cosClientConfig.getBucket(), key, uploadId);
        return cosClient.listParts(listPartsRequest);
    }

    /**
     * 上传分块
     * @param key
     * @param uploadId
     * @param i
     * @param file
     * @return
     * @throws CosClientException
     * @throws CosServiceException
     */
    public UploadPartResult uploadPart(String key, String uploadId, int partNumber, File file, long offset, long partSize) throws CosClientException, CosServiceException{
        UploadPartRequest uploadPartRequest = new UploadPartRequest();
        uploadPartRequest.setBucketName(cosClientConfig.getBucket());
        uploadPartRequest.setKey(key);
        uploadPartRequest.setUploadId(uploadId);
        uploadPartRequest.setFile(file);
        uploadPartRequest.setPartNumber(partNumber);
        uploadPartRequest.setFileOffset(offset);
        uploadPartRequest.setPartSize(partSize);
        return cosClient.uploadPart(uploadPartRequest);

    }
    /**
     * 完成分片上传
     * @param key
     * @param uploadId
     * @param partETags
     * @return
     * @throws CosClientException
     * @throws CosServiceException
     */
    public String completeMultipartUpload(String key, String uploadId,List<PartETag> partETags) throws CosClientException, CosServiceException{
        // 保存已上传的分片信息，实际情况里，这里的内容是从上传分块接口中获取到的


// 分片上传结束后，调用 complete 完成分片上传
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(cosClientConfig.getBucket(), key, uploadId, partETags);
        cosClient.completeMultipartUpload(completeMultipartUploadRequest);
            return cosClientConfig.getHost()+"/" + key;



    }
/**
 * 验证uploadId是否仍然有效
 */
private boolean isUploadIdValid(String bucketName, String key, String uploadId) {
    try {
        // 尝试列出已上传的分块，如果抛出异常说明uploadId无效
        listParts(key, uploadId);
        return true;
    } catch (Exception e) {
        log.warn("UploadId validation failed for bucket: {}, key: {}, uploadId: {}, error: {}",
                bucketName, key, uploadId, e.getMessage());
        return false;
    }
}

/**
 * 生成幂等键
 */
private String generateIdempotentKey(String bucketName, String key) {
    return UPLOAD_ID_PREFIX + bucketName + ":" + key;
}








}
