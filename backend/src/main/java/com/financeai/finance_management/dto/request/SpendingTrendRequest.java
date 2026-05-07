package com.financeai.finance_management.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class SpendingTrendRequest {

    private List<Integer> expenses;
}