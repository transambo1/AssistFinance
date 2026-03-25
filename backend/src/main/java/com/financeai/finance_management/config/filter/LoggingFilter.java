package com.financeai.finance_management.config.filter;

import com.financeai.finance_management.config.context.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter {

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/swagger-ui")
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/swagger-resources")
        || path.startsWith("/webjars")
        || path.equals("/favicon.ico")
        || path.startsWith("/.well-known");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    var userContext = UserContextHolder.get();
    String userEmail =
        (userContext != null && userContext.getUserEmail() != null)
            ? userContext.getUserEmail()
            : "ANONYMOUS";

    String requestId = UUID.randomUUID().toString();
    MDC.put("requestId", requestId);
    MDC.put("userEmail", userEmail);
    MDC.put("path", request.getRequestURI());
    MDC.put("locale", request.getLocale().getLanguage());
    MDC.put("startTime", String.valueOf(System.currentTimeMillis()));

    long startTime = System.currentTimeMillis();

    ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 300);
    ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

    try {
      filterChain.doFilter(wrappedRequest, wrappedResponse);
    } finally {
      long duration = System.currentTimeMillis() - startTime;

      logRequest(wrappedRequest);
      logResponse(wrappedResponse, duration);

      wrappedResponse.copyBodyToResponse();

      MDC.clear();
    }
  }

  private void logRequest(ContentCachingRequestWrapper request) {
    String body = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);

    log.info(
        "Incoming Request: method={}, path={}, query={}, payload={}",
        request.getMethod(),
        request.getRequestURI(),
        request.getQueryString(),
        body);
  }

  private void logResponse(ContentCachingResponseWrapper response, long duration) {
    String contentType = response.getContentType();

    if (contentType != null
        && (contentType.contains("application/octet-stream")
            || contentType.contains("application/pdf")
            || contentType.contains("application/vnd")
            || contentType.startsWith("image/")
            || contentType.startsWith("video/"))) {
      log.info(
          "Outgoing Response: status={}, duration={}ms, payload=[skipped binary content]",
          response.getStatus(),
          duration);
      return;
    }

    String body = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);

    log.info(
        "Outgoing Response: status={}, duration={}ms, payload={}",
        response.getStatus(),
        duration,
        body);
  }
}
