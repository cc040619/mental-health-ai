package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.AppointmentStatusDTO;
import com.cc.dto.request.RescheduleDTO;
import com.cc.dto.request.ReviewReplyDTO;
import com.cc.dto.response.AppointmentResponseDTO;
import com.cc.dto.response.ConsultationRecordResponseDTO;
import com.cc.dto.response.SlotResponseDTO;
import com.cc.service.AppointmentService;
import com.cc.service.ConsultantReviewService;
import com.cc.service.ConsultantSessionService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 预约后台管理控制器（管理员）：预约列表/详情、确认/取消(原因)/完成、
 * 改期（条件 UPDATE 抢时段）、空闲时段、评价查看/回复/删除。
 *
 * @author cc
 * @date 2026-09-14
 */
@RestController
@RequestMapping("/api/admin/appointments")
public class AdminAppointmentController {

    @Resource
    private AppointmentService appointmentService;
    @Resource
    private ConsultantReviewService consultantReviewService;
    @Resource
    private ConsultantSessionService consultantSessionService;

    /** 预约管理列表（状态/咨询师/来访者关键字/预约日期范围筛选 + 分页） */
    @GetMapping
    public Result<Map<String, Object>> page(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) Long consultantId,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate,
                                            @RequestParam(required = false) Long page,
                                            @RequestParam(required = false) Long size) {
        return Result.ok(appointmentService.adminPage(
                status, consultantId, keyword, startDate, endDate, page, size));
    }

    /** 预约详情：含问题描述、取消原因、评价与回复 */
    @GetMapping("/{id}")
    public Result<AppointmentResponseDTO> detail(@PathVariable Long id) {
        return Result.ok(appointmentService.adminDetail(id));
    }

    /** 咨询记录只读（4.8 联动展示，管理员只读，无记录返回 null 不报错） */
    @GetMapping("/{id}/record")
    public Result<ConsultationRecordResponseDTO> consultationRecord(@PathVariable Long id) {
        return Result.ok(consultantSessionService.getRecordForAdmin(id));
    }

    /** 状态变更：CONFIRMED/CANCELED/COMPLETED，状态机在 Service 层硬校验；CANCELED 可带取消原因 */
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id,
                                     @Valid @RequestBody AppointmentStatusDTO dto) {
        appointmentService.adminChangeStatus(id, dto.getStatus(), dto.getCancelReason());
        return Result.ok();
    }

    /** 改期：原子抢占新时段，仅待确认/已确认且未开始的预约可改 */
    @PutMapping("/{id}/reschedule")
    public Result<Void> reschedule(@PathVariable Long id,
                                   @Valid @RequestBody RescheduleDTO dto) {
        appointmentService.adminReschedule(id, dto.getSlotId());
        return Result.ok();
    }

    /** 查询咨询师未来空闲时段（改期选择用） */
    @GetMapping("/consultants/{consultantId}/free-slots")
    public Result<List<SlotResponseDTO>> freeSlots(@PathVariable Long consultantId) {
        return Result.ok(appointmentService.listFreeSlots(consultantId));
    }

    // ==================== 评价管理 ====================

    /** 评价分页：咨询师/评分/内容关键字筛选 */
    @GetMapping("/reviews")
    public Result<Map<String, Object>> reviewPage(@RequestParam(required = false) Long consultantId,
                                                  @RequestParam(required = false) Integer rating,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Long page,
                                                  @RequestParam(required = false) Long size) {
        return Result.ok(consultantReviewService.adminPage(
                consultantId, rating, keyword, page, size));
    }

    /** 回复评价（单级回复，重复回复覆盖） */
    @PutMapping("/reviews/{reviewId}/reply")
    public Result<Void> replyReview(@PathVariable Long reviewId,
                                    @Valid @RequestBody ReviewReplyDTO dto) {
        consultantReviewService.adminReply(reviewId, dto.getContent());
        return Result.ok();
    }

    /** 删除不当评价：预约评价标记复位，咨询师评分基于剩余评价全量重算 */
    @DeleteMapping("/reviews/{reviewId}")
    public Result<Void> deleteReview(@PathVariable Long reviewId) {
        consultantReviewService.adminDelete(reviewId);
        return Result.ok();
    }
}
