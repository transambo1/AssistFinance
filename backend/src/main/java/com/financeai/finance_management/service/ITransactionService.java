package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.request.TransactionFilterRequest;
import com.financeai.finance_management.dto.request.UpsertTransactionRequest;
import com.financeai.finance_management.dto.response.BasePaginationResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.TransactionResponse;

public interface ITransactionService {
    BaseResponse<Void> createTransaction(UpsertTransactionRequest request);

    BaseResponse<Void> updateTransaction(String id, UpsertTransactionRequest request);

    BaseResponse<Void> deleteTransaction(String id);

    BaseResponse<BasePaginationResponse<TransactionResponse>> getTransactionHistories(TransactionFilterRequest request);

}
