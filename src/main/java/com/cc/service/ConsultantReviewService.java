package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.request.ConsultantReviewDTO;
import com.cc.dto.response.AdminReviewResponseDTO;
import com.cc.entity.Appointment;
import com.cc.entity.Consultant;
import com.cc.entity.ConsultantReview;
import com.cc.enums.AppointmentStatus;
import com.cc.exception.BusinessException;
import com.cc.mapper.AppointmentMapper;
import com.cc.mapper.ConsultantMapper;
import com.cc.mapper.ConsultantReviewMapper;
import com.cc.util.CurrentUserUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 咨询评价业务层：仅 COMPLETED 且未评价的本人预约可评；
 * 提交/删除后统一走 {@link #recalculateRating} 全量重算 consultant 冗余评分，
 * 一单一评（uk_appointment 兜底）。
 *
 * @author cc
 * @date 2026-09-14
 */
@Slf4j
@Service
public class ConsultantReviewService {

    /** 评价列表分页上限 */
    private static final long MAX_PAGE_SIZE = 20L;

    @Resource
    private ConsultantReviewMapper consultantReviewMapper;
    @Resource
    private ConsultantMapper consultantMapper;
    @Resource
    private AppointmentMapper appointmentMapper;
    @Resource
    private ConsultantService consultantService;

    /**
     * 提交评价
     */
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long appointmentId, ConsultantReviewDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null || !userId.equals(appointment.getUserId())) {
            throw new BusinessException("A0301", "预约不存在或无权操作", null);
        }
        if (AppointmentStatus.fromCode(appointment.getStatus()) != AppointmentStatus.COMPLETED) {
            throw new BusinessException("咨询完成后才能评价哦");
        }
        if (Integer.valueOf(1).equals(appointment.getEvaluated())) {
            throw new BusinessException("该预约已评价过啦");
        }

        // 条件 UPDATE 占位 evaluated=1：并发提交时仅一个请求成功，其余走业务异常而非唯一键报错
        int occupied = appointmentMapper.update(null, new LambdaUpdateWrapper<Appointment>()
                .eq(Appointment::getId, appointmentId)
                .eq(Appointment::getEvaluated, 0)
                .set(Appointment::getEvaluated, 1));
        if (occupied == 0) {
            throw new BusinessException("该预约已评价过啦");
        }

        String content = dto.getContent() != null && !dto.getContent().isBlank()
                ? dto.getContent().trim() : null;
        consultantReviewMapper.insert(ConsultantReview.builder()
                .appointmentId(appointmentId)
                .consultantId(appointment.getConsultantId())
                .userId(userId)
                .rating(dto.getRating())
                .content(content)
                .build());

        // 全量重算冗余评分（与后台删除评价复用同一方法，避免两套口径）
        recalculateRating(appointment.getConsultantId());
        log.info("咨询评价已提交: appointmentId={}, consultantId={}", appointmentId, appointment.getConsultantId());
    }

    /**
     * 管理端评价分页：支持咨询师/评分/内容关键字筛选
     */
    public Map<String, Object> adminPage(Long consultantId, Integer rating, String keyword,
                                         Long page, Long size) {
        ensureAdmin();
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : 10L;
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        IPage<AdminReviewResponseDTO> result = consultantReviewMapper.selectAdminPage(
                new Page<>(cur, sz), consultantId, rating, kw);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return data;
    }

    /**
     * 管理端回复评价（单级回复，重复回复覆盖）
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminReply(Long reviewId, String content) {
        ensureAdmin();
        saveReply(mustExist(reviewId), content);
        log.info("评价回复已保存(管理员): reviewId={}", reviewId);
    }

    /**
     * 咨询师端 - 我的评价分页：强制 consultantId = 当前登录咨询师，复用管理端 JOIN 投影
     * （AdminReviewResponseDTO 只含昵称，不含手机号/邮箱）。
     */
    public Map<String, Object> consultantPage(Long page, Long size) {
        Long consultantId = CurrentUserUtils.getCurrentConsultantId();
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : 10L;
        IPage<AdminReviewResponseDTO> result = consultantReviewMapper.selectAdminPage(
                new Page<>(cur, sz), consultantId, null, null);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return data;
    }

    /**
     * 咨询师端 - 回复自己的评价：归属硬校验（仅评价所属咨询师可回复），重复回复覆盖。
     */
    @Transactional(rollbackFor = Exception.class)
    public void consultantReply(Long reviewId, String content) {
        Long consultantId = CurrentUserUtils.getCurrentConsultantId();
        ConsultantReview review = mustExist(reviewId);
        if (!consultantId.equals(review.getConsultantId())) {
            throw new BusinessException("A0301", "评价不存在或无权操作", null);
        }
        saveReply(review, content);
        log.info("评价回复已保存(咨询师): consultantId={}, reviewId={}", consultantId, reviewId);
    }

    /** 回复落库公共逻辑：覆盖更新 reply_content/reply_at（管理员/咨询师共用，禁止两套 SQL） */
    private void saveReply(ConsultantReview review, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("回复内容不能为空");
        }
        int updated = consultantReviewMapper.update(null, new LambdaUpdateWrapper<ConsultantReview>()
                .eq(ConsultantReview::getId, review.getId())
                .set(ConsultantReview::getReplyContent, content.trim())
                .set(ConsultantReview::getReplyAt, LocalDateTime.now()));
        if (updated == 0) {
            throw new BusinessException("评价不存在或已被删除");
        }
    }

    /**
     * 管理端删除不当评价：删除评价 + 预约 evaluated 复位（允许重新评价）+ 全量重算评分
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminDelete(Long reviewId) {
        ensureAdmin();
        ConsultantReview review = mustExist(reviewId);
        consultantReviewMapper.deleteById(reviewId);
        // 预约评价标记复位为未评价；删除与提交并发时以 evaluated=1 条件保护
        appointmentMapper.update(null, new LambdaUpdateWrapper<Appointment>()
                .eq(Appointment::getId, review.getAppointmentId())
                .eq(Appointment::getEvaluated, 1)
                .set(Appointment::getEvaluated, 0));
        recalculateRating(review.getConsultantId());
        log.info("评价已删除: reviewId={}, appointmentId={}, consultantId={}",
                reviewId, review.getAppointmentId(), review.getConsultantId());
    }

    /**
     * 基于剩余评价全量重算咨询师冗余评分与评价数（提交/删除评价唯一入口）。
     * 无评价时评分归零（null）、评价数 0。
     */
    public void recalculateRating(Long consultantId) {
        Consultant consultant = consultantMapper.selectById(consultantId);
        if (consultant == null) {
            return;
        }
        Map<String, Object> stats = consultantReviewMapper.selectRatingStats(consultantId);
        long count = stats != null && stats.get("count") != null
                ? ((Number) stats.get("count")).longValue() : 0L;
        BigDecimal avg = null;
        if (count > 0 && stats.get("avgRating") != null) {
            avg = new BigDecimal(stats.get("avgRating").toString()).setScale(1, RoundingMode.HALF_UP);
        }
        consultant.setRating(avg);
        consultant.setReviewCount((int) count);
        consultantMapper.updateById(consultant);
        consultantService.evictConsultantCache(consultantId);
    }

    private ConsultantReview mustExist(Long reviewId) {
        ConsultantReview review = consultantReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        return review;
    }

    private void ensureAdmin() {
        if (!CurrentUserUtils.isAdmin()) {
            throw new BusinessException("A0301", "无管理员权限", null);
        }
    }
}
