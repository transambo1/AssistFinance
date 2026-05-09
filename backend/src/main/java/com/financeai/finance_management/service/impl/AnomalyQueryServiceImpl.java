package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.dto.response.AnomalyItemResponse;
import com.financeai.finance_management.repository.TransactionRepository;
import com.financeai.finance_management.service.IBudgetService;
import com.financeai.finance_management.service.IAnomalyQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnomalyQueryServiceImpl
        implements IAnomalyQueryService {

    private final TransactionRepository transactionRepository;

    private final IBudgetService budgetService;

    @Override
    public List<AnomalyItemResponse> getTodayAnomalies() {

        String userId =
                budgetService.getCurrentUserId();

        return transactionRepository
                .findTodayAnomalies(userId)
                .stream()
                .map(t -> AnomalyItemResponse.builder()
                        .transactionId(t.getId())
                        .amount(t.getAmount())
                        .note(t.getNote())
                        .categoryName(
                                t.getCategory().getName()
                        )
                        .message(
                                t.getAnomalyMessage()
                        )
                        .transactionDate(
                                t.getTransactionDate()
                        )
                        .build())
                .toList();
    }
}