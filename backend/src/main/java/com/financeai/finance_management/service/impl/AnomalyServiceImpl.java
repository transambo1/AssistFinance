package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.dto.request.AnomalyRequest;
import com.financeai.finance_management.dto.response.AnomalyResponse;
import com.financeai.finance_management.service.AnomalyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AnomalyServiceImpl implements AnomalyService {

  private final RestTemplate restTemplate;

  private static final String AI_URL = "http://localhost:8001/detect-anomaly";

  @Override
  public AnomalyResponse detect(AnomalyRequest request) {

    return restTemplate.postForObject(AI_URL, request, AnomalyResponse.class);
  }
}
