package com.financeai.finance_management.dto.response;

import lombok.Data;

@Data
public class SpendingTrendResponse {

    private Double prediction;

    private String trend;

    private String analysis;
}