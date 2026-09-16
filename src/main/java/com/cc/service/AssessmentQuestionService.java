package com.cc.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.request.QuestionSaveDTO;
import com.cc.entity.AssessmentQuestion;
import com.cc.exception.BusinessException;
import com.cc.mapper.AssessmentQuestionMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 通用题目库 Service（素材池）
 * <p>题库题修改/删除不影响已建量表（快照隔离），删除不校验引用</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Slf4j
@Service
public class AssessmentQuestionService {

    @Resource
    private AssessmentQuestionMapper questionMapper;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 题库分页
     */
    public IPage<AssessmentQuestion> page(String keyword, String type, long page, long size) {
        long cur = Math.max(page, 1L);
        long sz = Math.min(Math.max(size, 1L), 50L);
        return questionMapper.selectAdminPage(new Page<>(cur, sz), keyword, type);
    }

    /**
     * 新增题库题
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(QuestionSaveDTO dto) {
        validateOptions(dto.getOptions());
        AssessmentQuestion q = AssessmentQuestion.builder()
                .stem(dto.getStem().trim())
                .type(dto.getType())
                .options(dto.getOptions())
                .dimensionTag(dto.getDimensionTag())
                .category(dto.getCategory())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        questionMapper.insert(q);
        log.info("管理员新增题库题: id={}", q.getId());
        return q.getId();
    }

    /**
     * 编辑题库题（快照机制：不影响已引用量表，注释说明）
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, QuestionSaveDTO dto) {
        AssessmentQuestion q = questionMapper.selectById(id);
        if (q == null) {
            throw new BusinessException("题目不存在");
        }
        validateOptions(dto.getOptions());
        q.setStem(dto.getStem().trim());
        q.setType(dto.getType());
        q.setOptions(dto.getOptions());
        q.setDimensionTag(dto.getDimensionTag());
        q.setCategory(dto.getCategory());
        q.setUpdatedAt(LocalDateTime.now());
        questionMapper.updateById(q);
        log.info("管理员编辑题库题: id={}", id);
    }

    /**
     * 删除题库题（快照隔离，不影响任何量表，直接删）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        questionMapper.deleteById(id);
        log.info("管理员删除题库题: id={}", id);
    }

    /**
     * 校验选项 JSON：≥2 项，每项含 label（非空）+ score（数字）
     */
    private void validateOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            throw new BusinessException("选项不能为空");
        }
        try {
            List<Map<String, Object>> options = MAPPER.readValue(optionsJson,
                    new TypeReference<List<Map<String, Object>>>() {});
            if (options.size() < 2) {
                throw new BusinessException("选项至少需要 2 项");
            }
            for (Map<String, Object> opt : options) {
                Object label = opt.get("label");
                Object score = opt.get("score");
                if (label == null || label.toString().isBlank()) {
                    throw new BusinessException("选项 label 不能为空");
                }
                if (score == null) {
                    throw new BusinessException("选项分值不能为空");
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("选项 JSON 格式错误");
        }
    }
}
