package com.cc.service.convert;

import com.cc.dto.response.CourseChapterResponseDTO;
import com.cc.dto.response.CourseDetailResponseDTO;
import com.cc.dto.response.CourseListResponseDTO;
import com.cc.entity.Course;
import com.cc.entity.CourseChapter;
import com.cc.entity.CourseProgress;
import com.cc.enums.CourseCategory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程模块对象转换器：Entity/DTO 互转、时长格式化、完成度计算
 * 禁止在 controller/service 手写转换，统一走此转换器
 *
 * @author cc
 * @date 2026-09-09
 */
@Component
public class CourseConvert {

    /**
     * 秒数格式化为「x分x秒」/「x秒」
     */
    public String formatDuration(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return "0秒";
        }
        int minutes = seconds / 60;
        int secs = seconds % 60;
        if (minutes <= 0) {
            return secs + "秒";
        }
        if (secs == 0) {
            return minutes + "分";
        }
        return minutes + "分" + secs + "秒";
    }

    /**
     * 课程 Entity → 列表项 DTO
     *
     * @param course     课程
     * @param isFavorite 当前用户是否收藏（广场/我的课程可空）
     * @param isLearning 当前用户是否已开始学习（广场可空）
     * @param completion 完成度百分比(0-100)，广场不传
     */
    public CourseListResponseDTO toListDTO(Course course, Boolean isFavorite, Boolean isLearning, Integer completion) {
        if (course == null) {
            return null;
        }
        CourseListResponseDTO dto = new CourseListResponseDTO();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setCategory(course.getCategory());
        CourseCategory cat = CourseCategory.fromCode(course.getCategory());
        dto.setCategoryLabel(cat != null ? cat.getLabel() : course.getCategory());
        dto.setCoverUrl(course.getCoverUrl());
        dto.setTotalDurationText(formatDuration(course.getTotalDuration()));
        dto.setLearnCount(course.getLearnCount());
        dto.setIsFavorite(isFavorite);
        dto.setIsLearning(isLearning);
        dto.setCompletion(completion);
        return dto;
    }

    /**
     * 章节 Entity + 进度 → 章节 DTO
     */
    public CourseChapterResponseDTO toChapterDTO(CourseChapter chapter, CourseProgress progress) {
        if (chapter == null) {
            return null;
        }
        CourseChapterResponseDTO dto = new CourseChapterResponseDTO();
        dto.setId(chapter.getId());
        dto.setTitle(chapter.getTitle());
        dto.setAudioUrl(chapter.getAudioUrl());
        dto.setDuration(chapter.getDuration());
        dto.setDurationText(formatDuration(chapter.getDuration()));
        dto.setIntro(chapter.getIntro());
        dto.setSortOrder(chapter.getSortOrder());
        if (progress != null) {
            dto.setProgressSeconds(progress.getProgressSeconds());
            dto.setIsFinished(progress.getIsFinished() != null && progress.getIsFinished() == 1);
        } else {
            dto.setProgressSeconds(0);
            dto.setIsFinished(false);
        }
        return dto;
    }

    /**
     * 课程详情 DTO 组装：课程 + 章节列表 + 当前用户进度 + 收藏 + 评论数
     * <p>完成度 = 已完成章节数 / 总章节数 * 100
     */
    public CourseDetailResponseDTO toDetailDTO(Course course,
                                               List<CourseChapter> chapters,
                                               List<CourseProgress> userProgress,
                                               Boolean isFavorite,
                                               Long commentCount) {
        CourseDetailResponseDTO dto = new CourseDetailResponseDTO();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setCategory(course.getCategory());
        CourseCategory cat = CourseCategory.fromCode(course.getCategory());
        dto.setCategoryLabel(cat != null ? cat.getLabel() : course.getCategory());
        dto.setDescription(course.getDescription());
        dto.setCoverUrl(course.getCoverUrl());
        dto.setTotalDurationText(formatDuration(course.getTotalDuration()));
        dto.setLearnCount(course.getLearnCount());
        dto.setIsFavorite(isFavorite);
        dto.setCommentCount(commentCount);

        // 进度按 chapterId 索引
        Map<Long, CourseProgress> progressMap = userProgress == null ? Map.of()
                : userProgress.stream().collect(Collectors.toMap(CourseProgress::getChapterId, p -> p, (a, b) -> a));

        List<CourseChapterResponseDTO> chapterDTOs = chapters.stream()
                .map(ch -> toChapterDTO(ch, progressMap.get(ch.getId())))
                .collect(Collectors.toList());
        dto.setChapters(chapterDTOs);

        // 完成度
        dto.setCompletion(calcCompletion(chapters, progressMap));
        return dto;
    }

    /**
     * 计算课程完成度百分比(0-100)
     */
    public Integer calcCompletion(List<CourseChapter> chapters, Map<Long, CourseProgress> progressMap) {
        if (chapters == null || chapters.isEmpty()) {
            return 0;
        }
        long finished = chapters.stream()
                .filter(ch -> {
                    CourseProgress p = progressMap == null ? null : progressMap.get(ch.getId());
                    return p != null && p.getIsFinished() != null && p.getIsFinished() == 1;
                })
                .count();
        return (int) Math.round(finished * 100.0 / chapters.size());
    }
}
