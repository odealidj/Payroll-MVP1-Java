package com.payroll.exception;

import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

public class DomainException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final List<Map<String, String>> details;

    public DomainException(HttpStatus status, String code, String message) {
        this(status, code, message, List.of());
    }

    public DomainException(HttpStatus status, String code, String message, List<Map<String, String>> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<Map<String, String>> getDetails() {
        return details;
    }
}
