package com.cc.common;

import com.cc.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器，统一捕获并处理 Controller 层抛出的异常
 *
 * @author cc
 * @date 2026-09-01
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 处理参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handlerException(MethodArgumentNotValidException e) {
        // 处理异常数据的处理
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.error(ResultCode.PARAM_ERROR.getCode(), ResultCode.PARAM_ERROR.getMsg(), message);
    }

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        // 如果异常携带额外的数据
        if (e.getData() != null) {
            return Result.error(e.getCode(), e.getMessage(), e.getData());
        }
        return Result.error(e.getCode(), e.getMessage(), null);
    }

    /**
     * 处理数据库完整性约束冲突（唯一键重复、外键冲突等）。
     * 仅返回友好提示，不向前端暴露原始 SQL 或堆栈。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<?> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        // ponytail: 仅提取 cause message 用于区分列名，不向外暴露完整堆栈
        String raw = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : "";
        log.warn("数据库完整性约束冲突: {}", raw);
        String msg = (raw != null && raw.contains("Duplicate entry"))
                ? resolveDuplicateMessage(raw)
                : "数据校验失败，请检查输入";
        return Result.error(ResultCode.DATA_DUPLICATE.getCode(), msg, null);
    }

    // 根据约束冲突信息中的 key 名映射为中文提示，未知 key 回退通用提示
    private String resolveDuplicateMessage(String raw) {
        String lower = raw.toLowerCase();
        if (lower.contains("user.phone")) {
            return "手机号已存在";
        }
        if (lower.contains("user.email")) {
            return "邮箱已存在";
        }
        if (lower.contains("user.username")) {
            return "用户名已存在";
        }
        return ResultCode.DATA_DUPLICATE.getMsg();
    }

}
