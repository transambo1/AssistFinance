package com.financeai.finance_management.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

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