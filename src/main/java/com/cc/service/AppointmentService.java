package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.request.AppointmentCreateDTO;
import com.cc.dto.response.AppointmentResponseDTO;
import com.cc.dto.response.SlotResponseDTO;
import com.cc.entity.Appointment;
import com.cc.entity.AppointmentSlot;
import com.cc.entity.Consultant;
import com.cc.entity.ConsultantReview;
import com.cc.enums.AppointmentStatus;
import com.cc.enums.NotificationType;
import com.cc.exception.BusinessException;
import com.cc.mapper.AppointmentMapper;
import com.cc.mapper.AppointmentSlotMapper;
import com.cc.mapper.ConsultantMapper;
import com.cc.mapper.ConsultantReviewMapper;
import com.cc.mapper.UserMapper;
import com.cc.entity.User;
import com.cc.service.convert.AppointmentConvert;
import com.cc.util.CurrentUserUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * 预约业务层：条件 UPDATE 原子抢时段、状态机硬校验、取消释放时段、
 * 确认/取消异步通知（{@link NotificationService}，@Async 跨 Bean 调用）。
 *
 * @author cc
 * @date 2026-09-14
 */
@Slf4j
@Service
public class AppointmentService {

    /** 预约列表分页上限 */
    private static final long MAX_PAGE_SIZE = 20L;

    /** 预约设置兜底默认（consultant 列理论上 NOT NULL DEFAULT） */
    private static final int DEFAULT_BOOKING_WINDOW_DAYS = 14;
    private static final int DEFAULT_CANCEL_HOURS_BEFORE = 2;

    /** 通知时间展示格式：M月d日 HH:mm */
    private static final DateTimeFormatter NOTICE_TIME_FORMATTER = DateTimeFormatter.ofPattern("M月d日 HH:mm");

    @Resource
    private AppointmentMapper appointmentMapper;
    @Resource
    private AppointmentSlotMapper appointmentSlotMapper;
    @Resource
    private ConsultantMapper consultantMapper;
    @Resource
    private ConsultantReviewMapper consultantReviewMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private ConsultantService consultantService;
    @Resource
    private AppointmentConvert appointmentConvert;
    @Resource
    private NotificationService notificationService;

    /**
     * 创建预约：事务内先原子抢占时段（禁止先查后改），成功后插入 PENDING 预约。
     * 事务保持短小：抢槽 + 插入；通知类副作用不混入。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(AppointmentCreateDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        Consultant consultant = consultantService.getActiveEntity(dto.getConsultantId());

        AppointmentSlot slot = appointmentSlotMapper.selectById(dto.getSlotId());
        LocalDate today = LocalDate.now();
        if (slot == null
                || !consultant.getId().equals(slot.getConsultantId())
                || !Integer.valueOf(0).equals(slot.getStatus())) {
            throw new BusinessException("该时段不可预约，请重新选择");
        }
        if (slot.getSlotDate().isBefore(today)
                || (slot.getSlotDate().equals(today) && !slot.getStartTime().isAfter(LocalTime.now()))) {
            throw new BusinessException("该时段已过期，请选择其他时段");
        }
        // 可预约窗口硬校验（与用户端时段查询同口径，防绕过前端直调）：今天起 N 天含今天
        int windowDays = consultant.getBookingWindowDays() != null
                ? consultant.getBookingWindowDays() : DEFAULT_BOOKING_WINDOW_DAYS;
        if (slot.getSlotDate().isAfter(today.plusDays(Math.max(windowDays, 1L) - 1L))) {
            throw new BusinessException("该日期超出咨询师开放的可预约天数，请选择更近的日期");
        }

        // 原子抢时段：并发下仅一个请求影响行数为 1
        int occupied = appointmentSlotMapper.occupyIfFree(slot.getId());
        if (occupied == 0) {
            throw new BusinessException("手慢了，该时段刚刚被预约啦，请换个时段试试");
        }

        String problemDesc = dto.getProblemDesc() != null && !dto.getProblemDesc().isBlank()
                ? dto.getProblemDesc().trim() : null;
        Appointment appointment = Appointment.builder()
                .userId(userId)
                .consultantId(consultant.getId())
                .slotId(slot.getId())
                .appointmentDate(slot.getSlotDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .problemDesc(problemDesc)
                .status(AppointmentStatus.PENDING.name())
                .evaluated(0)
                .build();
        appointmentMapper.insert(appointment);
        // 写后删详情缓存，保证回源一致（列表 5min TTL 兜底）
        consultantService.evictConsultantCache(consultant.getId());
        log.info("预约已创建: appointmentId={}, consultantId={}, userId={}",
                appointment.getId(), consultant.getId(), userId);
        return appointment.getId();
    }

    /**
     * 我的预约分页
     */
    public Map<String, Object> mine(String status, Long page, Long size) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : 10L;
        validateStatusFilter(status);
        IPage<AppointmentResponseDTO> result = appointmentMapper.selectMinePage(
                new Page<>(cur, sz), userId, status);
        LocalDateTime now = LocalDateTime.now();
        result.getRecords().forEach(dto -> appointmentConvert.fillLabelsAndFlags(dto, true, now));
        return pageData(result);
    }

    /**
     * 预约详情：本人或管理员可查看；本人带取消/评价权限位
     */
    public AppointmentResponseDTO detail(Long id) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        boolean isAdmin = CurrentUserUtils.isAdmin();
        Appointment appointment = getAppointmentForMember(id, userId, isAdmin);
        AppointmentResponseDTO dto = appointmentConvert.toDTO(appointment);
        fillConsultantInfo(dto, appointment.getConsultantId());
        appointmentConvert.fillLabelsAndFlags(dto, !isAdmin, LocalDateTime.now());
        return dto;
    }

    /**
     * 用户取消预约：PENDING/CONFIRMED 开始前可取消；受该咨询师 cancel_hours_before 时限约束；
     * 取消后释放时段。咨询师/管理员取消为管理特权，不受此时限约束（见 consultantCancel/adminChangeStatus）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelByUser(Long id) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null || !userId.equals(appointment.getUserId())) {
            throw new BusinessException("A0301", "预约不存在或无权操作", null);
        }
        assertWithinUserCancelWindow(appointment);
        doCancel(appointment, null);
        log.info("用户取消预约: appointmentId={}, userId={}", id, userId);
    }

    /**
     * 后台状态变更：CONFIRMED/CANCELED/COMPLETED，严格走状态机。
     * CONFIRMED 时 consult_count +1 并异步通知用户；CANCELED 释放时段、记录取消原因并通知用户。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminChangeStatus(Long id, String targetCode, String cancelReason) {
        ensureAdmin();
        Appointment appointment = mustExist(id);
        AppointmentStatus current = AppointmentStatus.fromCode(appointment.getStatus());
        AppointmentStatus target = AppointmentStatus.fromCode(targetCode);
        if (!current.canTransitTo(target)) {
            throw new BusinessException("非法状态流转：" + current.getLabel() + " → " + target.getLabel());
        }
        if (target == AppointmentStatus.CANCELED && !canCancelBeforeStart(appointment)) {
            throw new BusinessException("咨询已开始，无法取消");
        }

        if (target == AppointmentStatus.CANCELED) {
            String reason = cancelReason != null && !cancelReason.isBlank() ? cancelReason.trim() : null;
            // 管理特权：不受用户取消时限（cancel_hours_before）约束
            doCancel(appointment, reason);
            notifyStatusChange(appointment, target);
            log.info("管理员取消预约: appointmentId={}", id);
            return;
        }

        // 条件 UPDATE 防并发重复流转
        int updated = updateStatusIfCurrent(id, current, target);
        if (updated == 0) {
            throw new BusinessException("预约状态已变更，请刷新后重试");
        }
        if (target == AppointmentStatus.CONFIRMED) {
            incrementConsultCount(appointment.getConsultantId());
        }
        notifyStatusChange(appointment, target);
        log.info("管理员变更预约状态: appointmentId={}, {} -> {}", id, current, target);
    }

    /**
     * 咨询师变更自己的预约状态：仅允许 PENDING→CONFIRMED（确认）、CONFIRMED→COMPLETED（完成）。
     * 取消权归来访者/管理员；COMPLETED 幂等（重复完成直接成功返回）。
     * 归属硬校验：仅预约所属咨询师可操作，越权抛业务异常。
     */
    @Transactional(rollbackFor = Exception.class)
    public void consultantChangeStatus(Long id, String targetCode) {
        Long consultantId = CurrentUserUtils.getCurrentConsultantId();
        Appointment appointment = mustExist(id);
        if (!consultantId.equals(appointment.getConsultantId())) {
            throw new BusinessException("A0301", "预约不存在或无权操作", null);
        }
        AppointmentStatus current = AppointmentStatus.fromCode(appointment.getStatus());
        AppointmentStatus target = AppointmentStatus.fromCode(targetCode);
        // COMPLETED 幂等：定时任务与手动完成可能重叠
        if (current == AppointmentStatus.COMPLETED && target == AppointmentStatus.COMPLETED) {
            return;
        }
        if (target != AppointmentStatus.CONFIRMED && target != AppointmentStatus.COMPLETED) {
            // 取消权限归来访者 / 管理员，咨询师端不放取消入口
            throw new BusinessException("咨询师仅可确认或完成预约，取消请由来访者操作");
        }
        if (!current.canTransitTo(target)) {
            throw new BusinessException("非法状态流转：" + current.getLabel() + " → " + target.getLabel());
        }

        int updated = updateStatusIfCurrent(id, current, target);
        if (updated == 0) {
            throw new BusinessException("预约状态已变更，请刷新后重试");
        }
        if (target == AppointmentStatus.CONFIRMED) {
            incrementConsultCount(consultantId);
            notifyStatusChange(appointment, target);
        } else {
            // 与定时自动完成同口径：提醒来访者评价
            notifyCompleted(appointment);
        }
        log.info("咨询师变更预约状态: consultantId={}, appointmentId={}, {} -> {}",
                consultantId, id, current, target);
    }

    /**
     * 咨询师取消自己的预约（管理特权）：PENDING/CONFIRMED → CANCELED，必填取消原因（DTO 校验），
     * 落 cancelReason/canceledAt 并释放时段。不受用户取消时限（cancel_hours_before）约束，
     * 但 doCancel 内「咨询已开始」守卫仍生效（状态机安全底线）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void consultantCancel(Long id, String cancelReason) {
        Long consultantId = CurrentUserUtils.getCurrentConsultantId();
        Appointment appointment = mustExist(id);
        if (!consultantId.equals(appointment.getConsultantId())) {
            throw new BusinessException("A0301", "预约不存在或无权操作", null);
        }
        String reason = cancelReason != null && !cancelReason.isBlank() ? cancelReason.trim() : null;
        doCancel(appointment, reason);
        notifyStatusChange(appointment, AppointmentStatus.CANCELED);
        log.info("咨询师取消预约: consultantId={}, appointmentId={}", consultantId, id);
    }

    /** CONFIRMED 副作用：consult_count +1 + 删用户端缓存（管理员确认/咨询师确认共用，禁止两套计数） */
    private void incrementConsultCount(Long consultantId) {
        Consultant consultant = consultantMapper.selectById(consultantId);
        if (consultant != null) {
            consultant.setConsultCount(consultant.getConsultCount() == null
                    ? 1 : consultant.getConsultCount() + 1);
            consultantMapper.updateById(consultant);
            consultantService.evictConsultantCache(consultant.getId());
        }
    }

    /** 完成通知：提醒来访者评价（定时自动完成与咨询师手动完成共用） */
    private void notifyCompleted(Appointment appointment) {
        notificationService.createNotification(appointment.getUserId(),
                NotificationType.APPOINTMENT,
                "咨询已完成",
                "你与咨询师的本次咨询已完成，记得分享一下咨询感受并给出评价哦",
                "APPOINTMENT", appointment.getId());
    }

    /**
     * 后台预约分页：状态/咨询师/来访者关键字/预约日期范围筛选
     */
    public Map<String, Object> adminPage(String status, Long consultantId, String keyword,
                                         String startDate, String endDate, Long page, Long size) {
        ensureAdmin();
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : 10L;
        validateStatusFilter(status);
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        IPage<AppointmentResponseDTO> result = appointmentMapper.selectAdminPage(
                new Page<>(cur, sz), status, consultantId, kw, startDate, endDate);
        result.getRecords().forEach(dto -> appointmentConvert.fillLabelsAndFlags(dto, false, LocalDateTime.now()));
        return pageData(result);
    }

    /**
     * 后台预约详情：含问题描述、取消原因与评价信息
     */
    public AppointmentResponseDTO adminDetail(Long id) {
        ensureAdmin();
        Appointment appointment = mustExist(id);
        AppointmentResponseDTO dto = appointmentConvert.toDTO(appointment);
        fillConsultantInfo(dto, appointment.getConsultantId());
        // 来访者信息（与列表 SQL 同口径：昵称为空回退用户名）
        User user = userMapper.selectById(appointment.getUserId());
        if (user != null) {
            dto.setUserNickname(user.getNickname() != null && !user.getNickname().isBlank()
                    ? user.getNickname() : user.getUsername());
            dto.setUserPhone(user.getPhone());
        }
        appointmentConvert.fillLabelsAndFlags(dto, false, LocalDateTime.now());
        ConsultantReview review = consultantReviewMapper.selectOne(new LambdaQueryWrapper<ConsultantReview>()
                .eq(ConsultantReview::getAppointmentId, id)
                .last("LIMIT 1"));
        if (review != null) {
            dto.setReviewRating(review.getRating());
            dto.setReviewContent(review.getContent());
            dto.setReviewReply(review.getReplyContent());
        }
        return dto;
    }

    /**
     * 后台改期：仅 PENDING/CONFIRMED 且未开始可改。
     * 并发安全：先条件 UPDATE 原子抢占新时段（复用 3.7 occupyIfFree），
     * 再条件 UPDATE 预约（状态未变才生效，失败回滚新槽），最后释放旧时段。禁止先查后改。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminReschedule(Long id, Long newSlotId) {
        ensureAdmin();
        Appointment appointment = mustExist(id);
        AppointmentStatus current = AppointmentStatus.fromCode(appointment.getStatus());
        if (current != AppointmentStatus.PENDING && current != AppointmentStatus.CONFIRMED) {
            throw new BusinessException("仅待确认/已确认的预约可以改期");
        }
        if (!canCancelBeforeStart(appointment)) {
            throw new BusinessException("咨询已开始，无法改期");
        }
        if (Objects.equals(appointment.getSlotId(), newSlotId)) {
            throw new BusinessException("新时段与当前时段相同");
        }

        // 输入校验：新时段归属同咨询师且未过期；是否可约由原子 UPDATE 最终判定
        AppointmentSlot newSlot = appointmentSlotMapper.selectById(newSlotId);
        LocalDate today = LocalDate.now();
        if (newSlot == null
                || !newSlot.getConsultantId().equals(appointment.getConsultantId())
                || newSlot.getSlotDate().isBefore(today)
                || (newSlot.getSlotDate().equals(today) && !newSlot.getStartTime().isAfter(LocalTime.now()))) {
            throw new BusinessException("目标时段不可用，请重新选择");
        }

        // 1. 原子抢占新时段：并发下仅一个请求影响行数为 1
        int occupied = appointmentSlotMapper.occupyIfFree(newSlotId);
        if (occupied == 0) {
            throw new BusinessException("目标时段已被预约，请换一个时段");
        }

        // 2. 条件 UPDATE 预约：状态未被并发改变才生效；失败需释放刚抢到的新槽
        Long oldSlotId = appointment.getSlotId();
        int updated = appointmentMapper.update(null, new LambdaUpdateWrapper<Appointment>()
                .eq(Appointment::getId, id)
                .eq(Appointment::getStatus, current.name())
                .set(Appointment::getSlotId, newSlotId)
                .set(Appointment::getAppointmentDate, newSlot.getSlotDate())
                .set(Appointment::getStartTime, newSlot.getStartTime())
                .set(Appointment::getEndTime, newSlot.getEndTime()));
        if (updated == 0) {
            appointmentSlotMapper.releaseIfOccupied(newSlotId);
            throw new BusinessException("预约状态已变更，请刷新后重试");
        }

        // 3. 释放旧时段供他人预约
        appointmentSlotMapper.releaseIfOccupied(oldSlotId);
        consultantService.evictConsultantCache(appointment.getConsultantId());

        // 4. 通知用户改期结果
        notifyReschedule(appointment, newSlot);
        log.info("管理员改期: appointmentId={}, oldSlotId={}, newSlotId={}", id, oldSlotId, newSlotId);
    }

    /**
     * 查询咨询师未来可预约的空闲时段（改期选择用，上限 100 条）
     */
    public List<SlotResponseDTO> listFreeSlots(Long consultantId) {
        ensureAdmin();
        consultantService.getActiveEntity(consultantId);
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        List<AppointmentSlot> slots = appointmentSlotMapper.selectList(new LambdaQueryWrapper<AppointmentSlot>()
                .eq(AppointmentSlot::getConsultantId, consultantId)
                .eq(AppointmentSlot::getStatus, 0)
                .and(w -> w.gt(AppointmentSlot::getSlotDate, today)
                        .or(o -> o.eq(AppointmentSlot::getSlotDate, today)
                                .gt(AppointmentSlot::getStartTime, nowTime)))
                .orderByAsc(AppointmentSlot::getSlotDate)
                .orderByAsc(AppointmentSlot::getStartTime)
                .last("LIMIT 100"));
        return slots.stream().map(this::toSlotDTO).toList();
    }

    /**
     * 用户管理后台：按指定 userId 分页查询其全部预约记录（管理员视角，含全部状态）
     */
    public Map<String, Object> pageForAdminByUserId(Long userId, long page, long size) {
        long cur = Math.max(page, 1L);
        long sz = Math.min(Math.max(size, 1L), MAX_PAGE_SIZE);
        IPage<AppointmentResponseDTO> result = appointmentMapper.selectAdminByUserIdPage(new Page<>(cur, sz), userId);
        result.getRecords().forEach(dto -> appointmentConvert.fillLabelsAndFlags(dto, false, LocalDateTime.now()));
        return pageData(result);
    }

    /**
     * 咨询师管理后台：按指定 consultantId 分页查询其全部预约记录（管理员视角，含全部状态）。
     * 管理员身份由调用方 ConsultantService.ensureAdmin 统一校验。
     */
    public Map<String, Object> pageForAdminByConsultantId(Long consultantId, long page, long size) {
        long cur = Math.max(page, 1L);
        long sz = Math.min(Math.max(size, 1L), MAX_PAGE_SIZE);
        IPage<AppointmentResponseDTO> result = appointmentMapper.selectAdminByConsultantIdPage(new Page<>(cur, sz), consultantId);
        result.getRecords().forEach(dto -> appointmentConvert.fillLabelsAndFlags(dto, false, LocalDateTime.now()));
        return pageData(result);
    }

    /**
     * 定时自动完成：CONFIRMED 且已过结束时间（今天已结束或更早日期）→ COMPLETED。
     * 命中 idx_status_date，条件 UPDATE 防止与人工操作竞争；完成后异步提醒用户评价。
     *
     * @return 本次自动完成的预约数
     */
    @Transactional(rollbackFor = Exception.class)
    public int autoCompleteDue() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        List<Appointment> dues = appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getStatus, AppointmentStatus.CONFIRMED.name())
                .and(w -> w.lt(Appointment::getAppointmentDate, today)
                        .or(o -> o.eq(Appointment::getAppointmentDate, today)
                                .le(Appointment::getEndTime, now))));
        int completed = 0;
        for (Appointment appointment : dues) {
            int updated = updateStatusIfCurrent(appointment.getId(),
                    AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED);
            if (updated > 0) {
                completed++;
                notifyCompleted(appointment);
            }
        }
        if (completed > 0) {
            log.info("定时自动完成预约: {} 条", completed);
        }
        return completed;
    }

    /**
     * WebSocket 握手 / 留言接口鉴权：预约存在且当前用户为本人或管理员。
     */
    public Appointment getAppointmentForMember(Long appointmentId, Long userId, boolean isAdmin) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null
                || (!isAdmin && !appointment.getUserId().equals(userId))) {
            throw new BusinessException("A0301", "预约不存在或无权操作", null);
        }
        return appointment;
    }

    // ==================== 内部方法 ====================

    /**
     * 取消公共逻辑（用户/咨询师/管理员三端共用）：状态机校验 + 开始前校验 + 条件更新
     * （带取消原因/取消时间）+ 释放时段。用户取消时限 cancel_hours_before 由调用方
     * （仅用户路径）先经 {@link #assertWithinUserCancelWindow} 校验，特权路径不校验。
     */
    private void doCancel(Appointment appointment, String cancelReason) {
        AppointmentStatus current = AppointmentStatus.fromCode(appointment.getStatus());
        if (!current.canTransitTo(AppointmentStatus.CANCELED)) {
            throw new BusinessException(current.getLabel() + "的预约不能取消");
        }
        if (!canCancelBeforeStart(appointment)) {
            throw new BusinessException("咨询已开始，无法取消");
        }
        // 条件 UPDATE：仅当前状态匹配时流转，同时落取消原因/取消时间
        int updated = appointmentMapper.update(null, new LambdaUpdateWrapper<Appointment>()
                .eq(Appointment::getId, appointment.getId())
                .eq(Appointment::getStatus, current.name())
                .set(Appointment::getStatus, AppointmentStatus.CANCELED.name())
                .set(Appointment::getCancelReason, cancelReason)
                .set(Appointment::getCanceledAt, LocalDateTime.now()));
        if (updated == 0) {
            throw new BusinessException("预约状态已变更，请刷新后重试");
        }
        // 释放时段，供其他用户预约；status=2(停用) 的时段不恢复
        appointmentSlotMapper.releaseIfOccupied(appointment.getSlotId());
        consultantService.evictConsultantCache(appointment.getConsultantId());
    }

    /** CONFIRMED 预约仅开始时间之前可取消；PENDING 无此限制 */
    private boolean canCancelBeforeStart(Appointment appointment) {
        AppointmentStatus status = AppointmentStatus.fromCode(appointment.getStatus());
        if (status == AppointmentStatus.PENDING) {
            return true;
        }
        LocalDateTime start = LocalDateTime.of(appointment.getAppointmentDate(), appointment.getStartTime());
        return start.isAfter(LocalDateTime.now());
    }

    /**
     * 用户取消时限（仅约束来访者路径；咨询师/管理员取消为管理特权，不调用本校验）：
     * 距开始不足 cancel_hours_before 小时一律拒绝，配置 0 = 不限制。PENDING 同样受限，
     * 避免临近开始临门取消导致空场。
     */
    private void assertWithinUserCancelWindow(Appointment appointment) {
        Consultant consultant = consultantMapper.selectById(appointment.getConsultantId());
        int hours = consultant != null && consultant.getCancelHoursBefore() != null
                ? consultant.getCancelHoursBefore() : DEFAULT_CANCEL_HOURS_BEFORE;
        if (hours <= 0) {
            return;
        }
        LocalDateTime start = LocalDateTime.of(appointment.getAppointmentDate(), appointment.getStartTime());
        if (start.minusHours(hours).isBefore(LocalDateTime.now())) {
            throw new BusinessException("距离咨询开始不足" + hours + "小时，已过取消时限，请联系咨询师或管理员");
        }
    }

    /** 条件 UPDATE：仅当前状态匹配时流转，返回影响行数 */
    private int updateStatusIfCurrent(Long id, AppointmentStatus expect, AppointmentStatus target) {
        return appointmentMapper.update(null, new LambdaUpdateWrapper<Appointment>()
                .eq(Appointment::getId, id)
                .eq(Appointment::getStatus, expect.name())
                .set(Appointment::getStatus, target.name()));
    }

    /** 确认/取消后异步通知用户（问题描述不入通知内容） */
    private void notifyStatusChange(Appointment appointment, AppointmentStatus target) {
        Consultant consultant = consultantMapper.selectById(appointment.getConsultantId());
        String consultantName = consultant != null ? consultant.getName() : "咨询师";
        String timeText = LocalDateTime.of(appointment.getAppointmentDate(), appointment.getStartTime())
                .format(NOTICE_TIME_FORMATTER);
        if (target == AppointmentStatus.CONFIRMED) {
            notificationService.createNotification(appointment.getUserId(),
                    NotificationType.APPOINTMENT,
                    "预约已确认",
                    "你与" + consultantName + "咨询师的预约已确认（" + timeText + "），请准时参加～",
                    "APPOINTMENT", appointment.getId());
        } else if (target == AppointmentStatus.CANCELED) {
            String reasonText = appointment.getCancelReason() != null && !appointment.getCancelReason().isBlank()
                    ? "，取消原因：" + appointment.getCancelReason() : "";
            notificationService.createNotification(appointment.getUserId(),
                    NotificationType.APPOINTMENT,
                    "预约已取消",
                    "你与" + consultantName + "咨询师 " + timeText + " 的预约已取消"
                            + reasonText + "，如有需要可重新预约",
                    "APPOINTMENT", appointment.getId());
        }
    }

    private void fillConsultantInfo(AppointmentResponseDTO dto, Long consultantId) {
        Consultant consultant = consultantMapper.selectById(consultantId);
        if (consultant != null) {
            dto.setConsultantName(consultant.getName());
            dto.setConsultantAvatar(consultant.getAvatar());
            dto.setConsultantTitle(consultant.getTitle());
        }
    }

    /** 改期后异步通知用户新时间 */
    private void notifyReschedule(Appointment appointment, AppointmentSlot newSlot) {
        Consultant consultant = consultantMapper.selectById(appointment.getConsultantId());
        String consultantName = consultant != null ? consultant.getName() : "咨询师";
        String newTimeText = LocalDateTime.of(newSlot.getSlotDate(), newSlot.getStartTime())
                .format(NOTICE_TIME_FORMATTER);
        notificationService.createNotification(appointment.getUserId(),
                NotificationType.APPOINTMENT,
                "预约时间已调整",
                "你与" + consultantName + "咨询师的预约已改期至 " + newTimeText + "，请留意时间变化～",
                "APPOINTMENT", appointment.getId());
    }

    private SlotResponseDTO toSlotDTO(AppointmentSlot slot) {
        SlotResponseDTO dto = new SlotResponseDTO();
        dto.setId(slot.getId());
        dto.setSlotDate(slot.getSlotDate() != null ? slot.getSlotDate().toString() : null);
        dto.setStartTime(slot.getStartTime() != null
                ? slot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) : null);
        dto.setEndTime(slot.getEndTime() != null
                ? slot.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")) : null);
        dto.setStatus(slot.getStatus());
        return dto;
    }

    private Appointment mustExist(Long id) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new BusinessException("预约不存在");
        }
        return appointment;
    }

    private void validateStatusFilter(String status) {
        if (status != null && !status.isBlank()) {
            AppointmentStatus.fromCode(status);
        }
    }

    private Map<String, Object> pageData(IPage<AppointmentResponseDTO> result) {
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return data;
    }

    private void ensureAdmin() {
        if (!CurrentUserUtils.isAdmin()) {
            throw new BusinessException("A0301", "无管理员权限", null);
        }
    }
}
