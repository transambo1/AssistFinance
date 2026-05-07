package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.request.AiChatRequest;
import com.financeai.finance_management.dto.request.AiParseRequest;
import com.financeai.finance_management.dto.request.AiQueryRequest;
import com.financeai.finance_management.dto.response.*;

import java.util.List;

public interface IAiService {
    BaseResponse<List<TransactionResponse>> parseAndSaveTransaction(AiParseRequest request);
    BaseResponse<AiQueryResponse> query(AiQueryRequest request);
    BaseResponse<AiChatResponse> chat(AiChatRequest request);
    BaseResponse<SpendingTrendResponse> analyzeSpendingTrend();
}