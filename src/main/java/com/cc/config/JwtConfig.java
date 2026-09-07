package com.cc.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * JWT 相关配置类，读取 application.yml 中以 jwt 为前缀的配置项
 *
 * @author cc
 * @date 2026-09-01
 */
@Data
@Component
@ConfigurationProperties(prefix="jwt")
public class JwtConfig {
    private String secret;
    private long expiration;
    private long refreshExpiration;
    private String header;
    private String tokenPrefix;
}
