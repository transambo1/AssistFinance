package com.financeai.finance_management.dto.response;

import java.math.BigDecimal;

public interface CategorySumProjection {
  String getName();

  BigDecimal getAmount();

  String getColor();
}
