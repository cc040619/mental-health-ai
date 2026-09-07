package com.cc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传响应 DTO，前端使用 filePath 属性作为封面图片地址
 *
 * @author cc
 * @date 2026-09-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoDTO {

    // 文件ID
    private Long id;

    // 原始文件名
    private String originalName;

    // 文件访问路径（OSS 完整URL）
    private String filePath;

    // 文件大小（字节）
    private Long fileSize;

    // 文件类型（IMG/PDF/TXT等）
    private String fileType;
}
