package com.cc.util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 富文本 HTML 白名单清洗器（XSS 防护）
 *
 * <p>策略：移除 script/style/iframe 等危险块 → 剔除所有 on* 事件属性 →
 * 仅保留白名单标签与安全协议（http/https/相对路径）的 src/href，
 * 非白名单标签去掉标签但保留内部文字。</p>
 *
 * <p>ponytail: 正则白名单清洗覆盖富文本存储 XSS 的主要攻击面；
 * 若未来需要处理不可信大规模富文本（如允许外部 HTML 粘贴），升级为 jsoup 白名单方案。</p>
 *
 * @author cc
 * @date 2026-09-10
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    /** 允许保留的标签白名单 */
    private static final Set<String> ALLOWED_TAGS = Set.of(
            "p", "br", "b", "strong", "i", "em", "u", "s", "h1", "h2", "h3", "h4", "h5", "h6",
            "ul", "ol", "li", "blockquote", "hr", "img", "a", "span", "div"
    );

    /** 危险块标签（连标签带内容整体移除） */
    private static final Pattern DANGEROUS_BLOCK = Pattern.compile(
            "<(script|style|iframe|object|embed)[^>]*>[\\s\\S]*?</\\1>", Pattern.CASE_INSENSITIVE);

    /** 危险自闭合/残留标签 */
    private static final Pattern DANGEROUS_TAG = Pattern.compile(
            "</?(script|style|iframe|object|embed)[^>]*>", Pattern.CASE_INSENSITIVE);

    /** 所有 HTML 标签 */
    private static final Pattern TAG_PATTERN = Pattern.compile(
            "<(/?)([a-zA-Z][a-zA-Z0-9]*)\\b([^>]*)>");

    /** on* 事件属性（onerror/onclick 等） */
    private static final Pattern ON_ATTR = Pattern.compile(
            "\\s+on[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", Pattern.CASE_INSENSITIVE);

    /** src/href 属性提取 */
    private static final Pattern URL_ATTR = Pattern.compile(
            "\\s+(src|href)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", Pattern.CASE_INSENSITIVE);

    /**
     * 清洗富文本 HTML，返回安全 HTML
     */
    public static String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        // 1. 移除危险块与危险标签
        String safe = DANGEROUS_BLOCK.matcher(html).replaceAll("");
        safe = DANGEROUS_TAG.matcher(safe).replaceAll("");

        // 2. 逐标签白名单过滤
        Matcher matcher = TAG_PATTERN.matcher(safe);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String closing = matcher.group(1);
            String tag = matcher.group(2).toLowerCase(Locale.ROOT);
            String attrs = matcher.group(3);
            String replacement;
            if (!ALLOWED_TAGS.contains(tag)) {
                // 非白名单标签：去掉标签，保留内部文字
                replacement = "";
            } else if ("/".equals(closing)) {
                replacement = "</" + tag + ">";
            } else if ("br".equals(tag) || "hr".equals(tag)) {
                replacement = "<" + tag + " />";
            } else {
                replacement = "<" + tag + cleanAttributes(attrs) + ">";
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 清洗标签属性：剔除事件属性，仅保留安全协议的 src/href
     * <p>style/class 等展示属性保留（wangeditor 排版依赖），事件属性与 javascript: 协议已移除
     */
    private static String cleanAttributes(String attrs) {
        String cleaned = ON_ATTR.matcher(attrs).replaceAll("");
        StringBuilder kept = new StringBuilder();
        Matcher urlMatcher = URL_ATTR.matcher(cleaned);
        while (urlMatcher.find()) {
            String name = urlMatcher.group(1).toLowerCase(Locale.ROOT);
            String value = urlMatcher.group(2) != null ? urlMatcher.group(2)
                    : urlMatcher.group(3) != null ? urlMatcher.group(3) : urlMatcher.group(4);
            if (isSafeUrl(value)) {
                kept.append(' ').append(name).append("=\"").append(value.trim()).append('"');
            }
        }
        return kept.toString();
    }

    /**
     * 仅允许 http/https/相对路径/mailto，拦截 javascript:、data: 等危险协议
     */
    private static boolean isSafeUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String u = url.trim().toLowerCase(Locale.ROOT);
        return u.startsWith("http://") || u.startsWith("https://")
                || u.startsWith("/") || u.startsWith("mailto:");
    }
}
