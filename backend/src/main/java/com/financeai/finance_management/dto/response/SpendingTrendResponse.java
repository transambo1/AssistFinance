package com.financeai.finance_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpendingTrendResponse {

  private Double prediction;

  private String trend;

  private String analysis;
}
