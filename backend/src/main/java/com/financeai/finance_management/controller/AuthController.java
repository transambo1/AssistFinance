package com.financeai.finance_management.controller;

import com.financeai.finance_management.model.User;
import com.financeai.finance_management.security.JwtUtils;
import com.financeai.finance_management.security.JwtResponse;
import com.financeai.finance_management.service.UserService; // Import thêm cái này
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService; // THÊM DÒNG NÀY ĐỂ GỌI XUỐNG DB

    // API Đăng ký (Đã sửa để lưu thật vào DB)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            // Lệnh này mới thực sự mã hóa mật khẩu và lưu xuống MySQL!
            userService.saveUser(user);
            return ResponseEntity.ok("Đăng ký thành công người dùng: " + user.getUsername());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi đăng ký: " + e.getMessage());
        }
    }

    // API Đăng nhập trả về JWT
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody User loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateToken(loginRequest.getUsername());

        return ResponseEntity.ok(new JwtResponse(jwt));
    }
}