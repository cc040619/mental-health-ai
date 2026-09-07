package com.cc.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.aliyun.oss.OSS;
import com.cc.config.OssProperties;
import com.cc.dto.response.FileInfoDTO;
import com.cc.entity.SysFileInfo;
import com.cc.exception.BusinessException;
import com.cc.common.ResultCode;
import com.cc.mapper.SysFileInfoMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 文件业务层：校验上传文件、写入阿里云 OSS 并落库 sys_file_info
 *
 * @author cc
 * @date 2026-09-06
 */
@Slf4j
@Service
public class FileService {

    /** 允许上传的图片与常见文档类型 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "pdf", "txt", "doc", "docx", "xls", "xlsx");

    /** 最大文件大小：10MB */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    /** 扩展名与文件类型标识的映射 */
    private static final Map<String, String> FILE_TYPE_MAP = Map.ofEntries(
            Map.entry("jpg", "IMG"), Map.entry("jpeg", "IMG"), Map.entry("png", "IMG"),
            Map.entry("gif", "IMG"), Map.entry("webp", "IMG"), Map.entry("bmp", "IMG"),
            Map.entry("pdf", "PDF"), Map.entry("txt", "TXT"),
            Map.entry("doc", "DOC"), Map.entry("docx", "DOC"),
            Map.entry("xls", "XLS"), Map.entry("xlsx", "XLS")
    );

    @Resource
    private OSS ossClient;

    @Resource
    private OssProperties ossProperties;

    @Resource
    private SysFileInfoMapper sysFileInfoMapper;

    /**
     * 上传文件到阿里云 OSS 并记录文件信息
     *
     * @param file          上传的文件
     * @param businessType  业务类型（如 ARTICLE）
     * @param businessId    业务对象ID
     * @param businessField 业务字段名（如 cover）
     * @param uploadUserId  上传用户ID
     * @return 文件信息（filePath 为 OSS 完整访问URL）
     */
    public FileInfoDTO upload(MultipartFile file, String businessType, String businessId,
                              String businessField, Long uploadUserId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND.getCode(), "上传文件不能为空", null);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEEDED.getCode(), ResultCode.FILE_SIZE_EXCEEDED.getMsg(), null);
        }

        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED.getCode(), ResultCode.FILE_TYPE_NOT_SUPPORTED.getMsg(), null);
        }

        // OSS 对象Key：bussiness/{业务类型}/{yyyyMM}/{uuid}.{ext}
        String objectKey = String.format("bussiness/%s/%s/%s.%s",
                sanitizePathSegment(businessType), DateUtil.format(LocalDateTime.now(), "yyyyMM"),
                IdUtil.simpleUUID(), extension);

        // 上传到 OSS
        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(ossProperties.getBucketName(), objectKey, inputStream);
        } catch (IOException e) {
            log.error("文件上传 OSS 失败, originalName={}", originalName, e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED.getCode(), ResultCode.FILE_UPLOAD_FAILED.getMsg(), null);
        }

        // 构造公共读访问URL：https://{bucket}.{endpoint-host}/{objectKey}
        String endpointHost = ossProperties.getEndpoint().replaceFirst("^https?://", "");
        String url = String.format("https://%s.%s/%s", ossProperties.getBucketName(), endpointHost, objectKey);

        // 落库文件信息
        SysFileInfo fileInfo = SysFileInfo.builder()
                .originalName(originalName)
                .filePath(url)
                .fileSize(file.getSize())
                .fileType(FILE_TYPE_MAP.getOrDefault(extension, "OTHER"))
                .businessType(sanitizePathSegment(businessType))
                .businessId(businessId)
                .businessField(businessField)
                .uploadUserId(uploadUserId)
                .isTemp(0)
                .status(1)
                .createTime(LocalDateTime.now())
                .build();
        sysFileInfoMapper.insert(fileInfo);

        return FileInfoDTO.builder()
                .id(fileInfo.getId())
                .originalName(originalName)
                .filePath(url)
                .fileSize(file.getSize())
                .fileType(fileInfo.getFileType())
                .build();
    }

    /**
     * 提取文件扩展名（小写），文件名不合法时抛出业务异常
     */
    private String extractExtension(String originalName) {
        if (!StringUtils.hasText(originalName) || !originalName.contains(".")) {
            throw new BusinessException(ResultCode.FILE_NAME_INVALID.getCode(), ResultCode.FILE_NAME_INVALID.getMsg(), null);
        }
        String extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(extension)) {
            throw new BusinessException(ResultCode.FILE_NAME_INVALID.getCode(), ResultCode.FILE_NAME_INVALID.getMsg(), null);
        }
        return extension;
    }

    /**
     * 清理路径片段，防止通过业务类型构造非法 OSS Key
     */
    private String sanitizePathSegment(String segment) {
        if (!StringUtils.hasText(segment)) {
            return "COMMON";
        }
        String cleaned = segment.replaceAll("[^a-zA-Z0-9_-]", "");
        return StringUtils.hasText(cleaned) ? cleaned : "COMMON";
    }
}
