package com.financeai.finance_management.controller;

import com.financeai.finance_management.dto.request.AuthenticationRequest;

import com.financeai.finance_management.dto.request.RegisterRequest;
import com.financeai.finance_management.dto.response.AuthenticationResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.service.IAuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth APIs", description = "Grouped Auth APIs")
@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/auths")
@RequiredArgsConstructor
public class AuthController {

    IAuthenticationService authenticationService;

    @Operation(summary = "Login")
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<AuthenticationResponse>> login(
            @RequestBody @Valid AuthenticationRequest request) {

        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @PostMapping("/register")
    public ResponseEntity<BaseResponse<AuthenticationResponse>> register(
            @RequestBody @Valid RegisterRequest request) {

        return ResponseEntity.ok(authenticationService.register(request));
    }

}