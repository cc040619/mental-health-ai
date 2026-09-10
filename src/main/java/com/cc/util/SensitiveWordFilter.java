package com.cc.util;

import cn.hutool.http.HtmlUtil;
import com.cc.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 敏感词 / 危机词过滤器（DFA 确定有限自动机算法）
 *
 * <p>词库从 classpath 加载，一行一词：</p>
 * <ul>
 *   <li>sensitive-words.txt：违规敏感词，命中抛 {@link BusinessException} 拦截发布</li>
 *   <li>crisis-words.txt：危机词（自残/自杀类），命中不拦截，由业务层打 is_crisis 标记并返回干预提示</li>
 * </ul>
 *
 * @author cc
 * @date 2026-09-10
 */
@Slf4j
@Component
public class SensitiveWordFilter {

    /** 敏感词词库文件 */
    private static final String SENSITIVE_FILE = "sensitive-words.txt";

    /** 危机词词库文件 */
    private static final String CRISIS_FILE = "crisis-words.txt";

    /** DFA 词树结束标记 key */
    private static final String END_FLAG = "isEnd";

    /** 敏感词词树 */
    private Map<Object, Object> sensitiveWordMap = new HashMap<>();

    /** 危机词词树 */
    private Map<Object, Object> crisisWordMap = new HashMap<>();

    @PostConstruct
    public void init() {
        sensitiveWordMap = buildWordMap(SENSITIVE_FILE);
        crisisWordMap = buildWordMap(CRISIS_FILE);
        log.info("敏感词过滤器加载完成: 敏感词树节点数={}, 危机词树节点数={}", sensitiveWordMap.size(), crisisWordMap.size());
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
     * 从 classpath 加载词库并构建 DFA 词树
     */
    private Map<Object, Object> buildWordMap(String fileName) {
        Map<Object, Object> wordMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(fileName).getInputStream(), StandardCharsets.UTF_8))) {
            String word;
            while ((word = reader.readLine()) != null) {
                word = word.trim();
                if (word.isEmpty()) {
                    continue;
                }
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
        } catch (Exception e) {
            // 词库加载失败不阻断启动，但所有文本将放行：启动日志必须可见
            log.error("敏感词词库加载失败，文件={}，文本校验将失效: {}", fileName, e.getMessage());
        }
        return wordMap;
    }
}
