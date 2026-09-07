package com.cc.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.AIservice.EmotionAnalysisService;
import com.cc.dto.request.EmotionDiaryCreateDTO;
import com.cc.dto.request.EmotionDiaryPageQueryDTO;
import com.cc.dto.response.EmotionDiaryResponseDTO;
import com.cc.entity.EmotionDiary;
import com.cc.exception.BusinessException;
import com.cc.mapper.EmotionDiaryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 情绪日记业务层，处理日记创建、后台分页查询、删除及 AI 情绪分析触发
 *
 * @author cc
 * @date 2026-09-06
 */
@Service
public class EmotionDiaryService {

    @Resource
    private EmotionDiaryMapper emotionDiaryMapper;

    @Resource
    private EmotionAnalysisService emotionAnalysisService;

    /**
     * 添加情绪日记（同一用户同一日期重复提交时覆盖更新），并异步触发 AI 情绪分析
     *
     * @param userId     当前登录用户ID
     * @param createDTO  日记内容
     * @return 日记ID
     */
    public Long createDiary(Long userId, EmotionDiaryCreateDTO createDTO) {
        // 查询该用户当日是否已有日记（数据库存在 user_id + diary_date 唯一索引）
        LambdaQueryWrapper<EmotionDiary> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EmotionDiary::getUserId, userId)
                .eq(EmotionDiary::getDiaryDate, createDTO.getDiaryDate());
        EmotionDiary existing = emotionDiaryMapper.selectOne(queryWrapper);

        EmotionDiary diary;
        if (existing != null) {
            // 覆盖更新当日日记
            existing.setMoodScore(createDTO.getMoodScore());
            existing.setDominantEmotion(createDTO.getDominantEmotion());
            existing.setEmotionTriggers(createDTO.getEmotionTriggers());
            existing.setDiaryContent(createDTO.getDiaryContent());
            existing.setSleepQuality(createDTO.getSleepQuality());
            existing.setStressLevel(createDTO.getStressLevel());
            existing.setUpdatedAt(LocalDateTime.now());
            emotionDiaryMapper.updateById(existing);
            diary = existing;
        } else {
            diary = EmotionDiary.builder()
                    .userId(userId)
                    .diaryDate(createDTO.getDiaryDate())
                    .moodScore(createDTO.getMoodScore())
                    .dominantEmotion(createDTO.getDominantEmotion())
                    .emotionTriggers(createDTO.getEmotionTriggers())
                    .diaryContent(createDTO.getDiaryContent())
                    .sleepQuality(createDTO.getSleepQuality())
                    .stressLevel(createDTO.getStressLevel())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            emotionDiaryMapper.insert(diary);
        }

        // 异步触发 AI 情绪分析
        emotionAnalysisService.analyzeDiaryAsync(diary.getId());
        return diary.getId();
    }

    /**
     * 后台分页查询日记列表，支持用户ID与情绪评分范围筛选
     *
     * @param queryDTO 查询条件（current/size/userId/moodScreRange）
     * @return 分页结果
     */
    public IPage<EmotionDiaryResponseDTO> pageForAdmin(EmotionDiaryPageQueryDTO queryDTO) {
        // 解析情绪评分范围，如 "1-3"
        if (StrUtil.isNotBlank(queryDTO.getMoodScreRange())) {
            String[] parts = queryDTO.getMoodScreRange().split("-");
            if (parts.length == 2) {
                try {
                    queryDTO.setMoodScoreMin(Integer.parseInt(parts[0].trim()));
                    queryDTO.setMoodScoreMax(Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException e) {
                    throw new BusinessException("情绪评分范围格式不正确");
                }
            }
        }

        Page<EmotionDiaryResponseDTO> page = new Page<>(
                queryDTO.getCurrent() != null && queryDTO.getCurrent() > 0 ? queryDTO.getCurrent() : 1,
                queryDTO.getSize() != null && queryDTO.getSize() > 0 ? queryDTO.getSize() : 10);
        return emotionDiaryMapper.selectAdminPage(page, queryDTO);
    }

    /**
     * 后台删除日记
     *
     * @param id 日记ID
     */
    public void deleteForAdmin(Long id) {
        EmotionDiary diary = emotionDiaryMapper.selectById(id);
        if (diary == null) {
            throw new BusinessException("日记不存在");
        }
        emotionDiaryMapper.deleteById(id);
    }
}
