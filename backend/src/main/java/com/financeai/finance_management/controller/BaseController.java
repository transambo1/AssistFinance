package com.financeai.finance_management.controller;


import com.financeai.finance_management.common.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseController {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    protected Long extractUserIdFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        return jwtTokenUtil.extractUserId(header);
    }

    protected String extractUserRoleFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        return jwtTokenUtil.extractRole(header);
    }
}