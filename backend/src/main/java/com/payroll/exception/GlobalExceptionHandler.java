package com.payroll.exception;

import com.payroll.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Object>> handleDomainException(DomainException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(
                        UUID.randomUUID().toString(),
                        ex.getCode(),
                        ex.getMessage(),
                        ex.getDetails()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of("field", err.getField(), "issue", String.valueOf(err.getDefaultMessage())))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        UUID.randomUUID().toString(),
                        "ERR_VALIDATION_FAILED",
                        "Input tidak valid.",
                        details
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        UUID.randomUUID().toString(),
                        "ERR_INTERNAL_SERVER",
                        ex.getMessage() != null ? ex.getMessage() : "Terjadi kesalahan internal server.",
                        List.of()
                ));
    }
}
