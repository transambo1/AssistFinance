package com.financeai.finance_management.dto.request;

import com.financeai.finance_management.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpsertTransactionRequest {
    private String userId; // cho job
    @NotBlank(message = "CATEGORY_ID_REQUIRED")
    private String categoryId;

    @NotNull(message = "AMOUNT_REQUIRED")
    @Positive(message = "AMOUNT_MUST_BE_POSITIVE")
    private BigDecimal amount;
    private TransactionType type;
    private String note;
    private String imageUrl;
    private Boolean isAuto;
    private Long transactionDate;
}
