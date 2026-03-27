package com.financeai.finance_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String username;
    private String fullName;
    private String displayName;
    private String phone;
    private String email;
    private String photoUrl;
    private BigDecimal currentBalance;
    private Long createdAt;
    private BigDecimal salaryAmount;
    private Integer payday;
}
