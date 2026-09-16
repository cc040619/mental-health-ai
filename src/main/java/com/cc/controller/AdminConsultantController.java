package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.ConsultantAuditDTO;
import com.cc.dto.request.ConsultantSaveDTO;
import com.cc.dto.request.ConsultantStatusDTO;
import com.cc.dto.request.SlotBatchCreateDTO;
import com.cc.dto.request.SlotStatusDTO;
import com.cc.dto.response.ConsultantAdminDetailResponseDTO;
import com.cc.dto.response.ConsultantIncomeResponseDTO;
import com.cc.dto.response.ConsultantSaveResultDTO;
import com.cc.dto.response.SlotResponseDTO;
import com.cc.service.ConsultantService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
 * 咨询师后台管理控制器（管理员）：列表/审核/CRUD/禁用恢复/排班周视图/咨询记录/评价/收入统计。
 * 管理员权限在 Service 层 ensureAdmin 统一校验。
 *
 * <p>ponytail: 当前由管理员账号模拟咨询师完成排班；真实咨询师端上线后复用同一组排班接口。</p>
 *
 * @author cc
 * @date 2026-09-14
 */
@RestController
@RequestMapping("/api/admin/consultants")
public class AdminConsultantController {

    @Resource
    private ConsultantService consultantService;

    /** 咨询师列表：keyword 匹配姓名/擅长领域，status 按 0待审核/1正常/2禁用 筛选 */
    @GetMapping
    public Result<Map<String, Object>> page(@RequestParam(required = false) Long page,
                                            @RequestParam(required = false) Long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(consultantService.adminPage(page, size, keyword, status));
    }

    /** 列表状态 Tab 计数 */
    @GetMapping("/status-counts")
    public Result<Map<String, Object>> statusCounts() {
        return Result.ok(consultantService.statusCounts());
    }

    /** 咨询师详情（任意状态，含资质图片/审核信息/排序/价格） */
    @GetMapping("/{id}")
    public Result<ConsultantAdminDetailResponseDTO> detail(@PathVariable Long id) {
        return Result.ok(consultantService.adminDetail(id));
    }

    /** 资质审核：通过/驳回（驳回必填原因，仅待审核可审） */
    @PutMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id,
                               @Valid @RequestBody ConsultantAuditDTO dto) {
        consultantService.adminReview(id, dto);
        return Result.ok();
    }

    /** 咨询记录 Tab（分页，懒加载） */
    @GetMapping("/{id}/appointments")
    public Result<Map<String, Object>> appointments(@PathVariable Long id,
                                                     @RequestParam(required = false) Long page,
                                                     @RequestParam(required = false) Long size) {
        return Result.ok(consultantService.adminAppointments(id, page, size));
    }

    /** 评价列表 Tab（分页，懒加载） */
    @GetMapping("/{id}/reviews")
    public Result<Map<String, Object>> reviews(@PathVariable Long id,
                                               @RequestParam(required = false) Long page,
                                               @RequestParam(required = false) Long size) {
        return Result.ok(consultantService.adminReviews(id, page, size));
    }

    /** 收入统计（付费预留：已完成数 × price，无价格返回 null） */
    @GetMapping("/{id}/income")
    public Result<ConsultantIncomeResponseDTO> income(@PathVariable Long id) {
        return Result.ok(consultantService.adminIncome(id));
    }

    /**
     * 新增咨询师（默认待审核）：同步创建 user_type=3 登录账号，
     * 返回值中的 username/initialPassword 仅此次返回一次，请提示管理员转交本人
     */
    @PostMapping
    public Result<ConsultantSaveResultDTO> create(@Valid @RequestBody ConsultantSaveDTO dto) {
        return Result.ok(consultantService.save(dto));
    }

    /** 编辑咨询师（待审核状态编辑=重新提交，清空驳回原因；排序权重并入此接口） */
    @PutMapping
    public Result<ConsultantSaveResultDTO> update(@Valid @RequestBody ConsultantSaveDTO dto) {
        return Result.ok(consultantService.save(dto));
    }

    /** 禁用/恢复（1正常 2禁用；待审核记录拒绝） */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody ConsultantStatusDTO dto) {
        consultantService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }

    /**
     * 排班批量生成：按日（date）或周模板（weekStart，传周内任意一天，归一到周一）。
     * 周模式跳过已存在时段，返回 {created, skipped}。
     */
    @PostMapping("/{id}/slots")
    public Result<Map<String, Integer>> createSlots(@PathVariable Long id,
                                                    @Valid @RequestBody SlotBatchCreateDTO dto) {
        return Result.ok(consultantService.batchCreateSlots(id, dto));
    }

    /**
     * 排班查询：传 weekStart 返回整周 7 天时段（周视图，一次查询）；
     * 否则按 date 返回单日（date 空=今天）。
     */
    @GetMapping("/{id}/slots")
    public Result<List<SlotResponseDTO>> slots(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        if (weekStart != null) {
            return Result.ok(consultantService.weekSlots(id, weekStart));
        }
        return Result.ok(consultantService.adminSlots(id, date));
    }

    /** 单时段停用/恢复（0可约 2停用；已约时段拒绝） */
    @PutMapping("/slots/{slotId}/status")
    public Result<Void> updateSlotStatus(@PathVariable Long slotId,
                                         @Valid @RequestBody SlotStatusDTO dto) {
        consultantService.updateSlotStatus(slotId, dto.getStatus());
        return Result.ok();
    }
}
