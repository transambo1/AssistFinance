package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.dto.request.SavingAdviceRequest;
import com.financeai.finance_management.dto.request.SpendingTrendRequest;
import com.financeai.finance_management.dto.response.SavingAdviceResponse;
import com.financeai.finance_management.dto.response.SpendingTrendResponse;
import com.financeai.finance_management.service.GeminiAiService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class GeminiAiServiceImpl implements GeminiAiService {

  private final RestTemplate restTemplate;

  @Override
  public SpendingTrendResponse predictTrend(List<Integer> expenses) {

    String url = "http://127.0.0.1:8001/predict-spending";

    SpendingTrendRequest request = new SpendingTrendRequest();

    request.setExpenses(expenses);

    return restTemplate.postForObject(url, request, SpendingTrendResponse.class);
  }

  @Override
  public SavingAdviceResponse getSavingAdvice(Map<String, Double> categories) {

    String url = "http://localhost:8001/saving-advice";

    SavingAdviceRequest request = new SavingAdviceRequest();

    request.setCategories(categories);

    return restTemplate.postForObject(url, request, SavingAdviceResponse.class);
  }
}
