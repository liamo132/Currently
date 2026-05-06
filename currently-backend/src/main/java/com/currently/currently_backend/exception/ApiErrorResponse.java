package com.currently.currently_backend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    private final String code;
    private final String message;
    private final Map<String, String> details;
    private final Instant timestamp;
    private final String path;

    // API error DTO: standard response shape for Validation, Authentication, Security, and server errors.
    public ApiErrorResponse(String code, String message, Map<String, String> details, Instant timestamp, String path) {
        this.code = code;
        this.message = message;
        this.details = details;
        this.timestamp = timestamp;
        this.path = path;
    }

    // API error field: machine-readable code used by the frontend.
    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }
}
