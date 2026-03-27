package com.financeai.finance_management.dto.request;

import com.financeai.finance_management.enums.CategoryType;
import lombok.Data;

@Data
public class CategoryCreationRequest {
    private String userId;
    private String name;
    private CategoryType type;
    private String icon;
    private String color;
}