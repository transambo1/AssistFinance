package com.financeai.finance_management.exception.exception;

import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MIN_VALUE = "min";

    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> maxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        return buildResponse(ErrorCode.MAX_SIZE_UPLOAD_FILE);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        String enumKey = e.getFieldError().getDefaultMessage();
        Map<String, Object> attributes = null;
        ErrorCode errorCode = ErrorCode.valueOf(enumKey);

        var constraintViolation = e.getBindingResult()
                .getAllErrors()
                .get(0)
                .unwrap(ConstraintViolation.class);

        attributes = constraintViolation.getConstraintDescriptor().getAttributes();
        log.info(attributes.toString());

        String finalMessage = Objects.nonNull(attributes)
                ? mapAttribute(errorCode.getMessage(), attributes)
                : errorCode.getMessage();

        return buildResponse(errorCode.getCode(), finalMessage);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> accessDeniedExceptionHandler(AccessDeniedException e) {
        return buildResponse(ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<Map<String, Object>> appExceptionHandler(AppException appException) {
        return buildResponse(appException.getErrorCode());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof org.hibernate.exception.ConstraintViolationException) {
            String message = cause.getMessage();
            if (message != null) {
                return buildResponse(ErrorCode.ERR_SYSTEM);
            }
        }
        return buildResponse(ErrorCode.UNCATEGORIED_EXCEPTION);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalException(IllegalArgumentException ex) {
        log.error("Illegal argument exception: ", ex);
        return buildResponse(ErrorCode.ERR_SYSTEM);
    }

    private String mapAttribute(String msg, Map<String, Object> attributes) {
        String minValue = attributes.get(MIN_VALUE).toString();
        return msg.replace("{" + MIN_VALUE + "}", minValue);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(ErrorCode errorCode) {
        return buildResponse(errorCode.getCode(), errorCode.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(String code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("status", "error"); // nếu bạn không cần 'status', có thể bỏ
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST); // hoặc HttpStatus.OK nếu bạn muốn 200
    }
}
