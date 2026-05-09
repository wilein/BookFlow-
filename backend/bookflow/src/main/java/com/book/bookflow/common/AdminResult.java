package com.book.bookflow.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminResult<T> {

    private Integer code;

    private String message;

    private T data;

    public static <T> AdminResult<T> success(T data) {
        return new AdminResult<>(0, "success", data);
    }

    public static <T> AdminResult<T> success(String message, T data) {
        return new AdminResult<>(0, message, data);
    }

    public static <T> AdminResult<T> error(Integer code, String message) {
        return new AdminResult<>(code, message, null);
    }
}
