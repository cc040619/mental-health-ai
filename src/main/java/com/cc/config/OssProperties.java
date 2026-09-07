package com.cc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置属性，对应 application.yml 中的 aliyun.oss 前缀
 *
 * @author cc
 * @date 2026-09-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    /** OSS Endpoint，如 https://oss-cn-beijing.aliyuncs.com */
    private String endpoint;

    /** Bucket 名称 */
    private String bucketName;

    /** Bucket 所在地域，如 cn-beijing */
    private String region;

    /** 访问密钥 ID */
    private String accessKeyId;

    /** 访问密钥 Secret */
    private String accessKeySecret;
}
