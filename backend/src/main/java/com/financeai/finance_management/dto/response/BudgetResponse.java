package com.financeai.finance_management.dto.response;

import com.financeai.finance_management.enums.BudgetType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class BudgetResponse {
    private String id;
    private String userId;
    private String categoryId;
    private String name;
    private BudgetType type;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private Integer month;
    private Integer year;
    private boolean isActive;
    private Long createdAt;
    private Long updatedAt;
    private Long deletedAt;
}