package com.financeai.finance_management.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnomalyItemResponse {

  private String transactionId;

  private BigDecimal amount;

  private String note;

  private String categoryName;

  private String message;

  private Long transactionDate;
}
