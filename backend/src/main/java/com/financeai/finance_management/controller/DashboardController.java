package com.financeai.finance_management.controller;

import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.DashboardForecastResponse;
import com.financeai.finance_management.service.IBudgetService;
import com.financeai.finance_management.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
  private final IDashboardService dashboardService;
  private final IBudgetService budgetService;

  @GetMapping("/chart/{monthsToLookBack}")
  public ResponseEntity<BaseResponse<DashboardForecastResponse>> getForecastDashboard(
      @PathVariable Integer monthsToLookBack) {
    String userId = budgetService.getCurrentUserId();
    return ResponseEntity.status(HttpStatus.OK)
        .body(this.dashboardService.getForecastDashboard(userId, monthsToLookBack));
  }
}
