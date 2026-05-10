package com.financeai.finance_management.controller;

import com.financeai.finance_management.dto.request.AiChatRequest;
import com.financeai.finance_management.dto.request.AiParseRequest;
import com.financeai.finance_management.dto.request.AiQueryRequest;
import com.financeai.finance_management.dto.response.*;
import com.financeai.finance_management.service.IAiService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

  @GetMapping("/trend")
  public BaseResponse<SpendingTrendResponse> analyzeTrend() {
    return aiService.analyzeSpendingTrend();
  }

  @GetMapping("/saving-advice")
  public BaseResponse<SavingAdviceResponse> getSavingAdvice() {
    return aiService.getSavingAdvice();
  }
}
