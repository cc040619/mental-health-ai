package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.dto.request.CrisisPlanSaveDTO;
import com.cc.dto.response.CrisisPlanResponseDTO;
import com.cc.entity.CrisisEvent;
import com.cc.entity.CrisisSafetyPlan;
import com.cc.mapper.CrisisEventMapper;
import com.cc.mapper.CrisisSafetyPlanMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 危机干预业务层
 *
 * <p>职责：① 个人安全计划 GET/PUT（每用户一行，user_id 唯一键，不缓存）；
 * ② 危机事件记录（{@link #recordCrisisEvent} @Async 异步写入，不阻塞 SSE 流式链路）。</p>
 *
 * <p><b>隐私</b>：危机事件仅记录 user_id / source / 命中词 / 时间，禁止存储对话内容。
 * 安全计划仅本人可读写，越权抛 {@link com.cc.exception.BusinessException}。</p>
 *
 * <p>ponytail: 危机检测能力复用 {@link com.cc.util.SensitiveWordFilter#findCrisisWord}（DFA 内存词树，
 * 已加载 crisis-words.txt），不新建 CrisisDetector——SensitiveWordFilter 即"危机检测一处实现"，
 * 社区发帖/AI 对话/测评报告均复用它。若未来需要独立词表/复杂匹配语义，再抽离 CrisisDetector。</p>
 *
 * @author cc
 * @date 2026-09-14
 */
@Slf4j
@Service
public class CrisisPlanService {

    /** 危机事件来源：AI 对话命中危机词 */
    public static final String SOURCE_CHAT = "CHAT";
    /** 危机事件来源：社区发帖/评论命中危机词 */
    public static final String SOURCE_COMMUNITY = "COMMUNITY";
    /** 危机事件来源：测评报告高危 */
    public static final String SOURCE_ASSESSMENT = "ASSESSMENT";

    @Resource
    private CrisisSafetyPlanMapper crisisSafetyPlanMapper;
    @Resource
    private CrisisEventMapper crisisEventMapper;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 获取我的安全计划（无记录返回空模板，不报错）
     */
    public CrisisPlanResponseDTO getMyPlan(Long userId) {
        CrisisSafetyPlan plan = crisisSafetyPlanMapper.selectOne(
                new LambdaQueryWrapper<CrisisSafetyPlan>()
                        .eq(CrisisSafetyPlan::getUserId, userId));
        if (plan == null) {
            return emptyPlan();
        }
        return toDTO(plan);
    }

    /**
     * 保存安全计划（upsert：user_id 唯一键，存在则更新，不存在则新增）
     */
    @Transactional(rollbackFor = Exception.class)
    public CrisisPlanResponseDTO saveMyPlan(Long userId, CrisisPlanSaveDTO dto) {
        CrisisSafetyPlan existing = crisisSafetyPlanMapper.selectOne(
                new LambdaQueryWrapper<CrisisSafetyPlan>()
                        .eq(CrisisSafetyPlan::getUserId, userId));
        String lovedOnesJson = writeJson(dto.getLovedOnes());
        String copingJson = writeJson(dto.getCopingStrategies());
        String emergencyContact = dto.getEmergencyContact() == null ? null : dto.getEmergencyContact().trim();

        if (existing == null) {
            CrisisSafetyPlan plan = CrisisSafetyPlan.builder()
                    .userId(userId)
                    .lovedOnes(lovedOnesJson)
                    .copingStrategies(copingJson)
                    .emergencyContact(emergencyContact)
                    .build();
            crisisSafetyPlanMapper.insert(plan);
            log.info("新增安全计划: userId={}", userId);
            return toDTO(plan);
        }
        existing.setLovedOnes(lovedOnesJson);
        existing.setCopingStrategies(copingJson);
        existing.setEmergencyContact(emergencyContact);
        crisisSafetyPlanMapper.updateById(existing);
        log.info("更新安全计划: userId={}", userId);
        return toDTO(existing);
    }

    /**
     * 异步记录危机事件（仅元数据，不含对话内容）
     * <p>由 {@link com.cc.AIservice.PsychologicalSupportService} 流式链路命中危机词时跨 bean 调用，
     * @Async 保证不阻塞 SSE 流；同类自调用不生效，此处为跨 bean 调用，安全。</p>
     */
    @Async
    public void recordCrisisEvent(Long userId, String source, String matchedKeyword) {
        try {
            CrisisEvent event = CrisisEvent.builder()
                    .userId(userId)
                    .source(source)
                    .matchedKeyword(matchedKeyword)
                    .build();
            crisisEventMapper.insert(event);
            log.info("记录危机事件: userId={}, source={}, keyword={}", userId, source, matchedKeyword);
        } catch (Exception e) {
            // 危机事件记录失败不阻断主流程（已 @Async，仅记录日志）
            log.warn("危机事件记录失败(不影响对话): userId={}, source={}, 原因: {}", userId, source, e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    private CrisisPlanResponseDTO toDTO(CrisisSafetyPlan plan) {
        CrisisPlanResponseDTO dto = new CrisisPlanResponseDTO();
        dto.setId(plan.getId());
        dto.setLovedOnes(readLovedOnes(plan.getLovedOnes()));
        dto.setCopingStrategies(readStrList(plan.getCopingStrategies()));
        dto.setEmergencyContact(plan.getEmergencyContact());
        dto.setUpdatedAt(plan.getUpdatedAt());
        return dto;
    }

    private CrisisPlanResponseDTO emptyPlan() {
        CrisisPlanResponseDTO dto = new CrisisPlanResponseDTO();
        dto.setLovedOnes(new ArrayList<>());
        dto.setCopingStrategies(new ArrayList<>());
        return dto;
    }

    /** List<LovedOne> → JSON 字符串，空列表/ null 都返回 null */
    private String writeJson(List<?> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.warn("安全计划 JSON 序列化失败(存 null): {}", e.getMessage());
            return null;
        }
    }

    private List<CrisisPlanResponseDTO.LovedOne> readLovedOnes(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<CrisisPlanResponseDTO.LovedOne>>() {
            });
        } catch (Exception e) {
            log.warn("loved_ones JSON 解析失败(返回空列表): {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<String> readStrList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("coping_strategies JSON 解析失败(返回空列表): {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
