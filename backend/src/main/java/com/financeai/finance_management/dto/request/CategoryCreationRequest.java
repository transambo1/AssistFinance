package com.financeai.finance_management.dto.request;

import lombok.Data;

@Data
public class CategoryCreationRequest {
    private String userId;
    private String name;
    private String type;
    private String icon;
    private String color;
}