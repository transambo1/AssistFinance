package com.financeai.finance_management.controller;

import com.financeai.finance_management.dto.request.AiChatRequest;
import com.financeai.finance_management.dto.request.AiParseRequest;
import com.financeai.finance_management.dto.request.AiQueryRequest;
import com.financeai.finance_management.dto.response.AiChatResponse;
import com.financeai.finance_management.dto.response.AiQueryResponse;
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
    public BaseResponse<List<TransactionResponse>> parseAndSave(@RequestBody AiParseRequest request) {
        return aiService.parseAndSaveTransaction(request);
    }

    @PostMapping("/query")
    public BaseResponse<AiQueryResponse> query(@RequestBody AiQueryRequest request) {
        return aiService.query(request);
    }

    @PostMapping("/chat")
    public BaseResponse<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        return aiService.chat(request);
    }
}