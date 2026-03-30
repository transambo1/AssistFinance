package com.financeai.finance_management.dto.response;

import com.financeai.finance_management.enums.BudgetStatus;
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
    private Long startDate;
    private Long endDate;
    private Boolean isActive;
    private BudgetStatus status;
    private Long createdAt;
    private Long updatedAt;
    private Long deletedAt;
}