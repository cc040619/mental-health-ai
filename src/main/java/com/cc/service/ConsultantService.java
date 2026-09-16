package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.common.RedisConstants;
import com.cc.config.OssProperties;
import com.cc.dto.request.ConsultantAuditDTO;
import com.cc.dto.request.ConsultantSaveDTO;
import com.cc.dto.request.SlotBatchCreateDTO;
import com.cc.dto.response.AppointmentResponseDTO;
import com.cc.dto.response.ConsultantAdminDetailResponseDTO;
import com.cc.dto.response.ConsultantAdminListResponseDTO;
import com.cc.dto.response.ConsultantDetailResponseDTO;
import com.cc.dto.response.ConsultantIncomeResponseDTO;
import com.cc.dto.response.ConsultantListResponseDTO;
import com.cc.dto.response.ConsultantSaveResultDTO;
import com.cc.dto.response.ReviewResponseDTO;
import com.cc.dto.response.SlotResponseDTO;
import com.cc.entity.Appointment;
import com.cc.entity.AppointmentSlot;
import com.cc.entity.Consultant;
import com.cc.entity.ConsultantReview;
import com.cc.enums.AppointmentStatus;
import com.cc.enums.ConsultantStatus;
import com.cc.enums.UserStatus;
import com.cc.exception.BusinessException;
import com.cc.mapper.AppointmentMapper;
import com.cc.mapper.AppointmentSlotMapper;
import com.cc.mapper.ConsultantMapper;
import com.cc.mapper.ConsultantReviewMapper;
import com.cc.service.convert.AppointmentConvert;
import com.cc.service.convert.ConsultantConvert;
import com.cc.util.CurrentUserUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 咨询师业务层：用户端列表/详情 Redis 缓存（TTL 5min），管理端写操作主动删缓存；
 * 时段、评价、预约记录、收入统计实时查询不缓存。
 *
 * @author cc
 * @date 2026-09-14
 */
@Slf4j
@Service
public class ConsultantService {

    /** 列表分页上限（用户端与管理端统一 ≤20） */
    private static final long MAX_PAGE_SIZE = 20L;

    /** 资质图片最多张数（与 DTO @Size 对齐） */
    private static final int MAX_QUALIFICATION_IMAGES = 9;

    // appointment_slot.status：0-可约 1-已约 2-停用
    private static final int SLOT_FREE = 0;
    private static final int SLOT_BOOKED = 1;
    private static final int SLOT_DISABLED = 2;

    /** 可预约天数兜底默认（consultant.booking_window_days 理论上 NOT NULL DEFAULT 14） */
    private static final int DEFAULT_BOOKING_WINDOW_DAYS = 14;

    @Resource
    private ConsultantMapper consultantMapper;
    @Resource
    private AppointmentSlotMapper appointmentSlotMapper;
    @Resource
    private ConsultantReviewMapper consultantReviewMapper;
    @Resource
    private AppointmentMapper appointmentMapper;
    @Resource
    private ConsultantConvert consultantConvert;
    @Resource
    private AppointmentConvert appointmentConvert;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private OssProperties ossProperties;
    @Resource
    private UserService userService;

    // ==================== 用户端 ====================

    /**
     * 正常状态咨询师分页：仅展示审核通过(status=NORMAL)，按擅长领域 LIKE 筛选，缓存命中直接返回。
     * 排序：sort_weight 权重优先，评分/咨询次数次之。
     */
    public Map<String, Object> list(String specialty, Long page, Long size) {
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : 10L;
        String cacheKey = RedisConstants.buildConsultantListKey(specialty, cur, sz);

        Map<String, Object> cached = readListCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        LambdaQueryWrapper<Consultant> wrapper = new LambdaQueryWrapper<Consultant>()
                .eq(Consultant::getStatus, ConsultantStatus.NORMAL.getCode())
                .orderByDesc(Consultant::getSortWeight)
                .orderByDesc(Consultant::getRating)
                .orderByDesc(Consultant::getConsultCount)
                .orderByAsc(Consultant::getId);
        if (specialty != null && !specialty.isBlank()) {
            wrapper.like(Consultant::getSpecialties, specialty.trim());
        }
        IPage<Consultant> result = consultantMapper.selectPage(new Page<>(cur, sz), wrapper);
        List<ConsultantListResponseDTO> records = result.getRecords().stream()
                .map(consultantConvert::toListDTO).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        writeListCache(cacheKey, data);
        return data;
    }

    /**
     * 咨询师详情（仅正常状态），走缓存
     */
    public ConsultantDetailResponseDTO detail(Long id) {
        Consultant consultant = getActiveEntity(id);
        ConsultantDetailResponseDTO cached = readDetailCache(id);
        if (cached != null) {
            return cached;
        }
        cached = consultantConvert.toDetailDTO(consultant);
        writeDetailCache(id, cached);
        return cached;
    }

    /**
     * 某日可约时段（status=0，按时间升序）；date 为空返回可预约窗口内聚合。
     * 过去日期不返回时段；可预约日期范围受 consultant.booking_window_days 限制（今天起 N 天，含今天）。
     * 禁用/待审核咨询师经 getActiveEntity 拦截，新预约不可选。
     */
    public List<SlotResponseDTO> availableSlots(Long consultantId, LocalDate date) {
        Consultant consultant = getActiveEntity(consultantId);
        LocalDate today = LocalDate.now();
        int windowDays = consultant.getBookingWindowDays() != null
                ? consultant.getBookingWindowDays() : DEFAULT_BOOKING_WINDOW_DAYS;
        LocalDate lastBookable = today.plusDays(Math.max(windowDays, 1L) - 1L);
        // 超出窗口（或过去）的日期直接无时段；信任边界在 create() 还有同款硬校验
        if (date != null && (date.isBefore(today) || date.isAfter(lastBookable))) {
            return List.of();
        }
        LambdaQueryWrapper<AppointmentSlot> wrapper = new LambdaQueryWrapper<AppointmentSlot>()
                .eq(AppointmentSlot::getConsultantId, consultantId)
                .eq(AppointmentSlot::getStatus, SLOT_FREE)
                .ge(AppointmentSlot::getSlotDate, today)
                .orderByAsc(AppointmentSlot::getSlotDate)
                .orderByAsc(AppointmentSlot::getStartTime);
        if (date != null) {
            wrapper.eq(AppointmentSlot::getSlotDate, date);
        } else {
            wrapper.le(AppointmentSlot::getSlotDate, lastBookable);
        }
        return appointmentSlotMapper.selectList(wrapper).stream()
                .map(consultantConvert::toSlotDTO).toList();
    }

    /**
     * 来访者评价分页（匿名展示）
     */
    public Map<String, Object> reviews(Long consultantId, Long page, Long size) {
        getActiveEntity(consultantId);
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : 10L;
        IPage<ConsultantReview> result = consultantReviewMapper.selectPage(new Page<>(cur, sz),
                new LambdaQueryWrapper<ConsultantReview>()
                        .eq(ConsultantReview::getConsultantId, consultantId)
                        .orderByDesc(ConsultantReview::getCreatedAt));
        return reviewPage(result);
    }

    // ==================== 后台管理：列表/详情/编辑 ====================

    /**
     * 后台咨询师分页：keyword 匹配姓名/擅长领域（中缀 LIKE，数据量小可接受），
     * status 按新枚举筛选，命中 idx_status_created。
     */
    public Map<String, Object> adminPage(Long page, Long size, String keyword, Integer status) {
        ensureAdmin();
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : 10L;
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        LambdaQueryWrapper<Consultant> wrapper = new LambdaQueryWrapper<Consultant>()
                .eq(status != null, Consultant::getStatus, status)
                .and(kw != null, w -> w.like(Consultant::getName, kw)
                        .or().like(Consultant::getSpecialties, kw))
                .orderByDesc(Consultant::getCreatedAt)
                .orderByDesc(Consultant::getId);
        IPage<Consultant> result = consultantMapper.selectPage(new Page<>(cur, sz), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords().stream().map(consultantConvert::toAdminListDTO).toList());
        data.put("total", result.getTotal());
        return data;
    }

    /** 状态 Tab 计数：{0:待审核,1:正常,2:禁用,total:全部}，缺数状态补 0 */
    public Map<String, Object> statusCounts() {
        ensureAdmin();
        Map<String, Object> counts = new LinkedHashMap<>();
        long total = 0;
        for (ConsultantStatus s : ConsultantStatus.values()) {
            counts.put(String.valueOf(s.getCode()), 0L);
        }
        for (Map<String, Object> row : consultantMapper.countGroupByStatus()) {
            Object st = row.get("status");
            Object cnt = row.get("cnt");
            if (st != null && cnt != null) {
                long n = ((Number) cnt).longValue();
                counts.put(String.valueOf(((Number) st).intValue()), n);
                total += n;
            }
        }
        counts.put("total", total);
        return counts;
    }

    /** 后台查看咨询师详情（任意状态，不读缓存，含资质/审核字段） */
    public ConsultantAdminDetailResponseDTO adminDetail(Long id) {
        ensureAdmin();
        return consultantConvert.toAdminDetailDTO(getEntityOrThrow(id));
    }

    /**
     * 新增/编辑咨询师。
     * 新增默认待审核(PENDING)，并在同一事务内创建 user_type=3 登录账号（初始禁用，审核通过才可登录），
     * 回填 consultant.user_id，一次性返回用户名/初始密码；
     * 编辑待审核（含已驳回）记录视为重新提交，清空驳回原因回到纯待审核。
     */
    @Transactional(rollbackFor = Exception.class)
    public ConsultantSaveResultDTO save(ConsultantSaveDTO dto) {
        ensureAdmin();
        assertImageUrls(dto);
        Long id;
        boolean wasNormal;
        ConsultantSaveResultDTO result;
        if (dto.getId() == null) {
            // 先建登录账号（用户名冲突等异常在插入咨询师前抛出），再回填 user_id
            UserService.ConsultantAccountCredential account =
                    userService.createConsultantAccount(dto.getUsername());
            Consultant entity = consultantConvert.toEntity(dto);
            entity.setUserId(account.userId());
            entity.setStatus(ConsultantStatus.PENDING.getCode());
            consultantMapper.insert(entity);
            id = entity.getId();
            wasNormal = false;
            result = ConsultantSaveResultDTO.of(id, account.username(), account.rawPassword());
            log.info("管理员操作咨询师, consultantId={}, action=create, accountUserId={}",
                    id, account.userId());
        } else {
            Consultant existing = getEntityOrThrow(dto.getId());
            wasNormal = Integer.valueOf(ConsultantStatus.NORMAL.getCode()).equals(existing.getStatus());
            consultantConvert.mergeUpdate(existing, dto);
            // 待审核/已驳回状态下编辑保存 = 重新提交，清空驳回原因，等待管理员再次审核
            if (ConsultantStatus.isReviewable(existing.getStatus())) {
                existing.setRejectReason(null);
            }
            consultantMapper.updateById(existing);
            id = existing.getId();
            result = ConsultantSaveResultDTO.of(id);
            log.info("管理员操作咨询师, consultantId={}, action=update", id);
        }
        // 仅正常状态会进用户端缓存；待审核/禁用记录无缓存可删
        if (wasNormal) {
            evictConsultantCache(id);
        }
        return result;
    }

    /**
     * 禁用/恢复：仅 NORMAL ↔ DISABLED 互转；待审核记录请先审核。
     * 禁用语义：用户端列表不可见、新预约不可选（预约创建经 getActiveEntity 拦截）；
     * 已确认预约不做任何改动（不破坏用户安排）。
     */
    public void updateStatus(Long id, Integer status) {
        ensureAdmin();
        ConsultantStatus target = ConsultantStatus.fromCode(status);
        if (target != ConsultantStatus.NORMAL && target != ConsultantStatus.DISABLED) {
            throw new BusinessException("仅支持正常/禁用两种状态操作");
        }
        Consultant existing = getEntityOrThrow(id);
        ConsultantStatus current = ConsultantStatus.fromCode(existing.getStatus());
        if (current == ConsultantStatus.PENDING) {
            throw new BusinessException("待审核咨询师请先完成审核");
        }
        if (current == target) {
            return;
        }
        existing.setStatus(target.getCode());
        consultantMapper.updateById(existing);
        evictConsultantCache(id);
        // 联动登录账号：禁用同步禁用并踢下线，恢复同步启用
        userService.updateAccountStatus(existing.getUserId(),
                target == ConsultantStatus.DISABLED
                        ? UserStatus.DISABLED.getCode() : UserStatus.NORMAL.getCode());
        log.info("管理员操作咨询师, consultantId={}, action={}", id,
                target == ConsultantStatus.DISABLED ? "disable" : "enable");
    }

    /**
     * 资质审核：仅待审核(PENDING)记录可审，重复审核拒绝。
     * 通过 → NORMAL + reviewedAt/joinedAt；驳回 → 保持 PENDING + rejectReason 必填。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminReview(Long id, ConsultantAuditDTO dto) {
        ensureAdmin();
        Consultant consultant = getEntityOrThrow(id);
        if (!ConsultantStatus.isReviewable(consultant.getStatus())) {
            throw new BusinessException("仅待审核的咨询师可审核");
        }
        boolean approve = Boolean.TRUE.equals(dto.getApprove());
        String reason = dto.getRejectReason() == null ? null : dto.getRejectReason().trim();
        LocalDateTime now = LocalDateTime.now();
        consultant.setReviewedAt(now);
        if (approve) {
            consultant.setStatus(ConsultantStatus.NORMAL.getCode());
            consultant.setRejectReason(null);
            if (consultant.getJoinedAt() == null) {
                consultant.setJoinedAt(now);
            }
            consultantMapper.updateById(consultant);
            evictConsultantCache(id);
            // 审核通过：启用登录账号（建号时为禁用态，审核前登录被拒）
            userService.updateAccountStatus(consultant.getUserId(), UserStatus.NORMAL.getCode());
            log.info("管理员操作咨询师, consultantId={}, action=review_approve", id);
        } else {
            if (reason == null || reason.isEmpty()) {
                throw new BusinessException("驳回时必须填写驳回原因");
            }
            consultant.setRejectReason(reason);
            consultantMapper.updateById(consultant);
            log.info("管理员操作咨询师, consultantId={}, action=review_reject", id);
        }
    }

    // ==================== 后台管理：详情 Tab（预约/评价/收入） ====================

    /** 咨询记录 Tab：按咨询师分页（含全部状态 + 用户昵称），懒加载 */
    public Map<String, Object> adminAppointments(Long consultantId, Long page, Long size) {
        ensureAdmin();
        getEntityOrThrow(consultantId);
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : 10L;
        IPage<AppointmentResponseDTO> result = appointmentMapper.selectAdminByConsultantIdPage(
                new Page<>(cur, sz), consultantId);
        result.getRecords().forEach(dto -> appointmentConvert.fillLabelsAndFlags(dto, false, LocalDateTime.now()));
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return data;
    }

    /** 评价列表 Tab：任意状态咨询师均可查看（管理端不走上架校验），懒加载 */
    public Map<String, Object> adminReviews(Long consultantId, Long page, Long size) {
        ensureAdmin();
        getEntityOrThrow(consultantId);
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : 10L;
        IPage<ConsultantReview> result = consultantReviewMapper.selectPage(new Page<>(cur, sz),
                new LambdaQueryWrapper<ConsultantReview>()
                        .eq(ConsultantReview::getConsultantId, consultantId)
                        .orderByDesc(ConsultantReview::getCreatedAt));
        return reviewPage(result);
    }

    /**
     * 收入统计（付费预留，纯内存计算，不缓存）：
     * 收入 = 已完成预约数 × price；price 未设置返回 null（前端展示「未设置」）。
     * ponytail: 接入真实支付后，将口径替换为支付流水实付金额汇总。
     */
    public ConsultantIncomeResponseDTO adminIncome(Long consultantId) {
        ensureAdmin();
        Consultant consultant = getEntityOrThrow(consultantId);
        long totalCompleted = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getConsultantId, consultantId)
                .eq(Appointment::getStatus, AppointmentStatus.COMPLETED.name()));
        long monthCompleted = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getConsultantId, consultantId)
                .eq(Appointment::getStatus, AppointmentStatus.COMPLETED.name())
                .ge(Appointment::getAppointmentDate, LocalDate.now().minusDays(29)));

        ConsultantIncomeResponseDTO dto = new ConsultantIncomeResponseDTO();
        dto.setTotalCompleted(totalCompleted);
        dto.setMonthCompleted(monthCompleted);
        dto.setAvgRating(consultant.getRating());
        dto.setTotalIncome(consultant.getPrice() == null
                ? null : consultant.getPrice().multiply(BigDecimal.valueOf(totalCompleted)));
        return dto;
    }

    // ==================== 排班（管理端 4.3 与咨询师端 5.3 共用同一张 appointment_slot 表） ====================

    /** 管理端批量生成时段（任意咨询师，ensureAdmin）；逻辑见 {@link #batchCreateSlotsInternal} */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> batchCreateSlots(Long consultantId, SlotBatchCreateDTO dto) {
        ensureAdmin();
        return batchCreateSlotsInternal(consultantId, dto);
    }

    /**
     * 咨询师端排班（与管理端共用同一张 appointment_slot 表、同一套生成逻辑，不复制代码）：
     * consultantId 由调用方限定为当前登录咨询师 id，禁止传他人 id。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> batchCreateSlotsOwn(Long consultantId, SlotBatchCreateDTO dto) {
        return batchCreateSlotsInternal(consultantId, dto);
    }

    /**
     * 批量生成时段内部逻辑（管理端/咨询师端共用）：date=按日批量；weekStart=周模板批量（归一到周一，
     * 每日模板应用整周）。已存在时段（uk_consultant_slot）与模板内重复开始时间一律跳过，
     * 返回 created/skipped 计数。过去日期不生成（周模板中跳过，按日模式直接拒绝）。
     */
    private Map<String, Integer> batchCreateSlotsInternal(Long consultantId, SlotBatchCreateDTO dto) {
        getEntityOrThrow(consultantId);
        boolean dayMode = dto.getDate() != null;
        boolean weekMode = dto.getWeekStart() != null;
        if (dayMode == weekMode) {
            throw new BusinessException("date 与 weekStart 必须且只能传一个");
        }

        // 模板校验：结束晚于开始；按开始时间去重（同一开始时间只保留一条）
        Map<java.time.LocalTime, SlotBatchCreateDTO.SlotTime> template = new LinkedHashMap<>();
        for (SlotBatchCreateDTO.SlotTime item : dto.getSlots()) {
            if (!item.getEndTime().isAfter(item.getStartTime())) {
                throw new BusinessException("时段结束时间必须晚于开始时间");
            }
            template.putIfAbsent(item.getStartTime(), item);
        }

        List<LocalDate> dates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        if (dayMode) {
            if (dto.getDate().isBefore(today)) {
                throw new BusinessException("不能为过去的日期排班");
            }
            dates.add(dto.getDate());
        } else {
            LocalDate monday = toWeekStart(dto.getWeekStart());
            for (int i = 0; i < 7; i++) {
                LocalDate d = monday.plusDays(i);
                if (!d.isBefore(today)) {
                    dates.add(d);
                }
            }
            if (dates.isEmpty()) {
                throw new BusinessException("所选周已全部过期，无法排班");
            }
        }

        // 一次查回目标日期范围全部已存在时段，构造 (date,startTime) 去重集合，禁止逐日/逐条查库
        Set<String> existingKeys = loadSlotKeys(consultantId, dates.get(0), dates.get(dates.size() - 1));
        Set<String> batchKeys = new HashSet<>();
        int created = 0;
        int skipped = 0;
        for (LocalDate d : dates) {
            for (SlotBatchCreateDTO.SlotTime item : template.values()) {
                String key = slotKey(d, item.getStartTime());
                if (!existingKeys.add(key) || !batchKeys.add(key)) {
                    skipped++;
                    continue;
                }
                appointmentSlotMapper.insert(AppointmentSlot.builder()
                        .consultantId(consultantId)
                        .slotDate(d)
                        .startTime(item.getStartTime())
                        .endTime(item.getEndTime())
                        .status(SLOT_FREE)
                        .build());
                created++;
            }
        }
        log.info("咨询师排班: consultantId={}, days={}, created={}, skipped={}", consultantId, dates.size(), created, skipped);
        return Map.of("created", created, "skipped", skipped);
    }

    /** 后台查看某日全部时段（含已约/停用） */
    public List<SlotResponseDTO> adminSlots(Long consultantId, LocalDate date) {
        ensureAdmin();
        LocalDate target = date != null ? date : LocalDate.now();
        return listSlots(consultantId, target, target);
    }

    /**
     * 周视图（管理员）：weekStart 传周内任意一天，归一到周一，一次 BETWEEN 查询返回整周 7 天时段，
     * 前端按 slotDate 分组渲染网格。
     */
    public List<SlotResponseDTO> weekSlots(Long consultantId, LocalDate weekStart) {
        ensureAdmin();
        return weekSlotsInternal(consultantId, weekStart);
    }

    /**
     * 周视图（咨询师端）：不校验管理员，consultantId 由调用方限定为当前登录咨询师，
     * 禁止传入他人 id（Controller 必须传 CurrentUserUtils.getCurrentConsultantId()）。
     */
    public List<SlotResponseDTO> weekSlotsForConsultant(Long consultantId, LocalDate weekStart) {
        getEntityOrThrow(consultantId);
        return weekSlotsInternal(consultantId, weekStart);
    }

    private List<SlotResponseDTO> weekSlotsInternal(Long consultantId, LocalDate weekStart) {
        LocalDate monday = toWeekStart(weekStart != null ? weekStart : LocalDate.now());
        return listSlots(consultantId, monday, monday.plusDays(6));
    }

    /** 管理端单时段停用/恢复（任意咨询师，ensureAdmin） */
    public void updateSlotStatus(Long slotId, Integer status) {
        ensureAdmin();
        updateSlotStatusInternal(slotId, status, null);
    }

    /**
     * 咨询师端单时段停用/恢复：仅可操作本人时段，越权抛 A0301。
     */
    public void updateSlotStatusOwn(Long slotId, Integer status, Long operatorConsultantId) {
        updateSlotStatusInternal(slotId, status, operatorConsultantId);
    }

    /**
     * 单时段停用/恢复内部逻辑（两端共用）：仅 可约(0)↔停用(2) 可操作；已约(1)拒绝，
     * 防止破坏已确认预约。operatorConsultantId 非空时强制归属校验（咨询师端），null=管理端。
     */
    private void updateSlotStatusInternal(Long slotId, Integer status, Long operatorConsultantId) {
        if (status == null || (status != SLOT_FREE && status != SLOT_DISABLED)) {
            throw new BusinessException("时段状态仅支持 0-可约 或 2-停用");
        }
        AppointmentSlot slot = appointmentSlotMapper.selectById(slotId);
        if (slot == null) {
            throw new BusinessException("排班时段不存在");
        }
        if (operatorConsultantId != null
                && !operatorConsultantId.equals(slot.getConsultantId())) {
            throw new BusinessException("A0301", "无权操作他人排班时段", null);
        }
        if (Integer.valueOf(SLOT_BOOKED).equals(slot.getStatus())) {
            throw new BusinessException("已被预约的时段不可停用或恢复");
        }
        if (slot.getStatus().equals(status)) {
            return;
        }
        slot.setStatus(status);
        appointmentSlotMapper.updateById(slot);
        log.info("排班时段状态变更: slotId={}, consultantId={}, operator={}, action={}",
                slotId, slot.getConsultantId(),
                operatorConsultantId != null ? "consultant" : "admin",
                status == SLOT_DISABLED ? "slot_disable" : "slot_enable");
    }

    /**
     * 删除排班时段：仅 可约(0)/停用(2) 可删，已约(1) 拒绝（防破坏已确认预约）。
     * operatorConsultantId 非空时强制归属校验（咨询师端），null=管理端。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSlot(Long slotId, Long operatorConsultantId) {
        AppointmentSlot slot = appointmentSlotMapper.selectById(slotId);
        if (slot == null) {
            throw new BusinessException("排班时段不存在");
        }
        if (operatorConsultantId != null
                && !operatorConsultantId.equals(slot.getConsultantId())) {
            throw new BusinessException("A0301", "无权操作他人排班时段", null);
        }
        if (Integer.valueOf(SLOT_BOOKED).equals(slot.getStatus())) {
            throw new BusinessException("已被预约的时段不可删除");
        }
        appointmentSlotMapper.deleteById(slotId);
        log.info("删除排班时段: slotId={}, consultantId={}, operator={}",
                slotId, slot.getConsultantId(),
                operatorConsultantId != null ? "consultant" : "admin");
    }

    // ==================== 缓存 ====================

    /** 状态变更/评分变动后调用：删详情 + 全部分页列表缓存 */
    public void evictConsultantCache(Long consultantId) {
        evictDetailCache(consultantId);
        evictAllListCache();
    }

    private ConsultantDetailResponseDTO readDetailCache(Long id) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisConstants.buildConsultantDetailKey(id));
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached, ConsultantDetailResponseDTO.class);
            }
        } catch (Exception e) {
            log.warn("咨询师详情缓存读取异常，回源DB: id={}, err={}", id, e.getMessage());
        }
        return null;
    }

    private void writeDetailCache(Long id, ConsultantDetailResponseDTO dto) {
        try {
            stringRedisTemplate.opsForValue().set(RedisConstants.buildConsultantDetailKey(id),
                    objectMapper.writeValueAsString(dto),
                    RedisConstants.CONSULTANT_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("咨询师详情缓存写入失败（fail-open）: id={}, err={}", id, e.getMessage());
        }
    }

    private void evictDetailCache(Long id) {
        try {
            stringRedisTemplate.delete(RedisConstants.buildConsultantDetailKey(id));
        } catch (Exception e) {
            log.warn("删除咨询师详情缓存失败（fail-open）: id={}, err={}", id, e.getMessage());
        }
    }

    private Map<String, Object> readListCache(String cacheKey) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached == null || cached.isBlank()) {
                return null;
            }
            Map<String, Object> raw = objectMapper.readValue(cached, new TypeReference<Map<String, Object>>() {
            });
            List<ConsultantListResponseDTO> records = objectMapper.convertValue(raw.get("records"),
                    new TypeReference<List<ConsultantListResponseDTO>>() {
                    });
            Map<String, Object> data = new HashMap<>();
            data.put("records", records);
            data.put("total", raw.get("total"));
            return data;
        } catch (Exception e) {
            log.warn("咨询师列表缓存读取异常，回源DB: key={}, err={}", cacheKey, e.getMessage());
            return null;
        }
    }

    private void writeListCache(String cacheKey, Map<String, Object> data) {
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(data),
                    RedisConstants.CONSULTANT_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("咨询师列表缓存写入失败（fail-open）: key={}, err={}", cacheKey, e.getMessage());
        }
    }

    /** 删除全部分页列表缓存：specialty/page/size 维度多，SCAN 后批量删 */
    private void evictAllListCache() {
        try (Cursor<String> cursor = stringRedisTemplate.scan(ScanOptions.scanOptions()
                .match(RedisConstants.CONSULTANT_LIST_PREFIX + "*").count(200).build())) {
            List<String> keys = new ArrayList<>();
            cursor.forEachRemaining(keys::add);
            if (!keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("批量删除咨询师列表缓存失败（5min TTL 兜底）: err={}", e.getMessage());
        }
    }

    // ==================== 内部方法 ====================

    /** 取正常状态咨询师实体，不存在/待审核/禁用抛业务异常（用户端与预约创建共用此信任边界） */
    public Consultant getActiveEntity(Long id) {
        Consultant consultant = consultantMapper.selectById(id);
        if (consultant == null
                || consultant.getStatus() == null
                || consultant.getStatus() != ConsultantStatus.NORMAL.getCode()) {
            throw new BusinessException("咨询师不存在或已停诊");
        }
        return consultant;
    }

    private Consultant getEntityOrThrow(Long id) {
        Consultant consultant = consultantMapper.selectById(id);
        if (consultant == null) {
            throw new BusinessException("咨询师不存在");
        }
        return consultant;
    }

    /** 评价分页结果组装（用户端/管理端共用） */
    private Map<String, Object> reviewPage(IPage<ConsultantReview> result) {
        List<ReviewResponseDTO> records = result.getRecords().stream()
                .map(consultantConvert::toReviewDTO).toList();
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        return data;
    }

    /** 时段范围查询（含全部状态），按日期/开始时间升序 */
    private List<SlotResponseDTO> listSlots(Long consultantId, LocalDate from, LocalDate to) {
        return appointmentSlotMapper.selectList(new LambdaQueryWrapper<AppointmentSlot>()
                        .eq(AppointmentSlot::getConsultantId, consultantId)
                        .between(AppointmentSlot::getSlotDate, from, to)
                        .orderByAsc(AppointmentSlot::getSlotDate)
                        .orderByAsc(AppointmentSlot::getStartTime)).stream()
                .map(consultantConvert::toSlotDTO).toList();
    }

    /** 一次查回日期范围内全部已存在时段的 (date,startTime) 键集合 */
    private Set<String> loadSlotKeys(Long consultantId, LocalDate from, LocalDate to) {
        Set<String> keys = new HashSet<>();
        appointmentSlotMapper.selectList(new LambdaQueryWrapper<AppointmentSlot>()
                        .select(AppointmentSlot::getSlotDate, AppointmentSlot::getStartTime)
                        .eq(AppointmentSlot::getConsultantId, consultantId)
                        .between(AppointmentSlot::getSlotDate, from, to))
                .forEach(s -> keys.add(slotKey(s.getSlotDate(), s.getStartTime())));
        return keys;
    }

    private String slotKey(LocalDate date, java.time.LocalTime startTime) {
        return date + "|" + startTime;
    }

    /** 归一到 ISO 周一（Monday=1），周视图与周模板共用 */
    static LocalDate toWeekStart(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    /**
     * 信任边界校验：头像与资质图片只接受本站 OSS 上传产物（与 FileService 拼 URL 规则一致），
     * 禁止任意外部 URL 注入。
     */
    private void assertImageUrls(ConsultantSaveDTO dto) {
        String prefix = "https://" + ossProperties.getBucketName() + "."
                + ossProperties.getEndpoint().replaceFirst("^https?://", "") + "/";
        if (dto.getAvatar() != null && !dto.getAvatar().isBlank()
                && !dto.getAvatar().trim().startsWith(prefix)) {
            throw new BusinessException("头像地址非法，请使用上传后的图片地址");
        }
        if (dto.getQualificationImages() != null) {
            if (dto.getQualificationImages().size() > MAX_QUALIFICATION_IMAGES) {
                throw new BusinessException("资质证明图片最多" + MAX_QUALIFICATION_IMAGES + "张");
            }
            for (String url : dto.getQualificationImages()) {
                if (url == null || url.isBlank() || !url.trim().startsWith(prefix)) {
                    throw new BusinessException("资质证明图片地址非法，请重新上传");
                }
            }
        }
    }

    private void ensureAdmin() {
        if (!CurrentUserUtils.isAdmin()) {
            throw new BusinessException("A0301", "无管理员权限", null);
        }
    }
}
