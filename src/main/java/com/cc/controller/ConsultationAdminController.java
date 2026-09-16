package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.SessionMarkDTO;
import com.cc.dto.response.AdminSessionDetailResponseDTO;
import com.cc.service.ConsultationSessionService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 咨询记录后台管理控制器（管理员）
 *
 * <p>安全等级标注：会话标题/消息内容/评价反馈属心理健康敏感数据——
 * 接口仅管理员可访问（Controller 层 @PreAuthorize 双保险），日志不打印内容，导出文件名不含用户信息。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@RestController
@RequestMapping("/api/admin/consultations")
@PreAuthorize("hasRole('2')")
public class ConsultationAdminController {

    @Resource
    private ConsultationSessionService consultationSessionService;

    /** 会话列表：用户昵称/标题关键字、场景、情绪标签、评分范围、时间范围、危机会话筛选（单条聚合 SQL） */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String scene,
                                            @RequestParam(required = false) String emotionTag,
                                            @RequestParam(required = false) Integer ratingMin,
                                            @RequestParam(required = false) Integer ratingMax,
                                            @RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate,
                                            @RequestParam(required = false) Integer isCrisis,
                                            @RequestParam(required = false) Long page,
                                            @RequestParam(required = false) Long size) {
        return Result.ok(consultationSessionService.adminPage(keyword, scene, emotionTag,
                ratingMin, ratingMax, startDate, endDate, isCrisis, page, size));
    }

    /** 会话详情：完整信息 + 消息时间轴（含情绪值）+ 总结 + 评价 + 标记状态 */
    @GetMapping("/{id}")
    public Result<AdminSessionDetailResponseDTO> detail(@PathVariable Long id) {
        return Result.ok(consultationSessionService.adminDetail(id));
    }

    /** 标记会话：高质量/危机/备注（幂等，重复提交覆盖备注） */
    @PutMapping("/{id}/mark")
    public Result<Void> mark(@PathVariable Long id, @Valid @RequestBody SessionMarkDTO dto) {
        consultationSessionService.markSession(id, dto);
        return Result.ok();
    }

    /** 导出单会话 Excel：会话摘要 + 全部消息明细（文件名 session-{id}.xlsx，不含用户信息） */
    @GetMapping("/export/{id}")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        byte[] bytes = consultationSessionService.exportExcel(id);
        // 文件名仅含会话ID（数字），无用户信息、无注入面
        String filename = "session-" + id + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
