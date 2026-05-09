package com.financeai.finance_management.dto.request;

import lombok.Data;

import java.util.Map;

@Data
public class SavingAdviceRequest {

    private Map<String, Double> categories;
}