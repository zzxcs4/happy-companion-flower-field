package com.zzx.yunpicturebackend.controller;

import com.zzx.yunpicturebackend.common.BaseResponse;
import com.zzx.yunpicturebackend.model.vo.VideoVO;
import com.zzx.yunpicturebackend.service.VideosService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
@RestController
@Slf4j
@RequestMapping("/BigField")
public class BigFieldController {
    @Resource
    private VideosService videosService;
    @ApiOperation(value = "文件上传前检查文件")
    @PostMapping("/upload/checkfile")
    public BaseResponse<Boolean> checkfile(
            @RequestParam("fileMd5") String fileMd5
    ) throws Exception {
        BaseResponse<Boolean> booleanRestResponse = videosService.checkFile(fileMd5);
        return booleanRestResponse;
    }


    @ApiOperation(value = "分块文件上传前的检测")
    @PostMapping("/upload/checkchunk")
    public BaseResponse<Boolean> checkchunk(@RequestParam("fileMd5") String fileMd5,
                                            @RequestParam("chunk") int chunk) throws Exception {
        BaseResponse<Boolean> booleanRestResponse = videosService.checkChunk(fileMd5,chunk);
        return booleanRestResponse;
    }
    @ApiOperation(value = "上传大文件(后端处理分块)")
    @PostMapping("/upload/multipart")
    public BaseResponse<VideoVO> MultipartUploader(@RequestParam("file") MultipartFile file,  // 改为 MultipartFile
                                                   @RequestParam(value = "spaceId", required = false) Long spaceId,
                                                   @RequestParam("md5") String md5,
                                                   HttpServletRequest request) throws Exception {


            // 获取文件原始名称
            String originalFilename = file.getOriginalFilename();
            String suffix = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 创建临时文件
            File tempFile = File.createTempFile("upload_", suffix);

            // 写入数据
        file.transferTo(tempFile);



        try {
            return videosService.IdempotentMultipartUploader(tempFile, spaceId, md5, null,request);
        } finally {
            // 清理临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
