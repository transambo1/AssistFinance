package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.DashboardForecastResponse;
import com.financeai.finance_management.dto.response.SpendingTrendResponse;
import com.financeai.finance_management.entity.Transaction;
import com.financeai.finance_management.entity.User;
import com.financeai.finance_management.enums.TransactionType;
import com.financeai.finance_management.repository.TransactionRepository;
import com.financeai.finance_management.repository.UserRepository;
import com.financeai.finance_management.service.GeminiAiService;
import com.financeai.finance_management.service.IDashboardService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {
  private final TransactionRepository transactionRepository;
  private final UserRepository userRepository;
  private final GeminiAiService geminiAiService;

  @Override
  @Transactional(readOnly = true)
  @Cacheable(
      value = "dashboardForecast",
      key = "#userId + '-' + #year",
      unless =
          "#result.data.chartData.isEmpty() || #result.data.chartData.get(#result.data.chartData.size()-1).amount.doubleValue() == 0")
  public BaseResponse<DashboardForecastResponse> getForecastDashboard(String userId, Integer year) {
    User user = userRepository.findById(userId).orElseThrow();
    Map<YearMonth, BigDecimal> history = getMonthlyExpensesByYear(userId, year);

    boolean hasData =
        history.values().stream().anyMatch(amount -> amount.compareTo(BigDecimal.ZERO) > 0);

    SpendingTrendResponse aiResponse;

    if (!hasData) {
      aiResponse =
          SpendingTrendResponse.builder()
              .prediction(0.0)
              .trend("stable")
              .analysis("Chưa có dữ liệu chi tiêu trong năm này để thực hiện phân tích.")
              .build();
    } else {
      List<Integer> expenseValues =
          history.values().stream().map(BigDecimal::intValue).collect(Collectors.toList());
      try {
        aiResponse = geminiAiService.predictTrend(expenseValues);
      } catch (Exception e) {
        log.error("AI Service Error: {}", e);
        aiResponse =
            SpendingTrendResponse.builder()
                .prediction(0.0)
                .trend("unknown")
                .analysis("Tạm thời không có phân tích từ AI.")
                .build();
      }
    }

    List<DashboardForecastResponse.MonthlyDataPoint> chartData = new ArrayList<>();
    history.forEach(
        (month, amount) ->
            chartData.add(
                DashboardForecastResponse.MonthlyDataPoint.builder()
                    .label("TH" + month.getMonthValue())
                    .amount(amount)
                    .isForecast(false)
                    .build()));

    if (year >= YearMonth.now().getYear()) {
      YearMonth nextMonth =
          history.keySet().stream().max(YearMonth::compareTo).orElse(YearMonth.now()).plusMonths(1);

      chartData.add(
          DashboardForecastResponse.MonthlyDataPoint.builder()
              .label("TH" + nextMonth.getMonthValue())
              .amount(BigDecimal.valueOf(aiResponse.getPrediction()))
              .isForecast(true)
              .build());
    }

    DashboardForecastResponse response =
        DashboardForecastResponse.builder()
            .currency(user.getCurrency())
            .currentBalance(user.getCurrentBalance())
            .chartData(chartData)
            .aiAnalysis(aiResponse.getAnalysis())
            .percentageChange(calculateGrowthSingle(history, aiResponse.getPrediction()))
            .build();

    return BaseResponse.ok(response);
  }

  private String calculateGrowthSingle(Map<YearMonth, BigDecimal> history, Double prediction) {
    if (history.isEmpty() || prediction == 0) return "0%";

    BigDecimal lastActual = new ArrayList<>(history.values()).get(history.size() - 1);
    if (lastActual.compareTo(BigDecimal.ZERO) == 0) return "0%";

    double diff = ((prediction - lastActual.doubleValue()) / lastActual.doubleValue()) * 100;
    return String.format("%s%.0f%% Dự kiến", diff > 0 ? "+" : "", diff);
  }

  private Map<YearMonth, BigDecimal> getMonthlyExpensesByYear(String userId, int year) {
    YearMonth now = YearMonth.now();
    int endMonth;

    if (year < now.getYear()) {
      endMonth = 12;
    } else if (year == now.getYear()) {
      endMonth = now.getMonthValue();
    } else {
      return new TreeMap<>();
    }

    long startTimestamp =
        YearMonth.of(year, 1)
            .atDay(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
    long endTimestamp =
        YearMonth.of(year, endMonth)
            .atEndOfMonth()
            .atTime(23, 59, 59)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();

    List<Transaction> transactions =
        transactionRepository.findByUserIdAndTypeAndTransactionDateBetween(
            userId, TransactionType.EXPENSE, startTimestamp, endTimestamp);

    Map<YearMonth, BigDecimal> groupedData =
        transactions.stream()
            .collect(
                Collectors.groupingBy(
                    t ->
                        YearMonth.from(
                            Instant.ofEpochMilli(t.getTransactionDate())
                                .atZone(ZoneId.systemDefault())),
                    TreeMap::new,
                    Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

    Map<YearMonth, BigDecimal> fullYearData = new TreeMap<>();
    for (int i = 1; i <= endMonth; i++) {
      YearMonth ym = YearMonth.of(year, i);
      fullYearData.put(ym, groupedData.getOrDefault(ym, BigDecimal.ZERO));
    }
    return fullYearData;
  }
}
