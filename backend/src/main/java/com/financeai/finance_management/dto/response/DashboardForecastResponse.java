package com.financeai.finance_management.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardForecastResponse implements Serializable {
  private String currency;
  private BigDecimal currentBalance;
  private List<MonthlyDataPoint> chartData;
  private String aiAnalysis;
  private String percentageChange;

  @Data
  @Builder
  public static class MonthlyDataPoint implements Serializable {
    private String label;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    private boolean isForecast;
  }
}
