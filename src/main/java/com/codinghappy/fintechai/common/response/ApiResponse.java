package com.codinghappy.fintechai.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {

    private boolean success;

    private String code;

    private String message;

    private T data;

    private Long timestamp;

    private String requestId;

    // 成功响应
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("0000")
                .message("成功")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("0000")
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // 失败响应
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // 带请求ID的响应
    public static <T> ApiResponse<T> success(T data, String message, String requestId) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("0000")
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .requestId(requestId)
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message, String requestId) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .requestId(requestId)
                .build();
    }

    // 便捷方法
    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    // 快速创建常用错误响应
    public static <T> ApiResponse<T> badRequest(String message) {
        return error("400", message);
    }

    public static <T> ApiResponse<T> unauthorized(String message) {
        return error("401", message);
    }

    public static <T> ApiResponse<T> forbidden(String message) {
        return error("403", message);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return error("404", message);
    }

    public static <T> ApiResponse<T> tooManyRequests(String message) {
        return error("429", message);
    }

    public static <T> ApiResponse<T> internalError(String message) {
        return error("500", message);
    }

    // 业务错误
    public static <T> ApiResponse<T> businessError(String message) {
        return error("1000", message);
    }

    public static <T> ApiResponse<T> validationError(String message) {
        return error("1001", message);
    }

    public static <T> ApiResponse<T> rateLimitError(String message) {
        return error("1002", message);
    }

    public static <T> ApiResponse<T> apiError(String message) {
        return error("1003", message);
    }

    // 数据操作错误
    public static <T> ApiResponse<T> dataNotFound(String message) {
        return error("2001", message);
    }

    public static <T> ApiResponse<T> dataExists(String message) {
        return error("2002", message);
    }

    public static <T> ApiResponse<T> dataError(String message) {
        return error("2003", message);
    }
}