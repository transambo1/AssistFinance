package com.financeai.finance_management.controller;

import com.financeai.finance_management.dto.response.AnomalyItemResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.service.IAnomalyQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/anomalies")
public class AnomalyController {

  private final IAnomalyQueryService anomalyQueryService;

  @GetMapping("/today")
  public BaseResponse<List<AnomalyItemResponse>> getTodayAnomalies() {

    return BaseResponse.ok(anomalyQueryService.getTodayAnomalies());
  }
}
