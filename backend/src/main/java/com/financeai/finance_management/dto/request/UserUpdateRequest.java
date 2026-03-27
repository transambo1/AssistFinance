package com.financeai.finance_management.dto.request;

import lombok.Data;

@Data
public class UserUpdateRequest {
    String fullName;
    String displayName;
    String email;
    String phone;
    String photoUrl;

}
