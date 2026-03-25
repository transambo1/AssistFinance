package com.financeai.finance_management.controller;

import com.financeai.finance_management.service.IAuthenticationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User APIs", description = "Grouped User APIs") // Sidebar
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final IAuthenticationService authenticationService;

}
