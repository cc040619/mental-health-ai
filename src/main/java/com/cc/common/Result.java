package com.cc.common;

import lombok.Data;

/**
 * 统一返回结果类，包装接口返回的 code、msg、data
 *
 * @param <T> 数据泛型
 * @author cc
 * @date 2026-09-01
 */
@Data
public class Result<T> {

    private String code;
    private String msg;
    private T data;

    public static <T> Result<T> ok() {
        Result<T> result = new Result<T>();
        result.code = ResultCode.SUCCESS.getCode();
        result.msg = ResultCode.SUCCESS.getMsg();
        return result;
    }

    //方法重载
    public static <T> Result<T> ok(T data) {
        Result<T> result = ok();
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error() {
        Result<T> result = new Result<T>();
        result.code = ResultCode.ERROR.getCode();
        result.msg = ResultCode.ERROR.getMsg();
        return result;
    }


    public static <T> Result<T> error(String code, String msg, T data) {
        Result<T> result = new Result<T>();
        result.code = code;
        result.msg = msg;
        result.setData(data);
        return result;
    }
}
