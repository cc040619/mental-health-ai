package com.cc.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 我的课程响应 DTO（已学习 / 学习中 / 已收藏 三组）
 *
 * @author cc
 * @date 2026-09-09
 */
@Data
public class CourseMineResponseDTO {

    // 已学习（全部章节已完成）
    private List<CourseListResponseDTO> learned;

    // 学习中（有进度但未全部完成）
    private List<CourseListResponseDTO> learning;

    // 已收藏
    private List<CourseListResponseDTO> favorites;
}
