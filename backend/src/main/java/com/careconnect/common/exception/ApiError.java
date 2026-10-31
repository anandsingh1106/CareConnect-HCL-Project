package com.careconnect.common.exception;

import java.time.Instant;
import java.util.List;

/**
 * Error body returned by every failing API call, e.g.
 * {"timestamp":"...","status":400,"error":"Bad Request","code":"VALIDATION_FAILED",
 *  "message":"...","path":"/api/patients","fieldErrors":[{"field":"phone","message":"..."}]}
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldViolation> fieldErrors,
        Object details) {

    public record FieldViolation(String field, String message) {
    }
}
