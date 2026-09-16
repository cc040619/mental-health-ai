package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.OperationLogResponseDTO;
import com.cc.entity.AdminOperationLog;
import com.cc.mapper.AdminOperationLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员操作日志业务层：@Async 异步落库（复用 AsyncConfig 线程池，不阻塞业务）+ 列表查询
 *
 * <p>硬约束：记录失败仅告警，绝不影响业务接口；内容长度截断 1000 字；
 * 敏感参数已在 AOP 层脱敏，此处不做二次处理。
 * IP 由调用方在请求线程解析后传入（ThreadLocal 不跨线程传递到 @Async）。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Slf4j
@Service
public class OperationLogService {

    /** 内容截断上限（与表字段 varchar(1000) 对齐） */
    private static final int CONTENT_MAX_LENGTH = 1000;

    /** 列表分页上限 */
    private static final long MAX_PAGE_SIZE = 20L;

    @Resource
    private AdminOperationLogMapper adminOperationLogMapper;

    /**
     * 异步记录操作日志：登录/登出由 UserController 显式调用，后台写操作由 AOP 调用；
     * ip 须由调用方在请求线程通过 {@code CurrentUserUtils.getCurrentClientIp()} 解析后传入
     */
    @Async
    public void record(Long operatorId, String operatorName, String actionType,
                       String actionContent, String ip) {
        try {
            adminOperationLogMapper.insert(AdminOperationLog.builder()
                    .operatorId(operatorId)
                    .operatorName(operatorName)
                    .actionType(actionType)
                    .actionContent(truncate(actionContent))
                    .ip(ip)
                    .build());
        } catch (Exception e) {
            log.warn("操作日志写入失败: actionType={}, {}", actionType, e.getMessage());
        }
    }

    /**
     * 日志分页列表：操作人 / 类型 / 内容关键字 / 时间范围筛选（命中 idx_created）
     */
    public Map<String, Object> page(String operator, String actionType, String keyword,
                                    String startDate, String endDate, Long page, Long size) {
        long p = page == null || page < 1 ? 1 : page;
        long s = size == null || size < 1 ? 10 : Math.min(size, MAX_PAGE_SIZE);
        LambdaQueryWrapper<AdminOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(operator)) {
            wrapper.like(AdminOperationLog::getOperatorName, operator.trim());
        }
        if (StringUtils.hasText(actionType)) {
            wrapper.eq(AdminOperationLog::getActionType, actionType.trim());
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(AdminOperationLog::getActionContent, keyword.trim());
        }
        try {
            if (StringUtils.hasText(startDate)) {
                wrapper.ge(AdminOperationLog::getCreatedAt,
                        LocalDate.parse(startDate).atStartOfDay());
            }
            if (StringUtils.hasText(endDate)) {
                wrapper.le(AdminOperationLog::getCreatedAt,
                        LocalDate.parse(endDate).atTime(LocalTime.MAX));
            }
        } catch (Exception e) {
            throw new com.cc.exception.BusinessException("时间格式非法，应为 yyyy-MM-dd");
        }
        wrapper.orderByDesc(AdminOperationLog::getCreatedAt);
        Page<AdminOperationLog> result = adminOperationLogMapper.selectPage(new Page<>(p, s), wrapper);
        List<OperationLogResponseDTO> list = result.getRecords().stream()
                .map(row -> OperationLogResponseDTO.builder()
                        .id(row.getId())
                        .operatorName(row.getOperatorName())
                        .actionType(row.getActionType())
                        .actionContent(row.getActionContent())
                        .ip(row.getIp())
                        .createdAt(row.getCreatedAt())
                        .build())
                .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", result.getTotal());
        return data;
    }

    private String truncate(String content) {
        if (content == null) {
            return null;
        }
        return content.length() <= CONTENT_MAX_LENGTH ? content : content.substring(0, CONTENT_MAX_LENGTH);
    }
}
