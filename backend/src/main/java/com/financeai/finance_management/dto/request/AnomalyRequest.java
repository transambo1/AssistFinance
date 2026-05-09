package com.financeai.finance_management.dto.request;

import java.util.List;
import lombok.Data;

@Data
public class AnomalyRequest {

  private List<Double> amounts;
  private Double newAmount;
}
