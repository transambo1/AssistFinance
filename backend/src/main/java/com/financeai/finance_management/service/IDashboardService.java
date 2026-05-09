package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.DashboardForecastResponse;

public interface IDashboardService {
  BaseResponse<DashboardForecastResponse> getForecastDashboard(
      String userId, Integer monthsToLookBack);
}
