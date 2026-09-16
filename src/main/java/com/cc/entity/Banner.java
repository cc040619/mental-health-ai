package com.cc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 首页轮播图实体，对应 banner 表
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@TableName("banner")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Banner {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    // 图片地址（OSS 上传产物）
    @TableField("image_url")
    private String imageUrl;

    // 跳转链接（仅 http/https，拒绝 javascript: 等伪协议）
    @TableField("link_url")
    private String linkUrl;

    // 排序（小在前）
    @TableField("sort_order")
    private Integer sortOrder;

    // 0-停用 1-启用
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
