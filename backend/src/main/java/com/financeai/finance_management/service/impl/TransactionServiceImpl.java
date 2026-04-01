package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.config.context.UserContext;
import com.financeai.finance_management.config.context.UserContextHolder;
import com.financeai.finance_management.dto.request.TransactionFilterRequest;
import com.financeai.finance_management.dto.request.UpsertTransactionRequest;
import com.financeai.finance_management.dto.response.BasePaginationResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.CategoryResponse;
import com.financeai.finance_management.dto.response.TransactionResponse;
import com.financeai.finance_management.entity.Category;
import com.financeai.finance_management.entity.Transaction;
import com.financeai.finance_management.entity.User;
import com.financeai.finance_management.enums.TransactionType;
import com.financeai.finance_management.exception.exception.AppException;
import com.financeai.finance_management.exception.exception.ErrorCode;
import com.financeai.finance_management.mapper.TransactionMapper;
import com.financeai.finance_management.repository.*;
import com.financeai.finance_management.service.IBudgetService;
import com.financeai.finance_management.service.ITransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements ITransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private final IBudgetService budgetService;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional
    public BaseResponse<Void> createTransaction(UpsertTransactionRequest request) {

        var userContext = budgetService.getCurrentUserId();
        User user = userRepository.findById(userContext)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .category(category)
                .amount(request.getAmount())
                .type(request.getType())
                .note(request.getNote())
                .imageUrl(request.getImageUrl())
                .isAuto(request.getIsAuto())
                .build();
        transactionRepository.save(transaction);

        updateUserBalance(user, request.getAmount(), request.getType(), false);

        return BaseResponse.ok(null);
    }

    @Override
    @Transactional
    public BaseResponse<Void> updateTransaction(String id, UpsertTransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

        var userContext = budgetService.getCurrentUserId();
        User user = userRepository.findById(userContext)
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

        updateUserBalance(user, transaction.getAmount(), transaction.getType(), true);

        if (!transaction.getCategory().getId().equals(request.getCategoryId())) {
            Category newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));
            transaction.setCategory(newCategory);
        }

        transaction.setAmount(request.getAmount());
        transaction.setNote(request.getNote());
        transaction.setImageUrl(request.getImageUrl());

        transactionRepository.save(transaction);
        updateUserBalance(user, transaction.getAmount(), transaction.getType(), false);

        log.info("Updated transaction {}: New amount {}", id, transaction.getAmount());
        return BaseResponse.ok(null);
    }

    @Override
    @Transactional
    public BaseResponse<Void> deleteTransaction(String id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

        var userContext = budgetService.getCurrentUserId();
        User user = userRepository.findById(userContext)
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

        updateUserBalance(user, transaction.getAmount(), transaction.getType(), true);
        transaction.setDeletedAt(Instant.now().toEpochMilli());
        transaction.deactivate();
        transactionRepository.save(transaction);
        return BaseResponse.ok(null);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<BasePaginationResponse<TransactionResponse>> getTransactionHistories(TransactionFilterRequest request) {
        Specification<Transaction> spec = request.specification();
        Pageable pageable = request.pageable();

        Page<TransactionResponse> pageResponse =
                transactionRepository.findAll(spec, pageable).map(transactionMapper::toResponse);

        return BaseResponse.ok(BasePaginationResponse.of(pageResponse));
    }

    private void updateUserBalance(User user, BigDecimal amount, TransactionType type, boolean isReverting) {
        BigDecimal adjustment = (TransactionType.INCOME.equals(type)) ? amount : amount.negate();

        if (isReverting) {
            adjustment = adjustment.negate();
        }

        BigDecimal newBalance = user.getCurrentBalance().add(adjustment);

        user.setCurrentBalance(newBalance);
        userRepository.save(user);
    }
}
