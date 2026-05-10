package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.dto.response.*;
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
import org.springframework.web.client.HttpServerErrorException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {
  private final TransactionRepository transactionRepository;
  private final UserRepository userRepository;
  private final GeminiAiService geminiAiService;

  @Override
  @Transactional(readOnly = true)
  @Cacheable(value = "dashboardForecast", key = "'expense-' + #userId + '-' + #year")
  public BaseResponse<DashboardForecastResponse> getForecastDashboard(String userId, Integer year) {
    User user = userRepository.findById(userId).orElseThrow();
    Map<YearMonth, BigDecimal> history =
        getMonthlyDataByType(userId, year, TransactionType.EXPENSE);
    return BaseResponse.ok(buildForecastResponse(user, history, year, TransactionType.EXPENSE));
  }

  @Override
  @Transactional(readOnly = true)
  public BaseResponse<DashboardAnalyticsResponse> getDashboardAnalytics(
      String userId, Integer year, Integer month) {

    List<DashboardAnalyticsResponse.CategoryDataPoint> categoryData =
        getCategoryDistribution(userId, year, month);
    BigDecimal totalExpense =
        categoryData.stream()
            .map(DashboardAnalyticsResponse.CategoryDataPoint::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    DashboardAnalyticsResponse response =
        DashboardAnalyticsResponse.builder()
            //            .forecast(
            //                DashboardAnalyticsResponse.ForecastSection.builder()
            //                    .percentageChange(
            //                        calculateGrowthSingle(monthlyHistory,
            // aiResponse.getPrediction()))
            //                    .chartData(buildChartData(monthlyHistory, aiResponse, year))
            //                    .aiAnalysis(aiResponse.getAnalysis())
            //                    .build())
            .categoryDistribution(
                DashboardAnalyticsResponse.CategorySection.builder()
                    .totalExpense(totalExpense)
                    .details(categoryData)
                    .build())
            .build();

    return BaseResponse.ok(response);
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(value = "dashboardForecast", key = "'income-' + #userId + '-' + #year")
  public BaseResponse<DashboardForecastResponse> getIncomeForecastDashboard(
      String userId, Integer year) {
    User user = userRepository.findById(userId).orElseThrow();
    Map<YearMonth, BigDecimal> history = getMonthlyDataByType(userId, year, TransactionType.INCOME);
    return BaseResponse.ok(buildForecastResponse(user, history, year, TransactionType.INCOME));
  }

  private String calculateGrowthSingle(Map<YearMonth, BigDecimal> history, Double prediction) {
    if (history.isEmpty() || prediction == 0) return "0%";

    BigDecimal lastActual = new ArrayList<>(history.values()).get(history.size() - 1);
    if (lastActual.compareTo(BigDecimal.ZERO) == 0) return "0%";

    double diff = ((prediction - lastActual.doubleValue()) / lastActual.doubleValue()) * 100;
    return String.format("%s%.0f%% Dự kiến", diff > 0 ? "+" : "", diff);
  }

  private Map<YearMonth, BigDecimal> getMonthlyDataByType(
      String userId, int year, TransactionType type) {
    YearMonth now = YearMonth.now();
    int endMonth = (year < now.getYear()) ? 12 : (year == now.getYear() ? now.getMonthValue() : 0);

    if (endMonth == 0) return new TreeMap<>();

    long start =
        YearMonth.of(year, 1)
            .atDay(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
    long end =
        YearMonth.of(year, endMonth)
            .atEndOfMonth()
            .atTime(23, 59, 59)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();

    List<Transaction> transactions =
        transactionRepository.findByUserIdAndTypeAndTransactionDateBetween(
            userId, type, start, end);

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

  private DashboardForecastResponse buildForecastResponse(
      User user, Map<YearMonth, BigDecimal> history, Integer year, TransactionType type) {
    String typeVN = (type == TransactionType.EXPENSE) ? "chi tiêu" : "thu nhập";

    SpendingTrendResponse aiResponse = getAiPrediction(history, type);
    if (aiResponse.getPrediction() == 0 && aiResponse.getAnalysis().contains("Không có dữ liệu")) {
      aiResponse.setAnalysis("Chưa có dữ liệu " + typeVN + " trong năm này.");
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

    return DashboardForecastResponse.builder()
        .currency(user.getCurrency())
        .currentBalance(user.getCurrentBalance())
        .chartData(chartData)
        .aiAnalysis(aiResponse.getAnalysis())
        .percentageChange(calculateGrowthSingle(history, aiResponse.getPrediction()))
        .build();
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

  private SpendingTrendResponse getAiPrediction(
      Map<YearMonth, BigDecimal> history, TransactionType type) {
    boolean hasData =
        history.values().stream().anyMatch(amount -> amount.compareTo(BigDecimal.ZERO) > 0);

    if (!hasData) {
      return SpendingTrendResponse.builder().prediction(0.0).analysis("Không có dữ liệu.").build();
    }

    List<Integer> expenseValues =
        history.values().stream().map(BigDecimal::intValue).collect(Collectors.toList());

    try {
      if (type == TransactionType.EXPENSE) {
        return geminiAiService.predictTrend(expenseValues);
      } else {
        return geminiAiService.predictTrendIncome(expenseValues);
      }
    } catch (HttpServerErrorException e) {
      log.error("AI Server (Python) bị lỗi 500: {}", e);
      return SpendingTrendResponse.builder()
          .prediction(0.0)
          .analysis("Dịch vụ phân tích AI đang bảo trì.")
          .build();
    } catch (Exception e) {
      log.error("Lỗi kết nối AI: {}", e.getMessage());
      return SpendingTrendResponse.builder()
          .prediction(0.0)
          .analysis("Không thể kết nối AI.")
          .build();
    }
  }

  private List<DashboardAnalyticsResponse.CategoryDataPoint> getCategoryDistribution(
      String userId, Integer year, Integer month) {
    long start, end;

    if (month != null && month >= 1 && month <= 12) {
      YearMonth targetMonth = YearMonth.of(year, month);
      start = targetMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
      end =
          targetMonth
              .atEndOfMonth()
              .atTime(23, 59, 59)
              .atZone(ZoneId.systemDefault())
              .toInstant()
              .toEpochMilli();
    } else {
      YearMonth now = YearMonth.now();
      int endMonth = (year == now.getYear()) ? now.getMonthValue() : 12;
      start =
          YearMonth.of(year, 1)
              .atDay(1)
              .atStartOfDay(ZoneId.systemDefault())
              .toInstant()
              .toEpochMilli();
      end =
          YearMonth.of(year, endMonth)
              .atEndOfMonth()
              .atTime(23, 59, 59)
              .atZone(ZoneId.systemDefault())
              .toInstant()
              .toEpochMilli();
    }

    List<CategorySumProjection> rawData =
        transactionRepository.sumAmountByCategory(userId, start, end);

    BigDecimal totalYearlyExpense =
        rawData.stream()
            .map(CategorySumProjection::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    return rawData.stream()
        .map(
            dto -> {
              double percentage = 0;
              if (totalYearlyExpense.compareTo(BigDecimal.ZERO) > 0) {
                percentage =
                    (dto.getAmount().doubleValue() / totalYearlyExpense.doubleValue()) * 100;
              }

              return DashboardAnalyticsResponse.CategoryDataPoint.builder()
                  .categoryName(dto.getName())
                  .amount(dto.getAmount())
                  .percentage(Math.round(percentage * 10.0) / 10.0)
                  .color(dto.getColor())
                  .build();
            })
        .collect(Collectors.toList());
  }

  private List<DashboardForecastResponse.MonthlyDataPoint> buildChartData(
      Map<YearMonth, BigDecimal> history, SpendingTrendResponse aiResponse, Integer year) {

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

    return chartData;
  }
}
