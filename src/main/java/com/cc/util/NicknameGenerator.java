package com.cc.util;

import java.security.SecureRandom;

/**
 * 随机昵称生成工具：形容词 × 名词 + 2 位随机数（如"沐光风铃23"）。
 *
 * <p>无状态工具类，昵称不要求唯一（仅展示用）。</p>
 *
 * @author cc
 * @date 2026-09-16
 */
public class NicknameGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String[] ADJECTIVES = {
            "暖心", "向阳", "治愈", "沐光", "拾光", "逐风", "微澜", "晴朗"
    };

    private static final String[] NOUNS = {
            "小鹿", "云朵", "星星", "鲸鱼", "风铃", "向日葵", "布丁", "气球", "萤火虫", "小岛"
    };

    private NicknameGenerator() {
    }

    /**
     * 生成随机昵称：形容词 + 名词 + 10-99 的两位随机数。
     */
    public static String generate() {
        String adjective = ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[RANDOM.nextInt(NOUNS.length)];
        int suffix = 10 + RANDOM.nextInt(90); // 10..99
        return adjective + noun + suffix;
    }
}
