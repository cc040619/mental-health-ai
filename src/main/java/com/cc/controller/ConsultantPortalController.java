package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.AppointmentCancelDTO;
import com.cc.dto.request.AppointmentStatusDTO;
import com.cc.dto.request.ArticleCreateDTO;
import com.cc.dto.request.ConsultantReviewReplyDTO;
import com.cc.dto.request.ConsultantSettingsDTO;
import com.cc.dto.request.ConsultationRecordSaveDTO;
import com.cc.dto.request.SlotBatchCreateDTO;
import com.cc.dto.request.SlotStatusDTO;
import com.cc.dto.response.AppointmentMessageResponseDTO;
import com.cc.dto.response.CategoryResponseDTO;
import com.cc.dto.response.ConsultationRecordResponseDTO;
import com.cc.dto.response.ConsultantSettingsResponseDTO;
import com.cc.dto.response.SessionDetailResponseDTO;
import com.cc.dto.response.SlotResponseDTO;
import com.cc.dto.response.WorkbenchOverviewResponseDTO;
import com.cc.dto.response.WorkbenchTodosResponseDTO;
import com.cc.service.ConsultantSessionService;
import com.cc.service.ConsultantWorkbenchService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 咨询师端控制器（/api/consultant/**）：工作台/我的预约/日程/评价回复/发文 + 咨询会话管理。
 *
 * <p>鉴权：SecurityConfig 统一要求登录态；角色（user_type=3）+ 咨询师绑定校验在
 * Service 层 {@code CurrentUserUtils.getCurrentConsultantId()} 完成（与 /api/admin 同模式），
 * 所有数据按当前咨询师 id 过滤，禁止跨咨询师访问。</p>
 *
 * <p>会话管理安全闸门：所有按 appointmentId 操作的接口第一行调用
 * {@code ConsultantSessionService.ensureConsultantOfAppointment} 校验该预约属于当前咨询师，
 * 通过后才允许查该来访者数据；越权抛 BusinessException「无权查看该来访者信息」。</p>
 *
 * @author cc
 * @date 2026-09-17
 */
@RestController
@RequestMapping("/api/consultant")
public class ConsultantPortalController {

    @Resource
    private ConsultantWorkbenchService workbenchService;
    @Resource
    private ConsultantSessionService sessionService;

    /** 工作台 - 数据概览 */
    @GetMapping("/workbench/overview")
    public Result<WorkbenchOverviewResponseDTO> overview() {
        return Result.ok(workbenchService.overview());
    }

    /** 工作台 - 待办三组（即将开始/未回复评价/待处理预约，各 ≤5） */
    @GetMapping("/workbench/todos")
    public Result<WorkbenchTodosResponseDTO> todos() {
        return Result.ok(workbenchService.todos());
    }

    /** 我的预约分页：status 状态码，date=yyyy-MM-dd 精确日期 */
    @GetMapping("/appointments")
    public Result<Map<String, Object>> appointments(@RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String date,
                                                    @RequestParam(required = false) Long page,
                                                    @RequestParam(required = false) Long size) {
        return Result.ok(workbenchService.appointments(status, date, page, size));
    }

    /** 确认（PENDING→CONFIRMED）/ 完成（CONFIRMED→COMPLETED）自己的预约；取消走专用接口 */
    @PutMapping("/appointments/{id}/status")
    public Result<Void> changeAppointmentStatus(@PathVariable Long id,
                                                @Valid @RequestBody AppointmentStatusDTO dto) {
        workbenchService.changeAppointmentStatus(id, dto.getStatus());
        return Result.ok();
    }

    /**
     * 取消自己的预约（管理特权：不受用户取消时限约束，原因必填 ≤500 字）；
     * PENDING/CONFIRMED → CANCELED，释放时段并通知来访者（通知含取消原因）。
     */
    @PutMapping("/appointments/{id}/cancel")
    public Result<Void> cancelAppointment(@PathVariable Long id,
                                          @Valid @RequestBody AppointmentCancelDTO dto) {
        workbenchService.cancelAppointment(id, dto.getReason());
        return Result.ok();
    }

    /** 日程周视图（可编辑数据源）：weekStart 传周内任意一天，后端归一到周一，一次返回整周时段 */
    @GetMapping("/schedule")
    public Result<List<SlotResponseDTO>> schedule(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return Result.ok(workbenchService.schedule(weekStart));
    }

    /** 批量生成排班：按日 {date, slots} 或周模板 {weekStart, slots}（已存在跳过，返回 created/skipped） */
    @PostMapping("/schedule/slots")
    public Result<Map<String, Integer>> createSlots(@Valid @RequestBody SlotBatchCreateDTO dto) {
        return Result.ok(workbenchService.createSlots(dto));
    }

    /** 单时段停用/恢复（0可约 2停用；仅自己的时段，已约时段 Service 拒绝） */
    @PutMapping("/schedule/slots/{slotId}/status")
    public Result<Void> updateSlotStatus(@PathVariable Long slotId,
                                         @Valid @RequestBody SlotStatusDTO dto) {
        workbenchService.updateSlotStatus(slotId, dto.getStatus());
        return Result.ok();
    }

    /** 删除单时段（仅自己的时段；0可约/2停用可删，已约拒绝） */
    @DeleteMapping("/schedule/slots/{slotId}")
    public Result<Void> deleteSlot(@PathVariable Long slotId) {
        workbenchService.deleteSlot(slotId);
        return Result.ok();
    }

    /** 我的预约设置：咨询价格（空=不收费）/ 可预约天数 1-90 / 用户取消时限 0-48 小时 */
    @GetMapping("/settings")
    public Result<ConsultantSettingsResponseDTO> getSettings() {
        return Result.ok(workbenchService.getSettings());
    }

    @PutMapping("/settings")
    public Result<ConsultantSettingsResponseDTO> updateSettings(@Valid @RequestBody ConsultantSettingsDTO dto) {
        return Result.ok(workbenchService.updateSettings(dto));
    }

    /** 我的评价分页（仅昵称，不含手机号/邮箱） */
    @GetMapping("/reviews")
    public Result<Map<String, Object>> reviews(@RequestParam(required = false) Long page,
                                               @RequestParam(required = false) Long size) {
        return Result.ok(workbenchService.reviews(page, size));
    }

    /** 回复自己的评价（覆盖更新，≤500 字） */
    @PutMapping("/reviews/{id}/reply")
    public Result<Void> replyReview(@PathVariable Long id,
                                    @Valid @RequestBody ConsultantReviewReplyDTO dto) {
        workbenchService.replyReview(id, dto.getContent());
        return Result.ok();
    }

    /** 发文分类列表（复用启用分类树） */
    @GetMapping("/article/categories")
    public Result<List<CategoryResponseDTO>> articleCategories() {
        return Result.ok(workbenchService.articleCategories());
    }

    /** 发布文章（默认 status=0 草稿，管理员上架后用户端可见） */
    @PostMapping("/article")
    public Result<String> publishArticle(@Valid @RequestBody ArticleCreateDTO dto) {
        return Result.ok(workbenchService.publishArticle(dto));
    }

    // ==================== 咨询会话管理 ====================
    // 会话=预约：列表/详情/画像/留言/记录 全部前置 ensureConsultantOfAppointment 归属校验。
    // 画像出参刻意截断：测评/日记只给摘要，完整报告归来访者本人；手机号/邮箱永不返回。

    /** 会话列表=当前咨询师名下预约：状态筛选/昵称搜索，聚合最后留言摘要+留言数 */
    @GetMapping("/sessions")
    public Result<Map<String, Object>> sessions(@RequestParam(required = false) String status,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long page,
                                                @RequestParam(required = false) Long size) {
        return Result.ok(sessionService.sessions(status, keyword, page, size));
    }

    /** 会话详情：预约信息+来访者基本信息（昵称/头像，不含手机号邮箱）+咨询记录 */
    @GetMapping("/sessions/{appointmentId}")
    public Result<SessionDetailResponseDTO> sessionDetail(@PathVariable Long appointmentId) {
        return Result.ok(sessionService.sessionDetail(appointmentId));
    }

    /** 来访者测评记录摘要（复用 AssessmentService.listRecordsForAdmin，前置归属校验） */
    @GetMapping("/sessions/{appointmentId}/assessments")
    public Result<Map<String, Object>> sessionAssessments(@PathVariable Long appointmentId,
                                                         @RequestParam(required = false) Long page,
                                                         @RequestParam(required = false) Long size) {
        return Result.ok(sessionService.assessments(appointmentId, page, size));
    }

    /** 来访者情绪日记摘要（复用 pageForAdminByUserId，截断为摘要，不返回全文/AI分析） */
    @GetMapping("/sessions/{appointmentId}/diaries")
    public Result<Map<String, Object>> sessionDiaries(@PathVariable Long appointmentId,
                                                     @RequestParam(required = false) Long page,
                                                     @RequestParam(required = false) Long size) {
        return Result.ok(sessionService.diaries(appointmentId, page, size));
    }

    /** 来访者历史咨询记录（按 user_id 查当前咨询师名下，含当前预约记录） */
    @GetMapping("/sessions/{appointmentId}/history-records")
    public Result<Map<String, Object>> sessionHistoryRecords(@PathVariable Long appointmentId,
                                                             @RequestParam(required = false) Long page,
                                                             @RequestParam(required = false) Long size) {
        return Result.ok(sessionService.historyRecords(appointmentId, page, size));
    }

    /** 留言列表（复用 AppointmentMessageService.pageHistory，前置归属校验） */
    @GetMapping("/sessions/{appointmentId}/messages")
    public Result<Map<String, Object>> sessionMessages(@PathVariable Long appointmentId,
                                                       @RequestParam(required = false) Long page,
                                                       @RequestParam(required = false) Long size) {
        return Result.ok(sessionService.messages(appointmentId, page, size));
    }

    /** 发送留言（复用 save：senderType=2 咨询师，前置归属校验防越权） */
    @PostMapping("/sessions/{appointmentId}/messages")
    public Result<AppointmentMessageResponseDTO> sendSessionMessage(@PathVariable Long appointmentId,
                                                                   @RequestBody Map<String, String> body) {
        String content = body == null ? null : body.get("content");
        return Result.ok(sessionService.sendMessage(appointmentId, content));
    }

    /** 咨询记录（无则返回空模板，不报错） */
    @GetMapping("/sessions/{appointmentId}/record")
    public Result<ConsultationRecordResponseDTO> getSessionRecord(@PathVariable Long appointmentId) {
        return Result.ok(sessionService.getRecord(appointmentId));
    }

    /** 保存咨询记录（upsert，一单一记录，同事务） */
    @PutMapping("/sessions/{appointmentId}/record")
    public Result<Void> saveSessionRecord(@PathVariable Long appointmentId,
                                          @Valid @RequestBody ConsultationRecordSaveDTO dto) {
        sessionService.saveRecord(appointmentId, dto);
        return Result.ok();
    }
}
