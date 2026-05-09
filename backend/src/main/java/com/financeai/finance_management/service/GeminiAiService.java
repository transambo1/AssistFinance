package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.response.SavingAdviceResponse;
import com.financeai.finance_management.dto.response.SpendingTrendResponse;
import java.util.List;
import java.util.Map;

public interface GeminiAiService {

  SpendingTrendResponse predictTrend(List<Integer> expenses);

  SavingAdviceResponse getSavingAdvice(Map<String, Double> categories);
}
