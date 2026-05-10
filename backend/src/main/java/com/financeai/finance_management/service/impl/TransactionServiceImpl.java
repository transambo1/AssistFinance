package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.dto.request.AnomalyRequest;
import com.financeai.finance_management.dto.request.TransactionChangedEvent;
import com.financeai.finance_management.dto.request.TransactionFilterRequest;
import com.financeai.finance_management.dto.request.UpsertTransactionRequest;
import com.financeai.finance_management.dto.response.AnomalyResponse;
import com.financeai.finance_management.dto.response.BasePaginationResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.TransactionResponse;
import com.financeai.finance_management.entity.Category;
import com.financeai.finance_management.entity.Transaction;
import com.financeai.finance_management.entity.User;
import com.financeai.finance_management.enums.TransactionStatus;
import com.financeai.finance_management.enums.TransactionType;
import com.financeai.finance_management.exception.exception.AppException;
import com.financeai.finance_management.exception.exception.ErrorCode;
import com.financeai.finance_management.mapper.TransactionMapper;
import com.financeai.finance_management.repository.CategoryRepository;
import com.financeai.finance_management.repository.TransactionRepository;
import com.financeai.finance_management.repository.UserRepository;
import com.financeai.finance_management.service.AnomalyService;
import com.financeai.finance_management.service.IBudgetService;
import com.financeai.finance_management.service.ITransactionService;
import com.financeai.finance_management.specification.TransactionSpecification;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements ITransactionService {

  private final TransactionRepository transactionRepository;
  private final UserRepository userRepository;
  private final CategoryRepository categoryRepository;

  private final IBudgetService budgetService;
  private final TransactionMapper transactionMapper;
  private final ApplicationEventPublisher eventPublisher;
  private final AnomalyService anomalyService;

  @Override
  @Transactional
  public BaseResponse<TransactionResponse> createTransaction(UpsertTransactionRequest request) {

    var userContext = budgetService.getCurrentUserId();
    User user =
        userRepository
            .findById(userContext)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    Category category =
        categoryRepository
            .findById(request.getCategoryId())
            .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

    AnomalyResponse anomalyResponse = null;

    if (Boolean.TRUE.equals(request.getIsAuto())) {
      anomalyResponse = detectAnomaly(request, user.getId());
    }

    Transaction transaction =
        Transaction.builder()
            .id(UUID.randomUUID().toString())
            .user(user)
            .category(category)
            .amount(request.getAmount())
            .type(request.getType())
            .note(request.getNote())
            .imageUrl(request.getImageUrl())
            .isAuto(request.getIsAuto())
            .transactionDate(
                request.getTransactionDate() != null
                    ? request.getTransactionDate()
                    : Instant.now().toEpochMilli())
            .status(TransactionStatus.SUCCESS)
            .isAnomaly(anomalyResponse != null && anomalyResponse.isAnomaly())
            .anomalyMessage(
                anomalyResponse != null && anomalyResponse.isAnomaly()
                    ? "Chi tiêu bất thường"
                    : null)
            .build();

    transactionRepository.save(transaction);
    updateUserBalance(user, request.getAmount(), request.getType(), false);

    TransactionResponse response = transactionMapper.toResponse(transaction);

    if (anomalyResponse != null) {
      response.setZScore(anomalyResponse.getZScore());
    }

    eventPublisher.publishEvent(
        new TransactionChangedEvent(
            user.getId(), request.getType(), transaction.getTransactionDate()));

    return BaseResponse.ok(response);
  }

  @Override
  @Transactional
  public BaseResponse<TransactionResponse> updateTransaction(
      String id, UpsertTransactionRequest request) {
    Transaction transaction =
        transactionRepository
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

    var userContext = budgetService.getCurrentUserId();
    User user =
        userRepository
            .findById(userContext)
            .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

    updateUserBalance(user, transaction.getAmount(), transaction.getType(), true);

    if (!transaction.getCategory().getId().equals(request.getCategoryId())) {
      Category newCategory =
          categoryRepository
              .findById(request.getCategoryId())
              .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));
      transaction.setCategory(newCategory);
    }

    transaction.setAmount(request.getAmount());
    transaction.setNote(request.getNote());
    transaction.setType(request.getType());
    transaction.setImageUrl(request.getImageUrl());

    transactionRepository.save(transaction);
    updateUserBalance(user, transaction.getAmount(), transaction.getType(), false);

    eventPublisher.publishEvent(
        new TransactionChangedEvent(
            user.getId(), request.getType(), transaction.getTransactionDate()));

    log.info("Updated transaction {}: New amount {}", id, transaction.getAmount());
    return BaseResponse.ok(transactionMapper.toResponse(transaction));
  }

  @Override
  @Transactional
  public BaseResponse<Void> deleteTransaction(String id) {
    Transaction transaction =
        transactionRepository
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

    var userContext = budgetService.getCurrentUserId();
    User user =
        userRepository
            .findById(userContext)
            .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

    updateUserBalance(user, transaction.getAmount(), transaction.getType(), true);
    transaction.setDeletedAt(Instant.now().toEpochMilli());
    transaction.deactivate();
    transactionRepository.save(transaction);

    eventPublisher.publishEvent(
        new TransactionChangedEvent(
            user.getId(), transaction.getType(), transaction.getTransactionDate()));

    return BaseResponse.ok(null);
  }

  @Override
  @Transactional(readOnly = true)
  public BaseResponse<BasePaginationResponse<TransactionResponse>> getTransactionHistories(
      TransactionFilterRequest request) {

    var userContext = budgetService.getCurrentUserId();

    Specification<Transaction> spec =
        TransactionSpecification.builder()
            .withKeyword(request.getSearch())
            .withRegistrationDateRange(request.getStartDate(), request.getEndDate())
            .withUserId(userContext)
            .withCategoryId(request.getCategoryId())
            .build();

    Pageable pageable = request.pageable();

    Page<TransactionResponse> pageResponse =
        transactionRepository.findAll(spec, pageable).map(transactionMapper::toResponse);

    return BaseResponse.ok(BasePaginationResponse.of(pageResponse));
  }

  private void updateUserBalance(
      User user, BigDecimal amount, TransactionType type, boolean isReverting) {
    BigDecimal adjustment = TransactionType.INCOME.equals(type) ? amount : amount.negate();

    if (isReverting) {
      adjustment = adjustment.negate();
    }

    BigDecimal newBalance = user.getCurrentBalance().add(adjustment);
    user.setCurrentBalance(newBalance);
    userRepository.save(user);
  }

  private AnomalyResponse detectAnomaly(UpsertTransactionRequest request, String userId) {
    if (request.getType() != TransactionType.EXPENSE) {
      return null;
    }

    var oldAmounts =
        transactionRepository.findAmountsByUserAndCategory(userId, request.getCategoryId());
    log.info("OLD AMOUNTS = {}", oldAmounts);

    if (oldAmounts.size() < 5) {
      log.warn("NOT ENOUGH DATA: {}", oldAmounts.size());
      return null;
    }

    AnomalyRequest anomalyRequest = new AnomalyRequest();
    anomalyRequest.setAmounts(oldAmounts);
    anomalyRequest.setNewAmount(request.getAmount().doubleValue());

    log.info("NEW AMOUNT = {}", request.getAmount());

    AnomalyResponse response = anomalyService.detect(anomalyRequest);

    if (response.isAnomaly()) {
      log.warn(
          """
                    🚨 Giao dịch bất thường!
                    Amount: {}
                    Mean: {}
                    Std: {}
                    """,
          request.getAmount(),
          response.getMean(),
          response.getStd());
    }

    return response;
  }
}
