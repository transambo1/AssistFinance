package com.financeai.finance_management.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryResponse {
    private String id;
    private String userId;
    private String name;
    private String type;
    private String icon;
    private String color;
    private boolean isArchived;
    private Integer usageCount;
    private boolean isActive;
    private Long createdAt;
    private Long updatedAt;
    private Long deletedAt;
    private int version;
}