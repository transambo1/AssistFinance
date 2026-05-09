package com.financeai.finance_management.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class SavingAdviceResponse {

    private String analysis;

    private List<String> tips;
}