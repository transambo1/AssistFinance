package com.financeai.finance_management.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AnomalyRequest {

    private List<Double> amounts;
    private Double newAmount;
}