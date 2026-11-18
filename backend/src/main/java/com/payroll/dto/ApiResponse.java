package com.payroll.dto;

import java.util.List;
import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        String correlationId,
        T data,
        ErrorDetail error
) {
    public record ErrorDetail(String code, String message, List<Map<String, String>> details) {}

    public static <T> ApiResponse<T> ok(String correlationId, T data) {
        return new ApiResponse<>(true, correlationId, data, null);
    }

    public static <T> ApiResponse<T> error(String correlationId, String code, String message, List<Map<String, String>> details) {
        return new ApiResponse<>(false, correlationId, null, new ErrorDetail(code, message, details));
    }
}
