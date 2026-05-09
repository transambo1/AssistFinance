package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.request.AiChatRequest;
import com.financeai.finance_management.dto.request.AiParseRequest;
import com.financeai.finance_management.dto.request.AiQueryRequest;
import com.financeai.finance_management.dto.response.*;

import java.util.List;
import com.financeai.finance_management.dto.response.SavingAdviceResponse;
import com.financeai.finance_management.dto.response.BaseResponse;

public interface IAiService {
    BaseResponse<List<TransactionResponse>> parseAndSaveTransaction(AiParseRequest request);
    BaseResponse<AiQueryResponse> query(AiQueryRequest request);
    BaseResponse<AiChatResponse> chat(AiChatRequest request);
    BaseResponse<SpendingTrendResponse> analyzeSpendingTrend();
    BaseResponse<SavingAdviceResponse> getSavingAdvice();
}