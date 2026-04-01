package com.financeai.finance_management.controller;

import com.financeai.finance_management.dto.request.AiParseRequest;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.TransactionResponse;
import com.financeai.finance_management.service.IAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final IAiService aiService;

    @PostMapping("/parse-and-save")
    public BaseResponse<List<TransactionResponse>> parseAndSaveTransaction(@RequestBody AiParseRequest request) {
        return BaseResponse.ok(aiService.parseAndSaveTransaction(request));
    }
}