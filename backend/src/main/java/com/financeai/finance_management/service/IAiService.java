package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.request.AiChatRequest;
import com.financeai.finance_management.dto.request.AiParseRequest;
import com.financeai.finance_management.dto.request.AiQueryRequest;
import com.financeai.finance_management.dto.response.AiChatResponse;
import com.financeai.finance_management.dto.response.AiQueryResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.TransactionResponse;

import java.util.List;

public interface IAiService {
    BaseResponse<List<TransactionResponse>> parseAndSaveTransaction(AiParseRequest request);
    BaseResponse<AiQueryResponse> query(AiQueryRequest request);
    BaseResponse<AiChatResponse> chat(AiChatRequest request);
}