package com.example.chat_server.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Contract section 0.1: { "error": { "code", "message", "details" } }
    public record ErrorBody(ErrorDetail error) {
        public static ErrorBody of(String code, String message, Map<String, Object> details) {
            return new ErrorBody(new ErrorDetail(code, message, details));
        }
    }

    public record ErrorDetail(String code, String message,
                              @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, Object> details) {}

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorBody> handleApi(ApiException ex) {
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> messages = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> messages.putIfAbsent(e.getField(), e.getDefaultMessage()));

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("fields", List.copyOf(messages.keySet()));
        details.put("messages", messages);
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Request body is invalid", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorBody> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Request body is not readable JSON", null);
    }

    // Two concurrent requests passed the "already exists" check; the unique index caught it
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorBody> handleDuplicate(DuplicateKeyException ex) {
        return build(HttpStatus.CONFLICT, ErrorCode.EMAIL_TAKEN, "Email is already registered", null);
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<ErrorBody> handleMail(MailException ex) {
        log.error("Failed to send OTP email", ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.INTERNAL_ERROR, "Could not send the OTP email", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleOther(Exception ex) {
        // Spring's own MVC exceptions (404, 405, 415...) carry their status; keep it instead of turning them into 500
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            String code = status.value() == 404 ? ErrorCode.NOT_FOUND
                    : status.is4xxClientError() ? ErrorCode.VALIDATION_FAILED
                    : ErrorCode.INTERNAL_ERROR;
            return ResponseEntity.status(status).body(ErrorBody.of(code, ex.getMessage(), null));
        }
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "Internal server error", null);
    }

    private ResponseEntity<ErrorBody> build(HttpStatus status, String code, String message, Map<String, Object> details) {
        return ResponseEntity.status(status).body(ErrorBody.of(code, message, details));
    }
}
