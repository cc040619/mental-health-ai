package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.request.ImportWordsDTO;
import com.cc.dto.request.SensitiveWordDTO;
import com.cc.entity.SensitiveWord;
import com.cc.exception.BusinessException;
import com.cc.mapper.SensitiveWordMapper;
import com.cc.util.CurrentUserUtils;
import com.cc.util.SensitiveWordFilter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 敏感词管理业务层（仅违规词；危机词走 classpath 文件不纳入管理端，见 SensitiveWordFilter 决策注释）。
 * 所有写操作后调用 {@link SensitiveWordFilter#reload()} 重建词树，不重启生效。
 *
 * @author cc
 * @date 2026-09-15
 */
@Slf4j
@Service
public class SensitiveWordService {

    /** 词库分页上限 */
    private static final long MAX_PAGE_SIZE = 20L;

    /** 单次导入上限 */
    private static final int IMPORT_LIMIT = 1000;

    @Resource
    private SensitiveWordMapper sensitiveWordMapper;
    @Resource
    private SensitiveWordFilter sensitiveWordFilter;

    /**
     * 词库分页：词/分类筛选
     */
    public Map<String, Object> page(String keyword, String category, Long page, Long size) {
        ensureAdmin();
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : 10L;
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<SensitiveWord>()
                .like(keyword != null && !keyword.isBlank(), SensitiveWord::getWord, keyword.trim())
                .eq(category != null && !category.isBlank(), SensitiveWord::getCategory, category)
                .orderByDesc(SensitiveWord::getCreatedAt);
        IPage<SensitiveWord> result = sensitiveWordMapper.selectPage(new Page<>(cur, sz), wrapper);
        return Map.of("records", result.getRecords(), "total", result.getTotal());
    }

    /**
     * 新增敏感词（词唯一），写后重建词树
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(SensitiveWordDTO dto) {
        ensureAdmin();
        String word = dto.getWord().trim();
        checkDuplicate(word, null);
        sensitiveWordMapper.insert(SensitiveWord.builder()
                .word(word)
                .category(defaultCategory(dto.getCategory()))
                .status(dto.getStatus() == null ? 1 : dto.getStatus())
                .createdAt(LocalDateTime.now())
                .build());
        sensitiveWordFilter.reload();
        log.info("新增敏感词: id={}, operator=admin", sensitiveWordMapper.selectOne(
                new LambdaQueryWrapper<SensitiveWord>().eq(SensitiveWord::getWord, word)).getId());
    }

    /**
     * 编辑敏感词（词/分类/启停用），写后重建词树
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(SensitiveWordDTO dto) {
        ensureAdmin();
        if (dto.getId() == null) {
            throw new BusinessException("缺少敏感词ID");
        }
        SensitiveWord existing = mustExist(dto.getId());
        String word = dto.getWord().trim();
        checkDuplicate(word, dto.getId());
        existing.setWord(word);
        existing.setCategory(defaultCategory(dto.getCategory()));
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        sensitiveWordMapper.updateById(existing);
        sensitiveWordFilter.reload();
        log.info("编辑敏感词: id={}, operator=admin", dto.getId());
    }

    /**
     * 删除敏感词，写后重建词树
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ensureAdmin();
        mustExist(id);
        sensitiveWordMapper.deleteById(id);
        sensitiveWordFilter.reload();
        log.info("删除敏感词: id={}, operator=admin", id);
    }

    /**
     * 批量导入（≤1000/批，事务内逐条插入跳过重复），完成后统一重建一次词树。
     * ponytail: 逐条 insert 覆盖毕设量级；量级上万时升级为手写批量 INSERT VALUES
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> importWords(ImportWordsDTO dto) {
        ensureAdmin();
        List<String> rawWords = dto.getWords();
        if (rawWords.size() > IMPORT_LIMIT) {
            throw new BusinessException("单次导入不能超过" + IMPORT_LIMIT + "个词");
        }
        // 输入去重（保序），空行过滤
        Set<String> distinct = new LinkedHashSet<>();
        int invalid = 0;
        for (String raw : rawWords) {
            if (raw == null) {
                continue;
            }
            String word = raw.trim();
            if (word.isEmpty() || word.length() > 50) {
                invalid++;
            } else {
                distinct.add(word);
            }
        }
        // 库内已存在的词跳过
        Set<String> existing = distinct.isEmpty() ? Set.of()
                : sensitiveWordMapper.selectList(new LambdaQueryWrapper<SensitiveWord>()
                        .in(SensitiveWord::getWord, distinct))
                .stream().map(SensitiveWord::getWord).collect(java.util.stream.Collectors.toSet());

        String category = defaultCategory(dto.getCategory());
        int success = 0;
        for (String word : distinct) {
            if (existing.contains(word)) {
                continue;
            }
            sensitiveWordMapper.insert(SensitiveWord.builder()
                    .word(word)
                    .category(category)
                    .status(1)
                    .createdAt(LocalDateTime.now())
                    .build());
            success++;
        }
        // 全批只重建一次词树
        sensitiveWordFilter.reload();
        Map<String, Integer> result = new HashMap<>();
        result.put("success", success);
        result.put("duplicate", distinct.size() - success);
        result.put("invalid", invalid);
        log.info("批量导入敏感词: total={}, success={}, duplicate={}, invalid={}, operator=admin",
                rawWords.size(), success, distinct.size() - success, invalid);
        return result;
    }

    private SensitiveWord mustExist(Long id) {
        SensitiveWord word = sensitiveWordMapper.selectById(id);
        if (word == null) {
            throw new BusinessException("敏感词不存在");
        }
        return word;
    }

    /** 词唯一校验（编辑时排除自身） */
    private void checkDuplicate(String word, Long excludeId) {
        Long count = sensitiveWordMapper.selectCount(new LambdaQueryWrapper<SensitiveWord>()
                .eq(SensitiveWord::getWord, word)
                .ne(excludeId != null, SensitiveWord::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("该敏感词已存在");
        }
    }

    private String defaultCategory(String category) {
        return (category == null || category.isBlank()) ? "OTHER" : category;
    }

    private void ensureAdmin() {
        if (!CurrentUserUtils.isAdmin()) {
            throw new BusinessException("A0301", "无管理员权限", null);
        }
    }
}
