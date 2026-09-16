package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.request.ArticleCreateDTO;
import com.cc.dto.request.ConsultantSettingsDTO;
import com.cc.dto.request.SlotBatchCreateDTO;
import com.cc.dto.response.AppointmentResponseDTO;
import com.cc.dto.response.CategoryResponseDTO;
import com.cc.dto.response.ConsultantSettingsResponseDTO;
import com.cc.dto.response.SlotResponseDTO;
import com.cc.dto.response.WorkbenchOverviewResponseDTO;
import com.cc.dto.response.WorkbenchTodosResponseDTO;
import com.cc.entity.Appointment;
import com.cc.entity.Consultant;
import com.cc.entity.ConsultantReview;
import com.cc.entity.User;
import com.cc.enums.AppointmentStatus;
import com.cc.mapper.AppointmentMapper;
import com.cc.mapper.ConsultantMapper;
import com.cc.mapper.ConsultantReviewMapper;
import com.cc.mapper.UserMapper;
import com.cc.service.convert.AppointmentConvert;
import com.cc.util.CurrentUserUtils;
import com.cc.util.HtmlSanitizer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 咨询师工作台业务层：数据概览、待办、我的预约、日程周视图、我的评价/回复、发文。
 *
 * <p>信任边界：每个方法先经 {@link CurrentUserUtils#getCurrentConsultantId()} 完成
 * 「user_type=3 + 已绑定 consultant」校验，所有查询/操作强制拼当前咨询师 id，
 * 禁止查全量、禁止跨咨询师访问。用户信息只取昵称，不返回手机号/邮箱。</p>
 *
 * @author cc
 * @date 2026-09-17
 */
@Slf4j
@Service
public class ConsultantWorkbenchService {

    private static final long MAX_PAGE_SIZE = 20L;
    private static final int TODO_LIMIT = 5;
    private static final int UNREPLIED_REVIEW_DAYS = 30;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Resource
    private ConsultantMapper consultantMapper;
    @Resource
    private AppointmentMapper appointmentMapper;
    @Resource
    private ConsultantReviewMapper consultantReviewMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private ConsultantService consultantService;
    @Resource
    private ConsultantReviewService consultantReviewService;
    @Resource
    private AppointmentService appointmentService;
    @Resource
    private KnowledgeArticleService knowledgeArticleService;
    @Resource
    private KnowledgeCategoryService knowledgeCategoryService;
    @Resource
    private AppointmentConvert appointmentConvert;

    // ==================== 工作台 ====================

    /**
     * 数据概览：今日预约/待处理/本月咨询/估算总收入/平均评分。
     */
    public WorkbenchOverviewResponseDTO overview() {
        Long consultantId = CurrentUserUtils.getCurrentConsultantId();
        Consultant consultant = consultantMapper.selectById(consultantId);
        LocalDate today = LocalDate.now();

        long todayCount = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getConsultantId, consultantId)
                .eq(Appointment::getAppointmentDate, today)
                .in(Appointment::getStatus,
                        AppointmentStatus.PENDING.name(), AppointmentStatus.CONFIRMED.name()));
        long pendingCount = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getConsultantId, consultantId)
                .eq(Appointment::getStatus, AppointmentStatus.PENDING.name()));

        YearMonth month = YearMonth.now();
        long monthCompleted = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getConsultantId, consultantId)
                .eq(Appointment::getStatus, AppointmentStatus.COMPLETED.name())
                .between(Appointment::getAppointmentDate,
                        month.atDay(1), month.atEndOfMonth()));

        long totalCompleted = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getConsultantId, consultantId)
                .eq(Appointment::getStatus, AppointmentStatus.COMPLETED.name()));

        WorkbenchOverviewResponseDTO dto = new WorkbenchOverviewResponseDTO();
        dto.setConsultantName(consultant.getName());
        dto.setTodayAppointments(todayCount);
        dto.setPendingAppointments(pendingCount);
        dto.setMonthConsultCount(monthCompleted);
        dto.setAvgRating(consultant.getRating());
        dto.setReviewCount(consultant.getReviewCount());
        if (consultant.getPrice() == null) {
            // 未设置价格：收入返回 null + 标记位，前端展示「未设置」
            dto.setTotalIncome(null);
            dto.setPriceUnset(Boolean.TRUE);
        } else {
            dto.setTotalIncome(consultant.getPrice().multiply(BigDecimal.valueOf(totalCompleted)));
            dto.setPriceUnset(Boolean.FALSE);
        }
        return dto;
    }

    /**
     * 待办三组（各 ≤5）：即将开始 / 未回复评价（近 30 天）/ 待处理预约申请。
     * 一次批量查询来访者昵称，禁止逐条查库。
     */
    public WorkbenchTodosResponseDTO todos() {
        Long consultantId = CurrentUserUtils.getCurrentConsultantId();
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        List<Appointment> upcoming = appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getConsultantId, consultantId)
                .eq(Appointment::getAppointmentDate, today)
                .eq(Appointment::getStatus, AppointmentStatus.CONFIRMED.name())
                .ge(Appointment::getStartTime, nowTime)
                .orderByAsc(Appointment::getStartTime)
                .last("LIMIT " + TODO_LIMIT));

        List<Appointment> pending = appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getConsultantId, consultantId)
                .eq(Appointment::getStatus, AppointmentStatus.PENDING.name())
                .orderByAsc(Appointment::getCreatedAt)
                .last("LIMIT " + TODO_LIMIT));

        List<ConsultantReview> unreplied = consultantReviewMapper.selectList(
                new LambdaQueryWrapper<ConsultantReview>()
                        .eq(ConsultantReview::getConsultantId, consultantId)
                        .isNull(ConsultantReview::getReplyContent)
                        .ge(ConsultantReview::getCreatedAt,
                                LocalDateTime.now().minusDays(UNREPLIED_REVIEW_DAYS))
                        .orderByDesc(ConsultantReview::getCreatedAt)
                        .last("LIMIT " + TODO_LIMIT));

        // 批量取昵称（仅昵称，隐私边界内）
        Set<Long> userIds = new HashSet<>();
        upcoming.forEach(a -> userIds.add(a.getUserId()));
        pending.forEach(a -> userIds.add(a.getUserId()));
        unreplied.forEach(r -> userIds.add(r.getUserId()));
        Map<Long, String> nameMap = loadNicknames(userIds);

        WorkbenchTodosResponseDTO result = new WorkbenchTodosResponseDTO();
        result.setUpcoming(upcoming.stream().map(a -> {
            WorkbenchTodosResponseDTO.UpcomingItem item = new WorkbenchTodosResponseDTO.UpcomingItem();
            item.setAppointmentId(a.getId());
            item.setStartTime(a.getStartTime().format(TIME_FORMATTER));
            item.setEndTime(a.getEndTime().format(TIME_FORMATTER));
            item.setUserNickname(nameMap.get(a.getUserId()));
            return item;
        }).toList());
        result.setPendingAppointments(pending.stream().map(a -> {
            WorkbenchTodosResponseDTO.PendingAppointmentItem item =
                    new WorkbenchTodosResponseDTO.PendingAppointmentItem();
            item.setAppointmentId(a.getId());
            item.setUserNickname(nameMap.get(a.getUserId()));
            item.setAppointmentDate(a.getAppointmentDate());
            item.setStartTime(a.getStartTime().format(TIME_FORMATTER));
            item.setCreatedAt(a.getCreatedAt());
            return item;
        }).toList());
        result.setUnrepliedReviews(unreplied.stream().map(r -> {
            WorkbenchTodosResponseDTO.UnrepliedReviewItem item =
                    new WorkbenchTodosResponseDTO.UnrepliedReviewItem();
            item.setReviewId(r.getId());
            item.setAppointmentId(r.getAppointmentId());
            item.setUserNickname(nameMap.get(r.getUserId()));
            item.setRating(r.getRating());
            item.setContent(r.getContent());
            item.setCreatedAt(r.getCreatedAt());
            return item;
        }).toList());
        return result;
    }

    // ==================== 我的预约 ====================

    /**
     * 我的预约分页：仅当前咨询师记录，支持状态/预约日期筛选；投影不含手机号/邮箱。
     */
    public Map<String, Object> appointments(String status, String date, Long page, Long size) {
        Long consultantId = CurrentUserUtils.getCurrentConsultantId();
        if (status != null && !status.isBlank()) {
            // 非法状态直接抛业务异常，不带入 SQL
            AppointmentStatus.valueOf(status.trim());
        }
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : 10L;
        IPage<AppointmentResponseDTO> result = appointmentMapper.selectConsultantPage(
                new Page<>(cur, sz), consultantId,
                status != null && !status.isBlank() ? status.trim() : null,
                date != null && !date.isBlank() ? date.trim() : null);
        result.getRecords().forEach(dto -> appointmentConvert.fillLabelsAndFlags(dto, false, LocalDateTime.now()));
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return data;
    }

    /** 确认/完成自己的预约（状态机 + 归属校验在 AppointmentService） */
    public void changeAppointmentStatus(Long appointmentId, String targetStatus) {
        appointmentService.consultantChangeStatus(appointmentId, targetStatus);
    }

    /** 咨询师取消自己的预约（特权：不受用户取消时限约束，原因必填由 DTO 校验） */
    public void cancelAppointment(Long appointmentId, String reason) {
        appointmentService.consultantCancel(appointmentId, reason);
    }

    // ==================== 日程排班（与管理端 4.3 共用 appointment_slot） ====================

    /** 批量生成自己的排班（按日/周模板，已存在跳过，逻辑复用管理端同一内部方法） */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> createSlots(SlotBatchCreateDTO dto) {
        Long consultantId = CurrentUserUtils.getCurrentConsultantId();
        return consultantService.batchCreateSlotsOwn(consultantId, dto);
    }

    /** 停用/恢复自己的单个时段（已约拒绝，越权在 ConsultantService 抛 A0301） */
    public void updateSlotStatus(Long slotId, Integer status) {
        consultantService.updateSlotStatusOwn(slotId, status,
                CurrentUserUtils.getCurrentConsultantId());
    }

    /** 删除自己的单个时段（仅可约/停用可删，已约拒绝） */
    public void deleteSlot(Long slotId) {
        consultantService.deleteSlot(slotId, CurrentUserUtils.getCurrentConsultantId());
    }

    // ==================== 预约设置 ====================

    /** 我的预约设置：价格 / 可预约天数 / 用户取消时限 */
    public ConsultantSettingsResponseDTO getSettings() {
        Long consultantId = CurrentUserUtils.getCurrentConsultantId();
        Consultant c = consultantMapper.selectById(consultantId);
        ConsultantSettingsResponseDTO dto = new ConsultantSettingsResponseDTO();
        dto.setPrice(c.getPrice());
        dto.setBookingWindowDays(c.getBookingWindowDays() != null ? c.getBookingWindowDays() : 14);
        dto.setCancelHoursBefore(c.getCancelHoursBefore() != null ? c.getCancelHoursBefore() : 2);
        return dto;
    }

    /** 保存预约设置（范围由 DTO 校验）；写后删详情缓存，用户端预约窗口即时生效 */
    @Transactional(rollbackFor = Exception.class)
    public ConsultantSettingsResponseDTO updateSettings(ConsultantSettingsDTO dto) {
        Long consultantId = CurrentUserUtils.getCurrentConsultantId();
        consultantMapper.update(null, new LambdaUpdateWrapper<Consultant>()
                .eq(Consultant::getId, consultantId)
                // price 显式 set null = 不收费（LambdaUpdateWrapper 不忽略 null）
                .set(Consultant::getPrice, dto.getPrice())
                .set(Consultant::getBookingWindowDays, dto.getBookingWindowDays())
                .set(Consultant::getCancelHoursBefore, dto.getCancelHoursBefore()));
        consultantService.evictConsultantCache(consultantId);
        log.info("咨询师更新预约设置: consultantId={}, price={}, bookingWindowDays={}, cancelHoursBefore={}",
                consultantId, dto.getPrice(), dto.getBookingWindowDays(), dto.getCancelHoursBefore());
        return getSettings();
    }

    // ==================== 日程 ====================

    /**
     * 周视图数据源：复用管理端排班周查询（去 ensureAdmin 变体），已约时段补充来访者昵称；
     * 与管理端 4.3 共用 appointment_slot 同一张表，任一端改动另一端立即可见，无缓存。
     */
    public List<SlotResponseDTO> schedule(LocalDate weekStart) {
        Long consultantId = CurrentUserUtils.getCurrentConsultantId();
        List<SlotResponseDTO> slots = consultantService.weekSlotsForConsultant(consultantId, weekStart);

        List<Long> bookedSlotIds = slots.stream()
                .filter(s -> Integer.valueOf(1).equals(s.getStatus()))
                .map(SlotResponseDTO::getId).toList();
        if (bookedSlotIds.isEmpty()) {
            return slots;
        }
        // 已约时段 → 预约（PENDING/CONFIRMED 占用槽位）→ 来访者昵称
        List<Appointment> booked = appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getConsultantId, consultantId)
                .in(Appointment::getSlotId, bookedSlotIds)
                .in(Appointment::getStatus,
                        AppointmentStatus.PENDING.name(), AppointmentStatus.CONFIRMED.name()));
        Map<Long, Long> slotUserMap = booked.stream()
                .collect(Collectors.toMap(Appointment::getSlotId, Appointment::getUserId, (a, b) -> a));
        Map<Long, String> nameMap = loadNicknames(new HashSet<>(slotUserMap.values()));
        slots.forEach(s -> {
            if (Integer.valueOf(1).equals(s.getStatus())) {
                s.setUserNickname(nameMap.get(slotUserMap.get(s.getId())));
            }
        });
        return slots;
    }

    // ==================== 评价 ====================

    /** 我的评价分页（归属在 ConsultantReviewService 内强制） */
    public Map<String, Object> reviews(Long page, Long size) {
        return consultantReviewService.consultantPage(page, size);
    }

    /** 回复自己的评价（≤500 字由 DTO 校验，归属在 Service 内硬校验；不打印回复内容） */
    @Transactional(rollbackFor = Exception.class)
    public void replyReview(Long reviewId, String content) {
        consultantReviewService.consultantReply(reviewId, content);
    }

    // ==================== 发文 ====================

    /** 发文分类下拉：复用启用分类树 */
    public List<CategoryResponseDTO> articleCategories() {
        CurrentUserUtils.getCurrentConsultantId();
        return knowledgeCategoryService.listEnabled();
    }

    /**
     * 咨询师发布文章：author_id=当前登录账号，富文本先过 HtmlSanitizer 白名单清洗，
     * 复用管理端 create（默认 status=0 草稿），管理员在 4.4 上架后用户端可见。
     * ponytail: 如业务要求咨询师发文直发，将 KnowledgeArticleService.create 默认 status 调整为 1 即可。
     */
    public String publishArticle(ArticleCreateDTO dto) {
        Long consultantId = CurrentUserUtils.getCurrentConsultantId();
        dto.setContent(HtmlSanitizer.sanitize(dto.getContent()));
        String articleId = knowledgeArticleService.create(CurrentUserUtils.getCurrentUserId(), dto);
        log.info("咨询师发文: consultantId={}, articleId={}, action=publish_draft", consultantId, articleId);
        return articleId;
    }

    // ==================== 内部方法 ====================

    /** 按用户 id 批量取展示名（昵称为空回退用户名）；入参为空返回空 Map */
    private Map<Long, String> loadNicknames(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(User::getId, User::getDisplayName, (a, b) -> a));
    }
}
