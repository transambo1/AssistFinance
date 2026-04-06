package com.financeai.finance_management.controller;

import com.financeai.finance_management.dto.request.*;
import com.financeai.finance_management.dto.response.BasePaginationResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.CategoryResponse;
import com.financeai.finance_management.dto.response.TransactionResponse;
import com.financeai.finance_management.service.ICategoryService;
import com.financeai.finance_management.service.ISalaryConfigService;
import com.financeai.finance_management.service.ITransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Transaction APIs", description = "Grouped Transaction APIs")
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final ITransactionService transactionService;
    private final ISalaryConfigService salaryConfigService;

    @Operation(summary = "Create Transaction", description = "Create a Transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction created"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @PostMapping
    public ResponseEntity<BaseResponse<TransactionResponse>> createTransaction(
            @RequestBody UpsertTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createTransaction(request));
    }

    @Operation(summary = "Update Transaction", description = "Update Transaction information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @PutMapping("/{transactionId}")
    public ResponseEntity<BaseResponse<TransactionResponse>> updateTransaction(
            @PathVariable String transactionId,
            @RequestBody UpsertTransactionRequest request) {
        return ResponseEntity.ok(transactionService.updateTransaction(transactionId, request));
    }

    @Operation(summary = "Get list Transactions", description = "Get list Transactions by filter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Has result, return Transactions"),
            @ApiResponse(responseCode = "400", description = "Validation failed, entity not found")
    })
    @GetMapping
    public ResponseEntity<BaseResponse<BasePaginationResponse<TransactionResponse>>> getListTransactionsByFilter(
            @ParameterObject TransactionFilterRequest request) {
        return ResponseEntity.ok(transactionService.getTransactionHistories(request));
    }

    @Operation(summary = "Upsert Auto Config", description = "Create or Update auto income/expense config")
    @PostMapping("/auto-configs")
    public ResponseEntity<BaseResponse<Void>> upsertSalaryConfig(
            @RequestBody SalaryConfigReq request) {
        return ResponseEntity.ok(salaryConfigService.upsertConfig(request));
    }

    @Operation(summary = "Toggle Auto Config", description = "Enable or Disable an auto transaction config")
    @PatchMapping("/auto-configs/{configId}/toggle")
    public ResponseEntity<BaseResponse<Void>> toggleAutoConfig(
            @PathVariable String configId) {
        return ResponseEntity.ok(salaryConfigService.toggleActive(configId));
    }

    @Operation(summary = "Delete Auto Config", description = "Soft delete an auto transaction config")
    @DeleteMapping("/auto-configs/{configId}")
    public ResponseEntity<BaseResponse<Void>> deleteAutoConfig(
            @PathVariable String configId) {
        return ResponseEntity.ok(salaryConfigService.deleteConfig(configId));
    }
}
