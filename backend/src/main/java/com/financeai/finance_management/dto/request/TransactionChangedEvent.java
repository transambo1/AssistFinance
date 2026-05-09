package com.financeai.finance_management.dto.request;

import com.financeai.finance_management.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransactionChangedEvent {
  private final String userId;
  private final TransactionType type;
  private final Long transactionDate;
}
