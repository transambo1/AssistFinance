package com.financeai.finance_management.controller;

import com.financeai.finance_management.dto.request.UserUpdateRequest;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.entity.User;
import com.financeai.finance_management.service.IAuthenticationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User APIs", description = "Grouped User APIs") // Sidebar
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final IAuthenticationService authenticationService;

    @GetMapping("/my-info")
    public ResponseEntity<BaseResponse<User>> getMyInfo() {
        return ResponseEntity.ok(BaseResponse.ok(authenticationService.getMyInfo()));
    }
    @PutMapping("/my-info")
    public ResponseEntity<BaseResponse<User>> updateMyInfo(@RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(BaseResponse.ok(authenticationService.updateMyInfo(request)));
    }
}
