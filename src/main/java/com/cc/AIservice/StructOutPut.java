package com.cc.AIservice;

/**
 *`record` 是 Java 的特殊类，目的：消除 POJO 数据类的样板代码。
 * 传统写一个数据类，需要手写：
 * - 私有成员变量
 * - 全参构造器
 * - getter 方法
 * - `equals()`
 * - `hashCode()`
 * - `toString()`
 * 使用 `record`，只需要在括号里列出字段列表，编译器**自动帮你生成上面全部代码**，不需要手写。
 * AI流式会话输出对象，非常适合
 */
public class StructOutPut {
    public record StreamChatSession(
            String sessionId,
            Long userHash,
            String initialMessage,
            Long startTime,
            Long expiryTime,
            Integer messageCount,
            String status
    ) {}
}
