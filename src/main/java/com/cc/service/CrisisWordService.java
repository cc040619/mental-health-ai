package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.entity.CrisisWord;
import com.cc.exception.BusinessException;
import com.cc.mapper.CrisisWordMapper;
import com.cc.util.SensitiveWordFilter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 危机词库业务层：增删改 + 启停用（护栏：启用词不足 5 个拒绝删/停）+ 恢复默认
 *
 * <p>安全底线：危机词表永不为空——删除/停用前校验剩余启用词数量，不足 5 个拒绝并
 * 提示用「恢复默认」重建；恢复默认从 classpath crisis-words.txt 全量重建。
 * 所有变更调用 {@link SensitiveWordFilter#reloadCrisis()} 不重启生效；
 * 危机词不纳入 AI 回复过滤配置（4.12 底线不变）。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Slf4j
@Service
public class CrisisWordService {

    /** 启用危机词最低数量护栏（防误操作清空安全词表） */
    private static final long MIN_ENABLED_WORDS = 5L;

    /** 词库分页上限 */
    private static final long MAX_PAGE_SIZE = 20L;

    @Resource
    private CrisisWordMapper crisisWordMapper;

    @Resource
    private SensitiveWordFilter sensitiveWordFilter;

    /**
     * 词库分页列表（keyword 模糊匹配）
     */
    public Map<String, Object> page(String keyword, Long page, Long size) {
        long p = page == null || page < 1 ? 1 : page;
        long s = size == null || size < 1 ? 10 : Math.min(size, MAX_PAGE_SIZE);
        LambdaQueryWrapper<CrisisWord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(CrisisWord::getWord, keyword.trim());
        }
        wrapper.orderByDesc(CrisisWord::getCreatedAt);
        Page<CrisisWord> result = crisisWordMapper.selectPage(new Page<>(p, s), wrapper);
        // entity 不直接出参：转换为 Map 列表
        List<Map<String, Object>> list = result.getRecords().stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", row.getId());
                    item.put("word", row.getWord());
                    item.put("status", row.getStatus());
                    item.put("createdAt", row.getCreatedAt());
                    return item;
                }).toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", result.getTotal());
        return data;
    }

    /**
     * 新增危机词（去重），立即生效（reload 重建词树）
     */
    public void add(String word) {
        String trimmed = word.trim();
        Long count = crisisWordMapper.selectCount(new LambdaQueryWrapper<CrisisWord>()
                .eq(CrisisWord::getWord, trimmed));
        if (count > 0) {
            throw new BusinessException("危机词已存在");
        }
        crisisWordMapper.insert(CrisisWord.builder().word(trimmed).status(1).build());
        sensitiveWordFilter.reloadCrisis();
        log.info("新增危机词: wordLen={}, operator=admin", trimmed.length());
    }

    /**
     * 启停用：停用受护栏保护（剩余启用词不足 5 拒绝），立即生效
     */
    public void updateStatus(Long id, Integer status) {
        CrisisWord target = crisisWordMapper.selectById(id);
        if (target == null) {
            throw new BusinessException("危机词不存在");
        }
        if (Integer.valueOf(0).equals(status) && Integer.valueOf(1).equals(target.getStatus())) {
            checkGuardOnDisable(id);
        }
        CrisisWord update = CrisisWord.builder().id(id).status(status).build();
        crisisWordMapper.updateById(update);
        sensitiveWordFilter.reloadCrisis();
        log.info("危机词启停用: id={}, status={}, operator=admin", id, status);
    }

    /**
     * 删除：启用词受护栏保护（剩余启用词不足 5 拒绝），立即生效
     */
    public void delete(Long id) {
        CrisisWord target = crisisWordMapper.selectById(id);
        if (target == null) {
            throw new BusinessException("危机词不存在");
        }
        if (Integer.valueOf(1).equals(target.getStatus())) {
            checkGuardOnDisable(id);
        }
        crisisWordMapper.deleteById(id);
        sensitiveWordFilter.reloadCrisis();
        log.info("删除危机词: id={}, operator=admin", id);
    }

    /**
     * 恢复默认：清空词表 + 从 classpath crisis-words.txt 全量重建（单事务），立即生效
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> restoreDefault() {
        crisisWordMapper.delete(null);
        List<String> words = sensitiveWordFilter.getDefaultCrisisWords().stream()
                .distinct()
                .toList();
        for (String word : words) {
            crisisWordMapper.insert(CrisisWord.builder().word(word).status(1).build());
        }
        sensitiveWordFilter.reloadCrisis();
        log.info("危机词恢复默认: 共 {} 词, operator=admin", words.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", words.size());
        result.put("message", "已从默认词库重建，共 " + words.size() + " 个危机词");
        return result;
    }

    /**
     * 护栏：删除/停用该启用词后剩余启用词 < 5 时拒绝
     */
    private void checkGuardOnDisable(Long excludeId) {
        Long enabled = crisisWordMapper.selectCount(new LambdaQueryWrapper<CrisisWord>()
                .eq(CrisisWord::getStatus, 1)
                .ne(CrisisWord::getId, excludeId));
        if (enabled < MIN_ENABLED_WORDS) {
            throw new BusinessException("启用危机词不足 " + MIN_ENABLED_WORDS
                    + " 个，不允许删除/停用；如需重建词库请使用「恢复默认」");
        }
    }
}
