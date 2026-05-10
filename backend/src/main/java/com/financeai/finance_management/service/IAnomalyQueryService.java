package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.response.AnomalyCountResponse;
import com.financeai.finance_management.dto.response.AnomalyDetailResponse;
import com.financeai.finance_management.dto.response.AnomalyItemResponse;
import java.util.List;

public interface IAnomalyQueryService {

  AnomalyCountResponse countTodayAnomalies();

  List<AnomalyItemResponse> getTodayAnomalies();

  AnomalyDetailResponse getAnomalyDetail(String transactionId);
}
