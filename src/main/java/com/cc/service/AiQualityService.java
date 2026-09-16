package com.cc.service;

import com.cc.dto.request.AiSampleAnnotateDTO;
import com.cc.entity.AiQualitySample;
import com.cc.exception.BusinessException;
import com.cc.mapper.AiQualitySampleMapper;
import com.cc.util.CurrentUserUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话质量抽检业务层：随机抽会话（未抽过去重）+ 人工标注（好/一般/坏 + 评分 + 备注）
 *
 * @author cc
 * @date 2026-09-15
 */
@Slf4j
@Service
public class AiQualityService {

    /** 列表分页上限 */
    private static final long MAX_PAGE_SIZE = 20L;

    @Resource
    private AiQualitySampleMapper aiQualitySampleMapper;

    @Resource
    private ConsultationSessionService consultationSessionService;

    /**
     * 发起抽检：随机抽 N 个有消息且未抽过的会话建样本（去重 JOIN 排除）
     */
    @Transactional(rollbackFor = Exception.class)
    public int generate(int count) {
        List<Long> sessionIds = aiQualitySampleMapper.selectRandomSessionIds(count);
        if (sessionIds.isEmpty()) {
            throw new BusinessException("没有可抽检的会话（全部会话已被抽过或无消息）");
        }
        aiQualitySampleMapper.insertBatch(sessionIds);
        log.info("发起AI质量抽检: count={}, operator=admin", sessionIds.size());
        return sessionIds.size();
    }

    /**
     * 抽检列表：状态筛选 + 会话摘要（昵称/标题/场景/消息数）
     */
    public Map<String, Object> page(Integer status, Long page, Long size) {
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : 10L;
        List<Map<String, Object>> records = aiQualitySampleMapper.selectSamplePage(status, (cur - 1) * sz, sz);
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", aiQualitySampleMapper.countByStatus(status));
        return data;
    }

    /**
     * 抽检详情：复用咨询记录详情数据（4.9）+ 标注信息
     */
    public Map<String, Object> detail(Long id) {
        AiQualitySample sample = aiQualitySampleMapper.selectById(id);
        if (sample == null) {
            throw new BusinessException("抽检记录不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("sample", sample);
        data.put("session", consultationSessionService.adminDetail(sample.getSessionId()));
        return data;
    }

    /**
     * 标注：状态(1-3) + 评分(1-5) + 备注；记录标注管理员
     */
    public void annotate(Long id, AiSampleAnnotateDTO dto) {
        AiQualitySample sample = aiQualitySampleMapper.selectById(id);
        if (sample == null) {
            throw new BusinessException("抽检记录不存在");
        }
        AiQualitySample update = AiQualitySample.builder()
                .id(id)
                .status(dto.getStatus())
                .score(dto.getScore())
                .note(dto.getNote())
                .annotatorId(CurrentUserUtils.getCurrentUserId())
                .annotatedAt(LocalDateTime.now())
                .build();
        aiQualitySampleMapper.updateById(update);
        log.info("抽检标注完成: sampleId={}, status={}, operator=admin", id, dto.getStatus());
    }
}
