package com.financeai.finance_management.dto.response;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiParseResponse {
  private BigDecimal amount;
  private String category;
  private String type;
  private String description;
  private Long transactionDate;
}
