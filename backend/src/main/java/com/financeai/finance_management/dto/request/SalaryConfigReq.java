package com.financeai.finance_management.dto.request;

import com.financeai.finance_management.enums.FrequencyType;
import com.financeai.finance_management.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryConfigReq {
    private String id;
    private String userId;
    private String categoryId;
    private BigDecimal amount;
    private TransactionType type;
    private FrequencyType frequency;
    private Integer payDay; // 1-31 cho MONTHLY, 1-7 cho WEEKLY
    private String description;
}
