package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.request.TransactionFilterRequest;
import com.financeai.finance_management.dto.request.UpsertTransactionRequest;
import com.financeai.finance_management.dto.response.BasePaginationResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.TransactionResponse;

import java.util.List;

public interface ITransactionService {
  BaseResponse<TransactionResponse> createTransaction(UpsertTransactionRequest request);

  BaseResponse<TransactionResponse> updateTransaction(String id, UpsertTransactionRequest request);

  BaseResponse<Void> deleteTransaction(String id);

  BaseResponse<List<TransactionResponse>> getTransactionHistories(
          TransactionFilterRequest request);
}
