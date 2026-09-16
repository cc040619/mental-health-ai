package com.cc.util;

import cn.hutool.http.HtmlUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.entity.CrisisWord;
import com.cc.entity.SensitiveWord;
import com.cc.exception.BusinessException;
import com.cc.mapper.CrisisWordMapper;
import com.cc.mapper.SensitiveWordMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 敏感词 / 危机词过滤器（DFA 确定有限自动机算法）
 *
 * <p>词库来源：</p>
 * <ul>
 *   <li>违规敏感词：sensitive_word 表（管理端可增删改/导入，{@link #reload()} 重建词树不重启生效）；
 *       表加载失败时回退 classpath sensitive-words.txt 兜底（如迁移未执行），保证校验不失效</li>
 *   <li>危机词：crisis_word 表（管理端可增删改，{@link #reloadCrisis()} 重建不重启生效）；
 *       首次启动空表自动从 classpath crisis-words.txt 播种，表加载失败回退文件兜底。
 *       安全底线：管理端删除/停用受"启用词不足 5 个拒绝"护栏保护（见 CrisisWordService），
 *       且危机词<b>不纳入 AI 回复过滤配置</b>（4.12 底线），始终全量参与 3.9 危机检测</li>
 * </ul>
 *
 * <p>并发：两组词树均用 volatile 引用，reload 时整体替换引用，重建期间读旧树，无锁无撕裂。</p>
 *
 * @author cc
 * @date 2026-09-10
 */
@Slf4j
@Component
public class SensitiveWordFilter {

    /** 敏感词词库文件（表加载失败的兜底来源） */
    private static final String SENSITIVE_FILE = "sensitive-words.txt";

    /** 危机词默认词库文件（首次播种 + 恢复默认 + 表加载失败兜底来源） */
    private static final String CRISIS_FILE = "crisis-words.txt";

    /** DFA 词树结束标记 key */
    private static final String END_FLAG = "isEnd";

    /** 敏感词词树（volatile：reload 整体替换引用，重建期间读旧树） */
    private volatile Map<Object, Object> sensitiveWordMap = new HashMap<>();

    /** 危机词词树（volatile：reloadCrisis 整体替换引用，重建期间读旧树） */
    private volatile Map<Object, Object> crisisWordMap = new HashMap<>();

    @Resource
    private SensitiveWordMapper sensitiveWordMapper;

    @Resource
    private CrisisWordMapper crisisWordMapper;

    @PostConstruct
    public void init() {
        initCrisisWords();
        reload();
        log.info("敏感词过滤器加载完成: 敏感词树节点数={}, 危机词树节点数={}", sensitiveWordMap.size(), crisisWordMap.size());
    }

    /**
     * 危机词初始化：空表自动从 classpath 文件播种（迁移后首次启动），
     * 初始化/加载失败回退文件词树，保证危机词树永不为空（安全底线）
     */
    private void initCrisisWords() {
        try {
            if (crisisWordMapper.selectCount(null) == 0) {
                List<String> words = readWordsFromFile(CRISIS_FILE);
                for (String word : words) {
                    crisisWordMapper.insert(CrisisWord.builder().word(word).status(1).build());
                }
                log.info("危机词表为空，已从classpath播种 {} 个默认危机词", words.size());
            }
            reloadCrisis();
        } catch (Exception e) {
            log.error("危机词表初始化失败，回退classpath文件词树: {}", e.getMessage());
            crisisWordMap = buildWordMapFromFile(CRISIS_FILE);
        }
    }

    /**
     * 重建敏感词词树：查 sensitive_word 表启用词建树后整体替换引用。
     * 管理端增删改/导入后调用，不重启生效；查库失败回退词库文件（迁移未执行兜底）。
     */
    public void reload() {
        Map<Object, Object> newTree;
        try {
            List<SensitiveWord> words = sensitiveWordMapper.selectList(null);
            newTree = buildWordMap(words.stream()
                    .filter(w -> w.getStatus() != null && w.getStatus() == 1)
                    .map(SensitiveWord::getWord)
                    .toList());
        } catch (Exception e) {
            log.error("敏感词表加载失败，回退classpath词库文件: {}", e.getMessage());
            newTree = buildWordMapFromFile(SENSITIVE_FILE);
        }
        sensitiveWordMap = newTree;
    }

    /**
     * 重建危机词词树：查 crisis_word 表启用词建树后整体替换引用（volatile 无锁）。
     * 管理端增删改/恢复默认后调用，不重启生效；查库失败回退词库文件兜底。
     */
    public void reloadCrisis() {
        Map<Object, Object> newTree;
        try {
            List<CrisisWord> rows = crisisWordMapper.selectList(
                    new LambdaQueryWrapper<CrisisWord>().eq(CrisisWord::getStatus, 1));
            newTree = buildWordMap(rows.stream().map(CrisisWord::getWord).toList());
        } catch (Exception e) {
            log.error("危机词表加载失败，回退classpath词库文件: {}", e.getMessage());
            newTree = buildWordMapFromFile(CRISIS_FILE);
        }
        crisisWordMap = newTree;
    }

    /**
     * 默认危机词（classpath crisis-words.txt 内容），管理端「恢复默认」重建词库用
     */
    public List<String> getDefaultCrisisWords() {
        return readWordsFromFile(CRISIS_FILE);
    }

    /**
     * 校验文本是否包含敏感词，命中则抛业务异常拦截
     * <p>帖子正文为富文本 HTML，先去标签再匹配，避免标签字符干扰
     *
     * @param text 待校验文本（可为 HTML）
     */
    public void checkSensitive(String text) {
        String hit = findSensitiveWord(text);
        if (hit != null) {
            log.warn("内容命中敏感词，已拦截发布");
            throw new BusinessException("内容包含违规词汇，请修改后再发布");
        }
    }

    /**
     * 查找命中的敏感词，未命中返回 null
     */
    public String findSensitiveWord(String text) {
        return findWord(HtmlUtil.cleanHtmlTag(text == null ? "" : text), sensitiveWordMap);
    }

    /**
     * 查找命中的危机词（自残/自杀类），未命中返回 null
     */
    public String findCrisisWord(String text) {
        return findWord(HtmlUtil.cleanHtmlTag(text == null ? "" : text), crisisWordMap);
    }

    /**
     * DFA 匹配：以每个字符为起点沿词树下行，到达结束标记即命中
     */
    private String findWord(String text, Map<Object, Object> wordMap) {
        if (text == null || text.isBlank() || wordMap.isEmpty()) {
            return null;
        }
        for (int i = 0; i < text.length(); i++) {
            Map<Object, Object> node = wordMap;
            for (int j = i; j < text.length(); j++) {
                Object next = node.get(text.charAt(j));
                if (!(next instanceof Map)) {
                    break;
                }
                @SuppressWarnings("unchecked")
                Map<Object, Object> child = (Map<Object, Object>) next;
                if (Boolean.TRUE.equals(child.get(END_FLAG))) {
                    return text.substring(i, j + 1);
                }
                node = child;
            }
        }
        return null;
    }

    /**
     * 从词集合构建 DFA 词树（表驱动来源）
     */
    private Map<Object, Object> buildWordMap(List<String> words) {
        Map<Object, Object> wordMap = new HashMap<>();
        for (String word : words) {
            if (word == null || word.isBlank()) {
                continue;
            }
            insertWord(wordMap, word.trim());
        }
        return wordMap;
    }

    /**
     * 从 classpath 加载词库并构建 DFA 词树（危机词兜底 + 敏感词兜底）
     */
    private Map<Object, Object> buildWordMapFromFile(String fileName) {
        return buildWordMap(readWordsFromFile(fileName));
    }

    /**
     * 从 classpath 词库文件读取词列表（跳过空行）
     */
    private List<String> readWordsFromFile(String fileName) {
        List<String> words = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(fileName).getInputStream(), StandardCharsets.UTF_8))) {
            String word;
            while ((word = reader.readLine()) != null) {
                word = word.trim();
                if (word.isEmpty()) {
                    continue;
                }
                words.add(word);
            }
        } catch (Exception e) {
            // 词库加载失败不阻断启动，但所有文本将放行：启动日志必须可见
            log.error("敏感词词库加载失败，文件={}，文本校验将失效: {}", fileName, e.getMessage());
        }
        return words;
    }

    /** 单词插入 DFA 词树 */
    private void insertWord(Map<Object, Object> wordMap, String word) {
        Map<Object, Object> node = wordMap;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            @SuppressWarnings("unchecked")
            Map<Object, Object> child = (Map<Object, Object>) node.computeIfAbsent(c, k -> new HashMap<>());
            if (i == word.length() - 1) {
                child.put(END_FLAG, Boolean.TRUE);
            }
            node = child;
        }
    }
}
