package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.request.AiParseRequest;
import com.financeai.finance_management.dto.response.TransactionResponse;

import java.util.List;

public interface IAiService {
    List<TransactionResponse> parseAndSaveTransaction(AiParseRequest request);
}