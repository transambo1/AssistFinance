package com.financeai.finance_management.dto.request;

import com.financeai.finance_management.enums.BudgetType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BudgetUpdateRequest {
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BudgetType type;
    private String categoryId;
    private Integer month;
    private Integer year;
    private Boolean isActive;
}