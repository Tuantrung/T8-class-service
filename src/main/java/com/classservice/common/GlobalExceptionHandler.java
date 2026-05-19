package com.classservice.common;

import com.classservice.common.exception.BadCredentialsException;
import com.classservice.common.exception.DuplicateResourceException;
import com.classservice.common.exception.EntityNotFoundException;
import com.classservice.common.exception.ExcelParseException;
import com.classservice.common.exception.JwtExpiredException;
import com.classservice.common.exception.TenantMismatchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps application exceptions to HTTP status codes and a standard error body.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    record ApiError(String error, String message, Instant timestamp) {}

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message, Instant.now()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(EntityNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "ENTITY_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(TenantMismatchException.class)
    public ResponseEntity<ApiError> handleTenantMismatch(TenantMismatchException ex) {
        return error(HttpStatus.FORBIDDEN, "TENANT_MISMATCH", ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return error(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", ex.getMessage());
    }

    @ExceptionHandler(JwtExpiredException.class)
    public ResponseEntity<ApiError> handleJwtExpired(JwtExpiredException ex) {
        return error(HttpStatus.UNAUTHORIZED, "JWT_EXPIRED", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission to perform this action");
    }

    @ExceptionHandler(ExcelParseException.class)
    public ResponseEntity<ApiError> handleExcelParse(ExcelParseException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "EXCEL_PARSE_ERROR", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        record ValidationError(String error, String message, Map<String, String> fields, Instant timestamp) {}
        return ResponseEntity.badRequest()
            .body(new ValidationError("VALIDATION_ERROR", "Validation failed", fieldErrors, Instant.now()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntime(RuntimeException ex) {
        log.error("Unhandled exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
    }
}
