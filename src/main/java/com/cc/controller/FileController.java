package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.response.FileInfoDTO;
import com.cc.service.FileService;
import com.cc.util.CurrentUserUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件管理控制器
 *
 * @author cc
 * @date 2026-09-06
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Resource
    private FileService fileService;

    // 文件上传（multipart/form-data：file + businessType + businessId + businessField）
    @PostMapping("/upload")
    public Result<FileInfoDTO> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "businessType", required = false) String businessType,
                                      @RequestParam(value = "businessId", required = false) String businessId,
                                      @RequestParam(value = "businessField", required = false) String businessField) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        FileInfoDTO fileInfo = fileService.upload(file, businessType, businessId, businessField, userId);
        return Result.ok(fileInfo);
    }
}
