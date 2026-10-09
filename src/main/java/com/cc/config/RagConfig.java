package com.cc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 检索配置，对应 application.yml 中的 rag 前缀
 *
 * @author cc
 * @date 2026-09-27
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class RagConfig {

    /** 向量索引持久化文件路径（相对进程工作目录）；文件不存在时向量检索自动降级为关键词检索 */
    private String indexFile = "./data/rag-index.json";
}