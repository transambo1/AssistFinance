package com.financeai.finance_management.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AnomalyResponse {

  private boolean anomaly;

  @JsonProperty("z_score")
  private double zScore;

  private double mean;
  private double std;
  private String anomalyMessage;
}
