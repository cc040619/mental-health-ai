package com.cc.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 客户端配置，基于 OssProperties 构建全局 OSS 客户端
 *
 * @author cc
 * @date 2026-09-06
 */
@Configuration
public class OssConfig {

    @Resource
    private OssProperties ossProperties;

    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );
    }
}
