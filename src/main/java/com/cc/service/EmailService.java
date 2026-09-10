package com.cc.service;

import cn.hutool.core.util.RandomUtil;
import com.cc.common.RedisConstants;
import com.cc.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 邮件服务，负责发送 QQ 邮箱登录验证码
 *
 * <p>核心机制：</p>
 * <ul>
 *   <li>发送验证码时，先生成 6 位数字验证码写入 Redis（TTL 5 分钟）</li>
 *   <li>同时写入冷却 key（TTL 60 秒），防止 60 秒内重复发送</li>
 *   <li>登录时从 Redis 取出验证码与请求中的 code 比对</li>
 *   <li>验证成功后立即删除验证码，防止重复使用</li>
 * </ul>
 *
 * <p>异步发送：实际邮件投递走 {@code ioExecutor} 线程池异步执行，
 * 控制器可立即返回响应；发送失败时仅记日志并在 Redis 清理验证码与冷却 key，
 * 用户可立即重试。</p>
 *
 * @author cc
 * @date 2026-09-07
 */
@Slf4j
@Service
public class EmailService {

    @Resource
    private JavaMailSender mailSender;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 自注入代理对象，避免 {@code sendLoginCode} 直接调用本类的 {@code sendEmail}
     * 触发 Spring AOP 自调用失效问题（自调用不走代理，{@code @Async} 不会生效）。
     * 使用 {@code @Lazy} 避免循环依赖。
     */
    @Resource
    @Lazy
    private EmailService self;

    /** 邮件发件人地址，由 spring.mail.username 自动注入 */
    @Value("${spring.mail.username:}")
    private String fromEmail;

    /** 邮件发件人昵称 */
    private static final String FROM_NICKNAME = "心理健康助手";

    /** 邮件主题 */
    private static final String SUBJECT = "【心理健康助手】登录验证码";

    /** 验证码长度 */
    private static final int CODE_LENGTH = 6;

    /**
     * 发送邮箱登录验证码
     *
     * @param email 目标邮箱地址
     */
    public void sendLoginCode(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("邮箱不能为空");
        }

        // 冷却校验，防止 60 秒内重复发送
        String cooldownKey = RedisConstants.buildEmailLoginCooldownKey(email);
        Boolean absent = stringRedisTemplate.opsForValue().setIfAbsent(
                cooldownKey, "1", RedisConstants.EMAIL_CODE_COOLDOWN_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(absent)) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }

        // 生成 6 位数字验证码
        String code = RandomUtil.randomNumbers(CODE_LENGTH);

        // 写入 Redis，TTL 5 分钟
        String codeKey = RedisConstants.buildEmailLoginCodeKey(email);
        stringRedisTemplate.opsForValue().set(
                codeKey, code, RedisConstants.EMAIL_CODE_TTL_MINUTES, TimeUnit.MINUTES);

        // 异步发送邮件（通过 self 代理调用，确保 @Async 生效，避免自调用穿透）
        self.sendEmail(email, code);
    }

    /**
     * 校验邮箱登录验证码
     *
     * @param email 邮箱地址
     * @param code  用户输入的验证码
     * @return true=验证通过
     */
    public boolean verifyLoginCode(String email, String code) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(code)) {
            return false;
        }
        String codeKey = RedisConstants.buildEmailLoginCodeKey(email);
        String storedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!storedCode.equals(code)) {
            throw new BusinessException("验证码错误");
        }
        // 验证成功后立即删除验证码，防止重复使用
        stringRedisTemplate.delete(codeKey);
        // 清理冷却 key
        stringRedisTemplate.delete(RedisConstants.buildEmailLoginCooldownKey(email));
        return true;
    }

    /**
     * 异步发送邮件，使用 ioExecutor 线程池执行
     *
     * <p>由于在 {@link #sendLoginCode(String)} 内部通过 {@code self} 代理调用，
     * Spring AOP 会拦截本方法并将任务投递到 {@code ioExecutor} 线程池，
     * 调用方（{@code sendLoginCode}）会立即返回，不阻塞 HTTP 响应。</p>
     *
     * <p>失败处理：发送失败时不向调用方抛出异常（异步任务无法回传异常给调用方），
     * 仅清理 Redis 中的验证码与冷却 key 允许用户立即重试，并记录错误日志。
     * 用户若长时间未收到邮件，可在 60 秒冷却后重新发起请求。</p>
     *
     * @param toEmail 收件人邮箱
     * @param code    6 位验证码
     */
    @Async("ioExecutor")
    public void sendEmail(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, FROM_NICKNAME);
            helper.setTo(toEmail);
            helper.setSubject(SUBJECT);
            String content = buildEmailContent(code);
            helper.setText(content, true);
            mailSender.send(message);
            log.info("邮箱登录验证码发送成功, to={}, code={}", toEmail, code);
        } catch (MessagingException e) {
            // 异步发送失败：清理 Redis 中的验证码与冷却 key，允许用户立即重试
            stringRedisTemplate.delete(RedisConstants.buildEmailLoginCodeKey(toEmail));
            stringRedisTemplate.delete(RedisConstants.buildEmailLoginCooldownKey(toEmail));
            log.error("邮箱登录验证码发送失败, to={}", toEmail, e);
        } catch (Exception e) {
            stringRedisTemplate.delete(RedisConstants.buildEmailLoginCodeKey(toEmail));
            stringRedisTemplate.delete(RedisConstants.buildEmailLoginCooldownKey(toEmail));
            log.error("邮箱登录验证码发送异常, to={}", toEmail, e);
        }
    }

    /**
     * 构建邮件正文（HTML 格式，与登录页手账治愈风格保持一致）
     */
    private String buildEmailContent(String code) {
        return "<div style=\"font-family:'Microsoft YaHei','Segoe UI',sans-serif;color:#5C4A3A;background-color:#FDF6E3;padding:32px;border-radius:16px;max-width:480px;margin:0 auto;\">"
                + "<h2 style=\"color:#D67860;font-size:22px;margin:0 0 16px 0;\">📖 心理健康助手 · 登录验证码</h2>"
                + "<p style=\"font-size:14px;color:#8B7E6E;margin:0 0 16px 0;line-height:1.7;\">您正在进行邮箱登录操作，验证码为：</p>"
                + "<div style=\"text-align:center;margin:20px 0;\">"
                + "<span style=\"display:inline-block;font-size:32px;font-weight:bold;letter-spacing:8px;color:#E8927C;background-color:#FFFEF9;padding:12px 24px;border-radius:12px;border:1.5px dashed #C4B5A4;\">"
                + code
                + "</span>"
                + "</div>"
                + "<p style=\"font-size:14px;color:#8B7E6E;margin:8px 0 0 0;line-height:1.7;\">验证码 5 分钟内有效，请勿向他人泄露。如非本人操作，请忽略本邮件。</p>"
                + "<hr style=\"border:none;border-top:1px dashed #C4B5A4;margin:24px 0;\" />"
                + "<p style=\"font-size:12px;color:#C4B5A4;margin:0;\">—— 心理健康助手团队 🧡</p>"
                + "</div>";
    }
}
