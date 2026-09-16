package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.request.ConsultationRecordSaveDTO;
import com.cc.dto.response.AppointmentMessageResponseDTO;
import com.cc.dto.response.ConsultantSessionResponseDTO;
import com.cc.dto.response.ConsultationRecordResponseDTO;
import com.cc.dto.response.SessionDetailResponseDTO;
import com.cc.entity.Appointment;
import com.cc.entity.ConsultationRecord;
import com.cc.entity.User;
import com.cc.enums.AppointmentStatus;
import com.cc.exception.BusinessException;
import com.cc.mapper.AppointmentMapper;
import com.cc.mapper.ConsultationRecordMapper;
import com.cc.mapper.UserMapper;
import com.cc.service.convert.AppointmentConvert;
import com.cc.util.CurrentUserUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 咨询师端「咨询会话管理」业务层：会话=预约，零新会话表。
 *
 * <p>信任边界：所有按 appointmentId 操作的接口第一行调用
 * {@link #ensureConsultantOfAppointment(Long)} 校验该预约属于当前咨询师，
 * 通过后才允许查该来访者数据——这是防任意用户数据泄露的核心闸门。</p>
 *
 * <p>画像隐私边界：测评等级/日记情绪为敏感数据，仅当"该用户确实约过你"才可见；
 * 出参只含列表摘要（量表/时间/等级、日记日期/情绪/评分），不返回测评完整报告与日记全文，
 * 完整报告归用户本人。手机号/邮箱永不返回。</p>
 *
 * <p>可见性：咨询记录咨询师本人读写 + 管理员只读；用户端无任何接口暴露。</p>
 *
 * <p>复用：在线沟通复用 {@link AppointmentMessageService}（save+pageHistory，senderType=2 咨询师），
 * WS 端点不变；测评/日记复用 {@link AssessmentService#listRecordsForAdmin} /
 * {@link EmotionDiaryService#pageForAdminByUserId}，本服务仅做归属前置校验。</p>
 *
 * @author cc
 * @date 2026-09-17
 */
@Slf4j
@Service
public class ConsultantSessionService {

    private static final long MAX_PAGE_SIZE = 20L;
    /** 画像接口：日记内容摘要截断长度 */
    private static final int DIARY_SUMMARY_LEN = 100;
    /** 会话列表：最后留言摘要截断长度 */
    private static final int LAST_MSG_SUMMARY_LEN = 50;

    @Resource
    private AppointmentMapper appointmentMapper;
    @Resource
    private ConsultationRecordMapper consultationRecordMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private AppointmentMessageService appointmentMessageService;
    @Resource
    private AssessmentService assessmentService;
    @Resource
    private EmotionDiaryService emotionDiaryService;
    @Resource
    private AppointmentConvert appointmentConvert;

    // ==================== 核心闸门：归属校验 ====================

    /**
     * 校验当前咨询师拥有该预约：预约存在且 consultantId == 当前咨询师。
     * 所有 session 接口第一行调用，禁止跨咨询师访问他人来访者数据。
     *
     * @return 通过校验的预约实体
     */
    private Appointment ensureConsultantOfAppointment(Long appointmentId) {
        if (appointmentId == null || appointmentId <= 0) {
            throw new BusinessException("预约ID不正确");
        }
        Long consultantId = CurrentUserUtils.getCurrentConsultantId();
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null || !consultantId.equals(appointment.getConsultantId())) {
            throw new BusinessException("无权查看该来访者信息");
        }
        return appointment;
    }

    // ==================== 会话列表 ====================

    /**
     * 会话列表=当前咨询师名下预约分页：聚合最后一条留言摘要+留言数，一次查询禁止 N+1。
     * status 状态码（PENDING/CONFIRMED/COMPLETED/CANCELED）；keyword 匹配用户昵称。
     */
    public Map<String, Object> sessions(String status, String keyword, Long page, Long size) {
        Long consultantId = CurrentUserUtils.getCurrentConsultantId();
        if (status != null && !status.isBlank()) {
            // 非法状态直接抛业务异常，不带入 SQL
            AppointmentStatus.fromCode(status.trim());
        }
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : MAX_PAGE_SIZE;

        IPage<ConsultantSessionResponseDTO> result = appointmentMapper.selectConsultantSessions(
                new Page<>(cur, sz), consultantId,
                status != null && !status.isBlank() ? status.trim() : null,
                keyword != null && !keyword.isBlank() ? keyword.trim() : null);

        // 状态标签 + 最后留言摘要截断（隐私边界：列表只露摘要）
        result.getRecords().forEach(dto -> {
            dto.setStatusLabel(AppointmentStatus.fromCode(dto.getStatus()).getLabel());
            dto.setLastMessageContent(truncate(dto.getLastMessageContent(), LAST_MSG_SUMMARY_LEN));
        });

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return data;
    }

    // ==================== 会话详情 ====================

    /**
     * 会话详情聚合：预约信息 + 来访者基本信息（昵称/头像，不含手机号/邮箱）+ 咨询记录（如有）。
     * 留言/测评/日记/历史记录走各自分页子接口，本 DTO 不内嵌。
     */
    public SessionDetailResponseDTO sessionDetail(Long appointmentId) {
        Appointment appointment = ensureConsultantOfAppointment(appointmentId);
        User user = userMapper.selectById(appointment.getUserId());
        ConsultationRecord record = consultationRecordMapper.selectOne(
                new LambdaQueryWrapper<ConsultationRecord>()
                        .eq(ConsultationRecord::getAppointmentId, appointmentId));

        SessionDetailResponseDTO dto = new SessionDetailResponseDTO();
        dto.setAppointmentId(appointment.getId());
        dto.setUserId(appointment.getUserId());
        dto.setUserNickname(resolveNickname(user));
        dto.setUserAvatar(user != null ? user.getAvatar() : null);
        dto.setAppointmentDate(appointment.getAppointmentDate());
        dto.setStartTime(appointment.getStartTime().toString());
        dto.setEndTime(appointment.getEndTime().toString());
        dto.setStatus(appointment.getStatus());
        dto.setStatusLabel(AppointmentStatus.fromCode(appointment.getStatus()).getLabel());
        dto.setProblemDesc(appointment.getProblemDesc());
        dto.setRecord(appointmentConvert.toRecordDTO(record));
        return dto;
    }

    // ==================== 用户画像（前置归属校验） ====================

    /**
     * 用户测评记录（复用 AssessmentService.listRecordsForAdmin，前置归属校验）。
     * AssessmentRecordResponseDTO 已不含报告全文，可直接用——画像接口只出列表摘要。
     */
    public Map<String, Object> assessments(Long appointmentId, Long page, Long size) {
        Appointment appointment = ensureConsultantOfAppointment(appointmentId);
        return assessmentService.listRecordsForAdmin(appointment.getUserId(),
                page != null && page > 0 ? page : 1L,
                size != null && size > 0 ? Math.min(size, MAX_PAGE_SIZE) : MAX_PAGE_SIZE);
    }

    /**
     * 用户情绪日记（复用 EmotionDiaryService.pageForAdminByUserId，前置归属校验）。
     * 隐私边界：DTO 原含 diaryContent 全文 + aiEmotionAnalysis，本接口截断为摘要（前 100 字），
     * 清空 aiEmotionAnalysis——完整日记归用户本人，咨询师只看辅助决策的摘要。
     */
    public Map<String, Object> diaries(Long appointmentId, Long page, Long size) {
        Appointment appointment = ensureConsultantOfAppointment(appointmentId);
        long cur = page != null && page > 0 ? page : 1L;
        long sz = size != null && size > 0 ? Math.min(size, MAX_PAGE_SIZE) : MAX_PAGE_SIZE;
        IPage<com.cc.dto.response.EmotionDiaryResponseDTO> result =
                emotionDiaryService.pageForAdminByUserId(appointment.getUserId(), cur, sz);
        // 截断为摘要（隐私边界）
        result.getRecords().forEach(dto -> {
            dto.setDiaryContent(truncate(dto.getDiaryContent(), DIARY_SUMMARY_LEN));
            dto.setAiEmotionAnalysis(null);
        });
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return data;
    }

    /**
     * 该来访者历史咨询记录（consultation_record 按 user_id，当前咨询师名下）。
     * 含当前预约记录；按更新时间倒序分页。
     */
    public Map<String, Object> historyRecords(Long appointmentId, Long page, Long size) {
        Appointment appointment = ensureConsultantOfAppointment(appointmentId);
        long cur = page != null && page > 0 ? page : 1L;
        long sz = size != null && size > 0 ? Math.min(size, MAX_PAGE_SIZE) : MAX_PAGE_SIZE;
        Long consultantId = CurrentUserUtils.getCurrentConsultantId();

        IPage<ConsultationRecord> result = consultationRecordMapper.selectPage(new Page<>(cur, sz),
                new LambdaQueryWrapper<ConsultationRecord>()
                        .eq(ConsultationRecord::getUserId, appointment.getUserId())
                        .eq(ConsultationRecord::getConsultantId, consultantId)
                        .orderByDesc(ConsultationRecord::getUpdatedAt)
                        .orderByDesc(ConsultationRecord::getId));
        List<ConsultationRecordResponseDTO> records = result.getRecords().stream()
                .map(appointmentConvert::toRecordDTO).toList();
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        return data;
    }

    // ==================== 在线沟通（复用 3.7 留言板） ====================

    /**
     * 留言列表（复用 AppointmentMessageService.pageHistory，前置归属校验）。
     * WS 端点不变（握手 token 校验已按登录态区分角色）。
     */
    public Map<String, Object> messages(Long appointmentId, Long page, Long size) {
        ensureConsultantOfAppointment(appointmentId);
        return appointmentMessageService.pageHistory(appointmentId, page, size);
    }

    /**
     * 发送留言（复用 AppointmentMessageService.save，senderType=2 咨询师）。
     * 调用前归属校验防越权；日志只记 appointmentId+动作，不含留言内容。
     */
    public AppointmentMessageResponseDTO sendMessage(Long appointmentId, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("留言内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException("留言内容不能超过500字");
        }
        Appointment appointment = ensureConsultantOfAppointment(appointmentId);
        log.info("咨询师发送会话留言: appointmentId={}", appointmentId);
        return appointmentMessageService.save(appointment,
                CurrentUserUtils.getCurrentUserId(), 2, content);
    }

    // ==================== 咨询记录（一单一记录，upsert） ====================

    /**
     * 咨询记录（无则返回 null，前端展示空模板，不报错）。
     */
    public ConsultationRecordResponseDTO getRecord(Long appointmentId) {
        ensureConsultantOfAppointment(appointmentId);
        ConsultationRecord record = consultationRecordMapper.selectOne(
                new LambdaQueryWrapper<ConsultationRecord>()
                        .eq(ConsultationRecord::getAppointmentId, appointmentId));
        return appointmentConvert.toRecordDTO(record);
    }

    /**
     * 保存咨询记录（upsert：按 appointmentId 存在则更新，否则插入）。
     * 字段长度由 DTO @Size 校验；userId/consultantId 取自预约，防伪造。
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveRecord(Long appointmentId, ConsultationRecordSaveDTO dto) {
        Appointment appointment = ensureConsultantOfAppointment(appointmentId);
        ConsultationRecord existing = consultationRecordMapper.selectOne(
                new LambdaQueryWrapper<ConsultationRecord>()
                        .eq(ConsultationRecord::getAppointmentId, appointmentId));
        if (existing == null) {
            ConsultationRecord record = ConsultationRecord.builder()
                    .appointmentId(appointmentId)
                    .userId(appointment.getUserId())
                    .consultantId(appointment.getConsultantId())
                    .chiefComplaint(dto.getChiefComplaint())
                    .analysis(dto.getAnalysis())
                    .interventionPlan(dto.getInterventionPlan())
                    .nextSuggestion(dto.getNextSuggestion())
                    .build();
            consultationRecordMapper.insert(record);
        } else {
            existing.setChiefComplaint(dto.getChiefComplaint());
            existing.setAnalysis(dto.getAnalysis());
            existing.setInterventionPlan(dto.getInterventionPlan());
            existing.setNextSuggestion(dto.getNextSuggestion());
            consultationRecordMapper.updateById(existing);
        }
        log.info("咨询师保存咨询记录: appointmentId={}", appointmentId);
    }

    // ==================== 管理员只读联动（4.8） ====================

    /**
     * 管理员只读查询咨询记录（4.8 预约详情联动展示）。
     * 仅管理员可调（调用方需 ensureAdmin）；无记录返回 null，不报错。
     */
    public ConsultationRecordResponseDTO getRecordForAdmin(Long appointmentId) {
        if (!CurrentUserUtils.isAdmin()) {
            throw new BusinessException("A0301", "无管理员权限", null);
        }
        if (appointmentId == null || appointmentId <= 0) {
            throw new BusinessException("预约ID不正确");
        }
        ConsultationRecord record = consultationRecordMapper.selectOne(
                new LambdaQueryWrapper<ConsultationRecord>()
                        .eq(ConsultationRecord::getAppointmentId, appointmentId));
        return appointmentConvert.toRecordDTO(record);
    }

    // ==================== 工具 ====================

    private String resolveNickname(User user) {
        if (user == null) {
            return null;
        }
        return (user.getNickname() == null || user.getNickname().isBlank())
                ? user.getUsername() : user.getNickname();
    }

    private String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "…";
    }
}
