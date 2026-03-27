package com.financeai.finance_management.controller;

import com.financeai.finance_management.dto.request.BudgetCreationRequest;
import com.financeai.finance_management.dto.request.BudgetFilterRequest;
import com.financeai.finance_management.dto.request.BudgetUpdateRequest;
import com.financeai.finance_management.dto.response.BasePaginationResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.BudgetResponse;
import com.financeai.finance_management.service.IBudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Budget APIs", description = "Grouped Budget APIs")
@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final IBudgetService budgetService;

    @Operation(summary = "Create Budget", description = "Create a budget")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Budget created"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @PostMapping
    public ResponseEntity<BaseResponse<BudgetResponse>> createBudget(
            @RequestBody BudgetCreationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetService.createBudget(request));
    }

    @Operation(summary = "Update Budget", description = "Update budget information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Budget updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Budget not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<BudgetResponse>> updateBudget(
            @PathVariable String id,
            @RequestBody BudgetUpdateRequest request) {
        return ResponseEntity.ok(budgetService.updateBudget(id, request));
    }

    @Operation(summary = "Get Budget By Id", description = "Get budget detail by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Budget found"),
            @ApiResponse(responseCode = "404", description = "Budget not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<BudgetResponse>> getBudgetById(@PathVariable String id) {
        return ResponseEntity.ok(budgetService.getBudgetById(id));
    }

    @Operation(summary = "Get list budgets", description = "Get list budgets by filter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Has result, return budgets"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @GetMapping
    public ResponseEntity<BaseResponse<BasePaginationResponse<BudgetResponse>>> getAllBudgets(
            @ParameterObject BudgetFilterRequest request) {
        return ResponseEntity.ok(budgetService.getAllBudgets(request));
    }

    @Operation(summary = "Activate Budget", description = "Activate a budget")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Budget activated successfully"),
            @ApiResponse(responseCode = "404", description = "Budget not found")
    })
    @PatchMapping("/{id}/activate")
    public ResponseEntity<BaseResponse<String>> activateBudget(@PathVariable String id) {
        return ResponseEntity.ok(budgetService.activateBudget(id));
    }

    @Operation(summary = "Deactivate Budget", description = "Deactivate a budget")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Budget deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Budget not found")
    })
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<BaseResponse<String>> deactivateBudget(@PathVariable String id) {
        return ResponseEntity.ok(budgetService.deactivateBudget(id));
    }

    @Operation(summary = "Soft Delete Budget", description = "Soft delete a budget")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Budget deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Budget not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<String>> softDeleteBudget(@PathVariable String id) {
        return ResponseEntity.ok(budgetService.softDeleteBudget(id));
    }
}