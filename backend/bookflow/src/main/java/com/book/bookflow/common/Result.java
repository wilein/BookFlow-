package com.book.bookflow.common;

import cn.hutool.db.handler.StringHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> implements Serializable {
    private String code;
    private String message;
    private T data;
    /**
     * 成功的结果响应 不带参数
     * @return
     * @param <T>
     */
    public static <T> Result<T> success() {
        return new Result<>("200", "success", null);
    }
    public static <T> Result<T> success(String message,T data) {
        return new Result<>("200", message, data);
    }
    /**
     * 成功的结果响应
     * @param data 响应数据
     * @return
     * @param <T>
     */
    public static <T> Result<T> success(T data) {
        return new Result<>("200", "success", data);
    }
    public static <T> Result<T> error( String message) {
        return new Result<>("500", message, null);
    }
    /**
     * 错误的结果响应
     * @param code 状态码
     * @param message 错误消息
     * @return
     * @param <T>
     */
    public static <T> Result<T> error(String code, String message) {
        return new Result<>(code, message, null);
    }
    /**
     * 错误的结果响应
     * @param code 状态码
     * @param message 错误消息
     * @param data 错误数据
     * @return
     * @param <T>
     */
    public static <T> Result<T> error(String code, String message, T data) {
        return new Result<>(code, message, data);
    }
}