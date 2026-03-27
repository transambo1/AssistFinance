package com.financeai.finance_management.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {
  private boolean success;
  private T data;

  // error section (only for success = false)
  private HttpStatus status;
  private String exception;
  private String message;
  private String stackTrace;
  private List<FieldError> fieldErrors;

  // metadata
  private Instant timestamp;
  private String requestId;
  private String path;
  private Long durationMs;
  private String locale;

  public record FieldError(String field, String message) {}

  public static <T> BaseResponse<T> ok(T data) {
    long durationMs =
        System.currentTimeMillis()
            - (MDC.get("startTime") != null ? Long.parseLong(MDC.get("startTime")) : 0);
    return BaseResponse.<T>builder()
        .success(true)
        .data(data)
        .timestamp(Instant.now())
        .requestId(MDC.get("requestId"))
        .locale(MDC.get("locale"))
        .durationMs(durationMs)
        .path(MDC.get("path"))
        .build();
  }

  public static <T> BaseResponse<T> error(
      HttpStatus status,
      String exception,
      String message,
      String stackTrace,
      List<FieldError> fieldErrors) {
    String requestId = MDC.get("requestId");
    long durationMs =
        System.currentTimeMillis()
            - (MDC.get("startTime") != null ? Long.parseLong(MDC.get("startTime")) : 0);

    return BaseResponse.<T>builder()
        .success(false)
        .fieldErrors(fieldErrors)
        .timestamp(Instant.now())
        .status(status)
        .exception(exception)
        .message(message)
        .stackTrace(stackTrace)
        .requestId(requestId)
        .locale(MDC.get("locale"))
        .durationMs(durationMs)
        .path(MDC.get("path"))
        .build();
  }
}
