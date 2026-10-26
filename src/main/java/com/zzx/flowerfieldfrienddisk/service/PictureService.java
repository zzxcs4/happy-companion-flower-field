package com.zzx.flowerfieldfrienddisk.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.flowerfieldfrienddisk.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.zzx.flowerfieldfrienddisk.model.dto.picture.*;
import com.zzx.flowerfieldfrienddisk.model.entity.File;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.flowerfieldfrienddisk.model.entity.User;
import com.zzx.flowerfieldfrienddisk.model.vo.PictureVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author zzx
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-09-16 11:15:43
*/
public interface PictureService extends IService<File> {
    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    QueryWrapper<File> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    PictureVO getPictureVO(File file, HttpServletRequest request);

    Page<PictureVO> getPictureVOPage(Page<File> picturePage, HttpServletRequest request);

    void validPicture(File file);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(File file, User loginUser);

    void fillReviewParams(Long spaceId, File file, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

    void deletePicture(long pictureId, User loginUser);

    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    //void checkPictureAuth(User loginUser, Picture picture);

    @Async
    void clearPictureFile(File oldFile);

    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    @Transactional(rollbackFor = Exception.class)
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);
}
