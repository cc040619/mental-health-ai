package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.AiConfigSaveDTO;
import com.cc.dto.request.AiPromptSaveDTO;
import com.cc.dto.request.AiPromptTestDTO;
import com.cc.dto.request.AiSampleAnnotateDTO;
import com.cc.dto.request.AiSampleGenerateDTO;
import com.cc.dto.request.AiTestDTO;
import com.cc.service.AiCallLogService;
import com.cc.service.AiConfigService;
import com.cc.service.AiPromptService;
import com.cc.service.AiQualityService;
import com.cc.service.AiReplyFilterService;
import com.cc.util.CurrentUserUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 模型配置管理端（提示词版本管理 / 模型参数 / 实时测试 / 过滤记录 / 质量抽检 / 调用统计）
 *
 * @author cc
 * @date 2026-09-15
 */
@RestController
@RequestMapping("/api/admin/ai")
@PreAuthorize("hasRole('2')")
public class AiAdminController {

    @Resource
    private AiConfigService aiConfigService;
    @Resource
    private AiCallLogService aiCallLogService;
    @Resource
    private AiPromptService aiPromptService;
    @Resource
    private AiQualityService aiQualityService;
    @Resource
    private AiReplyFilterService aiReplyFilterService;

    // ==================== 模型参数 ====================

    /** 配置列表（模型/温度/最大token/top_p/单价/过滤开关与动作） */
    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        Map<String, Object> data = new HashMap<>();
        data.put("items", aiConfigService.getConfigMap());
        return Result.ok(data);
    }

    /** 保存配置（后端数值范围硬校验，写后删缓存下一条对话即生效） */
    @PutMapping("/config")
    public Result<Void> saveConfig(@Valid @RequestBody AiConfigSaveDTO dto) {
        aiConfigService.saveConfigs(dto.getItems());
        return Result.ok(null);
    }

    /** 实时测试：当前配置无记忆单轮调用（限频 10s/管理员，真实消耗 token） */
    @PostMapping("/test")
    public Result<Map<String, Object>> test(@Valid @RequestBody AiTestDTO dto) {
        aiConfigService.checkTestLimit(CurrentUserUtils.getCurrentUserId());
        return Result.ok(aiConfigService.test(dto.getMessage()));
    }

    // ==================== 提示词管理 ====================

    /** 按 key 分组列表：生效版本号/摘要/版本数 */
    @GetMapping("/prompts")
    public Result<Object> listPrompts() {
        return Result.ok(aiPromptService.listGrouped());
    }

    /** 某 key 全部版本（内容为摘要，全文走预览） */
    @GetMapping("/prompts/{key}/versions")
    public Result<Object> listVersions(@PathVariable("key") String key) {
        return Result.ok(aiPromptService.versions(key));
    }

    /** 预览某版本全文 */
    @GetMapping("/prompts/{key}")
    public Result<Object> previewPrompt(@PathVariable("key") String key,
                                        @RequestParam("version") Integer version) {
        return Result.ok(aiPromptService.preview(key, version));
    }

    /** 保存新版本（一律草稿不生效，需激活——两步防误操作） */
    @PostMapping("/prompts")
    public Result<Object> savePrompt(@Valid @RequestBody AiPromptSaveDTO dto) {
        return Result.ok(aiPromptService.saveDraft(dto));
    }

    /** 激活版本（返回影响说明，前端二次确认） */
    @PutMapping("/prompts/{id}/activate")
    public Result<Object> activatePrompt(@PathVariable("id") Long id) {
        return Result.ok(aiPromptService.activate(id));
    }

    /** 恢复默认：以静态常量生成新草稿（不自动激活） */
    @PostMapping("/prompts/{key}/restore")
    public Result<Object> restorePrompt(@PathVariable("key") String key) {
        return Result.ok(aiPromptService.restoreDefault(key));
    }

    /** 提示词试跑（限频 10s/管理员，真实消耗 token） */
    @PostMapping("/prompts/test")
    public Result<Map<String, Object>> testPrompt(@Valid @RequestBody AiPromptTestDTO dto) {
        aiConfigService.checkTestLimit(CurrentUserUtils.getCurrentUserId());
        return Result.ok(aiPromptService.testRun(dto.getContent(), dto.getMessage()));
    }

    // ==================== 敏感词过滤记录 ====================

    /** 拦截记录列表（时间/触发词/动作/摘要，不存全文） */
    @GetMapping("/blocks")
    public Result<Map<String, Object>> listBlocks(@RequestParam(value = "page", required = false) Long page,
                                                  @RequestParam(value = "size", required = false) Long size) {
        return Result.ok(aiReplyFilterService.pageBlocks(page, size));
    }

    // ==================== 质量抽检 ====================

    /** 发起抽检：随机抽 N 个未抽过的会话 */
    @PostMapping("/samples/generate")
    public Result<Map<String, Object>> generateSamples(@Valid @RequestBody AiSampleGenerateDTO dto) {
        int generated = aiQualityService.generate(dto.getCount());
        return Result.ok(Map.of("generated", generated));
    }

    /** 抽检列表 */
    @GetMapping("/samples")
    public Result<Map<String, Object>> listSamples(@RequestParam(value = "status", required = false) Integer status,
                                                   @RequestParam(value = "page", required = false) Long page,
                                                   @RequestParam(value = "size", required = false) Long size) {
        return Result.ok(aiQualityService.page(status, page, size));
    }

    /** 抽检详情（复用咨询记录详情数据 + 标注信息） */
    @GetMapping("/samples/{id}")
    public Result<Map<String, Object>> sampleDetail(@PathVariable("id") Long id) {
        return Result.ok(aiQualityService.detail(id));
    }

    /** 标注：状态/评分/备注 */
    @PutMapping("/samples/{id}")
    public Result<Void> annotateSample(@PathVariable("id") Long id,
                                       @Valid @RequestBody AiSampleAnnotateDTO dto) {
        aiQualityService.annotate(id, dto);
        return Result.ok(null);
    }

    // ==================== 调用统计 ====================

    /** 统计：汇总 + 按日趋势 + 用途分布（range=7D/30D） */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestParam(value = "range", required = false) String range) {
        return Result.ok(aiCallLogService.stats(AiCallLogService.normalizeRange(range)));
    }
}
