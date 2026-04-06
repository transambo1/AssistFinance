package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.dto.request.SalaryConfigReq;
import com.financeai.finance_management.dto.request.UpsertTransactionRequest;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.entity.Category;
import com.financeai.finance_management.entity.SalaryConfig;
import com.financeai.finance_management.entity.SalaryJobLog;
import com.financeai.finance_management.entity.User;
import com.financeai.finance_management.enums.JobStatus;
import com.financeai.finance_management.enums.TransactionType;
import com.financeai.finance_management.exception.exception.AppException;
import com.financeai.finance_management.exception.exception.ErrorCode;
import com.financeai.finance_management.repository.CategoryRepository;
import com.financeai.finance_management.repository.JobLogRepository;
import com.financeai.finance_management.repository.SalaryConfigRepository;
import com.financeai.finance_management.repository.UserRepository;
import com.financeai.finance_management.service.ISalaryConfigService;
import com.financeai.finance_management.service.ITransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryConfigServiceImpl implements ISalaryConfigService {

    private final SalaryConfigRepository configRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ITransactionService transactionService;
    private final JobLogRepository jobLogRepository;

    @Override
    @Transactional
    public BaseResponse<Void> upsertConfig(SalaryConfigReq request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

        SalaryConfig config = SalaryConfig.builder()
                .id(request.getId() != null ? request.getId() : UUID.randomUUID().toString())
                .user(user)
                .category(category)
                .amount(request.getAmount())
                .payDay(request.getPayDay())
                .frequency(request.getFrequency())
                .type(request.getType())
                .description(request.getDescription())
                .build();

        configRepository.save(config);
        return BaseResponse.ok(null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Quan trọng để tách Transaction
    public void executeAutoJob(SalaryConfig config) {
        try {
            // Tách logic check balance và tạo request
            UpsertTransactionRequest txReq = getUpsertTransactionRequest(config);

            transactionService.createTransaction(txReq);

            saveJobLog(config, JobStatus.SUCCESS, null);

            config.setLastProcessed(Instant.now().toEpochMilli());
            configRepository.save(config);

        } catch (AppException e) {
            log.warn("Business error for config {}: {}", config.getId(), e.getMessage());
            saveJobLog(config, JobStatus.FAILED, e.getMessage());
        } catch (Exception e) {
            log.error("System error for config {}: ", config.getId(), e);
            saveJobLog(config, JobStatus.FAILED, "System Error: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public BaseResponse<Void> deleteConfig(String id) {
        SalaryConfig config = configRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

        config.setDeletedAt(Instant.now().toEpochMilli());
        configRepository.save(config);

        log.info("Soft deleted auto config: {}", id);
        return BaseResponse.ok(null);
    }

    @Override
    @Transactional
    public BaseResponse<Void> toggleActive(String id) {
        Optional<SalaryConfig> config = configRepository.findById(id);
        config.ifPresent(salaryConfig -> {
            salaryConfig.setActive(!salaryConfig.isActive());
            configRepository.save(salaryConfig);
            log.info("Activated auto config: {}", id);
        });
        return BaseResponse.ok(null);
    }

    private static UpsertTransactionRequest getUpsertTransactionRequest(SalaryConfig config) {
        User user = config.getUser();

        if (config.getType() == TransactionType.EXPENSE &&
                user.getCurrentBalance().compareTo(config.getAmount()) < 0) {
            throw new AppException(ErrorCode.CURRENT_BALANCE_NOT_ENOUGH);
        }

        UpsertTransactionRequest txReq = new UpsertTransactionRequest();
        txReq.setUserId(user.getId());
        txReq.setCategoryId(config.getCategory().getId());
        txReq.setAmount(config.getAmount());
        txReq.setType(config.getType());
        txReq.setNote("[Auto] " + config.getDescription());
        txReq.setIsAuto(true);
        return txReq;
    }

    private void saveJobLog(SalaryConfig config, JobStatus status, String error) {
        SalaryJobLog jobLog = SalaryJobLog.builder()
                .id(UUID.randomUUID().toString())
                .salaryConfig(config)
                .user(config.getUser())
                .status(status)
                .amount(config.getAmount())
                .errorMessage(error)
                .build();
        jobLogRepository.save(jobLog);
    }
}
