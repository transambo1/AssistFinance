package com.financeai.finance_management.dto.response;

import com.financeai.finance_management.enums.TransactionType;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyDetailResponse {
  private String transactionId;
  private BigDecimal amount;
  private String note;

  private String categoryId;
  private String categoryName;
  private String categoryColor;

  private TransactionType type;
  private String message;
  private Long transactionDate;

  private String imageUrl;
  private Boolean isAuto;

  private Long createdAt;
  private Long updatedAt;
}
