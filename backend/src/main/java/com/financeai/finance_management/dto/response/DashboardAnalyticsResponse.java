package com.financeai.finance_management.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardAnalyticsResponse implements Serializable {
  //  private ForecastSection forecast;

  private CategorySection categoryDistribution;

  @Data
  @Builder
  public static class ForecastSection implements Serializable {
    private String percentageChange;
    private List<DashboardForecastResponse.MonthlyDataPoint> chartData;
    private String aiAnalysis;
  }

  @Data
  @Builder
  public static class CategorySection implements Serializable {
    private BigDecimal totalExpense; // 54.2M
    private List<CategoryDataPoint> details;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class CategoryDataPoint implements Serializable {
    private String categoryName;
    private BigDecimal amount;
    private Double percentage; // 45%
    private String color;

    public CategoryDataPoint(String categoryName, BigDecimal amount) {
      this.categoryName = categoryName;
      this.amount = amount;
    }
  }
}
