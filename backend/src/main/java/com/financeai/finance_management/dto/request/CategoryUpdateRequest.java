package com.financeai.finance_management.dto.request;

import lombok.Data;

@Data
public class CategoryUpdateRequest {
    private String name;
    private String type;
    private String icon;
    private String color;
    private Boolean isArchived;
}