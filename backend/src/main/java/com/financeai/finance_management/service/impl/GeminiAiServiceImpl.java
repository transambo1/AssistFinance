package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.dto.request.SpendingTrendRequest;
import com.financeai.finance_management.dto.response.SpendingTrendResponse;
import com.financeai.finance_management.service.GeminiAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GeminiAiServiceImpl implements GeminiAiService {

    private final RestTemplate restTemplate;

    @Override
    public SpendingTrendResponse predictTrend(List<Integer> expenses) {

        String url = "http://127.0.0.1:8001/predict-spending";

        SpendingTrendRequest request =
                new SpendingTrendRequest();

        request.setExpenses(expenses);

        return restTemplate.postForObject(
                url,
                request,
                SpendingTrendResponse.class
        );
    }
}