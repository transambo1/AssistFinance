package com.financeai.finance_management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeai.finance_management.exception.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {

        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;

        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("error", errorCode.getCode());
        errorBody.put("message", errorCode.getMessage());

        ResponseEntity<Map<String, String>> responseEntity = ResponseEntity
                .status(errorCode.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody);

        // Ghi ra response
        response.setStatus(responseEntity.getStatusCode().value());
        response.setContentType(Objects.requireNonNull(responseEntity.getHeaders().getContentType()).toString());
        response.getWriter().write(objectMapper.writeValueAsString(responseEntity.getBody()));
        response.flushBuffer();
    }
}
