package com.financeai.finance_management.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AnomalyResponse {
  @JsonProperty("isAnomaly")
  private boolean anomaly;

  @JsonProperty("zScore")
  private double zScore;

  private double mean;
  private double std;

  private String anomalyMessage;
}
