package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.dto.response.AnomalyCountResponse;
import com.financeai.finance_management.dto.response.AnomalyDetailResponse;
import com.financeai.finance_management.dto.response.AnomalyItemResponse;
import com.financeai.finance_management.entity.Transaction;
import com.financeai.finance_management.exception.exception.AppException;
import com.financeai.finance_management.exception.exception.ErrorCode;
import com.financeai.finance_management.repository.TransactionRepository;
import com.financeai.finance_management.service.IAnomalyQueryService;
import com.financeai.finance_management.service.IBudgetService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnomalyQueryServiceImpl implements IAnomalyQueryService {

  private final TransactionRepository transactionRepository;
  private final IBudgetService budgetService;

  @Override
  public AnomalyCountResponse countTodayAnomalies() {
    String userId = budgetService.getCurrentUserId();
    long count = transactionRepository.countTodayAnomalies(userId);

    return AnomalyCountResponse.builder().count(count).build();
  }

  @Override
  public List<AnomalyItemResponse> getTodayAnomalies() {
    String userId = budgetService.getCurrentUserId();

    return transactionRepository.findTodayAnomalies(userId).stream()
        .map(
            t ->
                AnomalyItemResponse.builder()
                    .transactionId(t.getId())
                    .amount(t.getAmount())
                    .note(t.getNote())
                    .categoryName(t.getCategory().getName())
                    .message(t.getAnomalyMessage())
                    .transactionDate(t.getTransactionDate())
                    .build())
        .toList();
  }

  @Override
  public AnomalyDetailResponse getAnomalyDetail(String transactionId) {
    String userId = budgetService.getCurrentUserId();

    Transaction transaction =
        transactionRepository
            .findAnomalyDetailById(userId, transactionId)
            .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

    return AnomalyDetailResponse.builder()
        .transactionId(transaction.getId())
        .amount(transaction.getAmount())
        .note(transaction.getNote())
        .categoryId(transaction.getCategory().getId())
        .categoryName(transaction.getCategory().getName())
        .categoryColor(transaction.getCategory().getColor())
        .type(transaction.getType())
        .message(transaction.getAnomalyMessage())
        .transactionDate(transaction.getTransactionDate())
        .imageUrl(transaction.getImageUrl())
        .isAuto(transaction.isAuto())
        .createdAt(transaction.getCreatedAt())
        .updatedAt(transaction.getUpdatedAt())
        .build();
  }
}
