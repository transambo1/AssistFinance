package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.response.AnomalyItemResponse;
import java.util.List;

public interface IAnomalyQueryService {

  List<AnomalyItemResponse> getTodayAnomalies();
}
