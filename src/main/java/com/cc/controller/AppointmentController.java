package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.AppointmentCancelDTO;
import com.cc.dto.request.AppointmentCreateDTO;
import com.cc.dto.request.AppointmentMessageDTO;
import com.cc.dto.request.ConsultantReviewDTO;
import com.cc.dto.response.AppointmentMessageResponseDTO;
import com.cc.dto.response.AppointmentResponseDTO;
import com.cc.entity.Appointment;
import com.cc.service.AppointmentMessageService;
import com.cc.service.AppointmentService;
import com.cc.service.ConsultantReviewService;
import com.cc.util.CurrentUserUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 预约咨询用户端控制器：预约/我的预约/取消/留言/评价。
 * 全部接口需登录，归属校验在 Service 层统一完成。
 *
 * @author cc
 * @date 2026-09-14
 */
@RestController
@RequestMapping("/api/appointment")
public class AppointmentController {

    @Resource
    private AppointmentService appointmentService;
    @Resource
    private AppointmentMessageService appointmentMessageService;
    @Resource
    private ConsultantReviewService consultantReviewService;

    /** 创建预约（事务内原子抢时段） */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody AppointmentCreateDTO dto) {
        return Result.ok(appointmentService.create(dto));
    }

    /** 我的预约（状态筛选 + 分页） */
    @GetMapping("/mine")
    public Result<Map<String, Object>> mine(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) Long page,
                                            @RequestParam(required = false) Long size) {
        return Result.ok(appointmentService.mine(status, page, size));
    }

    /** 预约详情 */
    @GetMapping("/{id}")
    public Result<AppointmentResponseDTO> detail(@PathVariable Long id) {
        return Result.ok(appointmentService.detail(id));
    }

    /** 取消预约（PENDING 可取消，CONFIRMED 仅开始前可取消；取消原因必填） */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @Valid @RequestBody AppointmentCancelDTO dto) {
        appointmentService.cancelByUser(id, dto.getReason());
        return Result.ok();
    }

    /** 留言历史（分页倒序取最近） */
    @GetMapping("/{id}/messages")
    public Result<Map<String, Object>> messages(@PathVariable Long id,
                                                @RequestParam(required = false) Long page,
                                                @RequestParam(required = false) Long size) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        boolean isAdmin = CurrentUserUtils.isAdmin();
        appointmentService.getAppointmentForMember(id, userId, isAdmin);
        return Result.ok(appointmentMessageService.pageHistory(id, page, size));
    }

    /** 发送留言（HTTP 兜底，与 WebSocket 同一落库逻辑；管理员发送 senderType=2） */
    @PostMapping("/{id}/messages")
    public Result<AppointmentMessageResponseDTO> sendMessage(@PathVariable Long id,
                                                             @Valid @RequestBody AppointmentMessageDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        boolean isAdmin = CurrentUserUtils.isAdmin();
        Appointment appointment = appointmentService.getAppointmentForMember(id, userId, isAdmin);
        Integer senderType = isAdmin ? 2 : 1;
        return Result.ok(appointmentMessageService.save(appointment, userId, senderType, dto.getContent()));
    }

    /** 提交评价：仅 COMPLETED 且未评价，一单一评 */
    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id,
                               @Valid @RequestBody ConsultantReviewDTO dto) {
        consultantReviewService.submit(id, dto);
        return Result.ok();
    }
}
