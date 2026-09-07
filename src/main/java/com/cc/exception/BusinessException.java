package com.cc.exception;

import lombok.Getter;

/**
 * 业务异常类，用于在业务逻辑中抛出可识别的错误
 *
 * @author cc
 * @date 2026-09-01
 */
@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final String message;
    private final Object data;

    public BusinessException(String message) {
        super(message);
        this.code = "BUSINESS_ERROR";
        this.message = message;
        this.data = null;
    }

    /**
     * 携带自定义错误码与额外数据的业务异常
     *
     * @param code    错误码（如 ResultCode 中的文件类错误码）
     * @param message 错误信息
     * @param data    额外数据
     */
    public BusinessException(String code, String message, Object data) {
        super(message);
        this.code = code;
        this.message = message;
        this.data = data;
    }

}