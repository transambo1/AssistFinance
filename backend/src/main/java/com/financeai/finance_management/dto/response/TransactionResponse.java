package com.financeai.finance_management.dto.response;

import com.financeai.finance_management.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private String id;
    private String userId;

    private String categoryId;
    private String categoryName;
    private String note;
    private String transactionDate;
    private TransactionType type;
    private BigDecimal amount;
}
