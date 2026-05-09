package com.book.bookflow.exception;

import com.book.bookflow.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomerException.class)
    @ResponseBody
    public Result<Void> handleCustomerException(CustomerException exception) {
        LOGGER.error("Business error", exception);
        return Result.error(exception.getCode(), exception.getMsg());
    }

    @ExceptionHandler({
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    @ResponseBody
    public Result<Void> handleBadRequest(Exception exception) {
        LOGGER.warn("Bad request", exception);
        return Result.error("400", "\u8bf7\u6c42\u53c2\u6570\u9519\u8bef");
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result<Void> handleException(Exception exception) {
        LOGGER.error("System error", exception);
        return Result.error("500", "系统异常，请稍后重试");
    }
}
