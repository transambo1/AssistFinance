package com.financeai.finance_management.dto.request;

import com.financeai.finance_management.enums.BudgetType;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BudgetCreationRequest {
  private String name;
  private BigDecimal targetAmount;
  private BigDecimal currentAmount;
  private BudgetType type;
  private String categoryId;
  private Long startDate;
  private Long endDate;
  private Integer durationMonths;
}
