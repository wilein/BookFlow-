package com.example.exception;
import com.example.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
/**
 * 全局异常捕获器
 */
@ControllerAdvice("com.example.controller")
public class GlobalExceptionHandler {
    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(Exception.class)
    @ResponseBody // 将result对象转换成 json的格式
    public Result error(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统异常");
    }
    @ExceptionHandler(CustomerException.class)
    @ResponseBody // 将result对象转换成 json的格式
    public Result customerError(CustomerException e) {
        log.error("自定义错误", e);
        return Result.error(e.getCode(), e.getMsg());
    }
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public Result handleAccessDenied(AccessDeniedException e) {
        log.error("权限不足", e);
        return Result.error("403", "权限不足");
    }
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    @ResponseBody
    public Result
    handleAuthenticationFailure(AuthenticationCredentialsNotFoundException e) {
        log.error("认证失败", e);
        return Result.error("401", "认证失败");
    }
}