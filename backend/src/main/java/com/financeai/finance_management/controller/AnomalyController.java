package com.financeai.finance_management.controller;

import com.financeai.finance_management.dto.response.AnomalyItemResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.service.IAnomalyQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/anomalies")
public class AnomalyController {

    private final IAnomalyQueryService anomalyQueryService;

    @GetMapping("/today")
    public BaseResponse<List<AnomalyItemResponse>>
    getTodayAnomalies() {

        return BaseResponse.ok(
                anomalyQueryService.getTodayAnomalies()
        );
    }
}