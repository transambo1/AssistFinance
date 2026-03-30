package com.financeai.finance_management.controller;

import com.financeai.finance_management.dto.request.BudgetCreationRequest;
import com.financeai.finance_management.dto.request.BudgetFilterRequest;
import com.financeai.finance_management.dto.request.BudgetUpdateRequest;
import com.financeai.finance_management.dto.response.BasePaginationResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.BudgetResponse;
import com.financeai.finance_management.service.IBudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final IBudgetService budgetService;

    @PostMapping
    public BaseResponse<BudgetResponse> createBudget(@RequestBody BudgetCreationRequest request) {
        return budgetService.createBudget(request);
    }

    @PutMapping("/{id}")
    public BaseResponse<BudgetResponse> updateBudget(
            @PathVariable String id,
            @RequestBody BudgetUpdateRequest request
    ) {
        return budgetService.updateBudget(id, request);
    }

    @GetMapping("/{id}")
    public BaseResponse<BudgetResponse> getBudgetById(@PathVariable String id) {
        return budgetService.getBudgetById(id);
    }

    @GetMapping
    public BaseResponse<BasePaginationResponse<BudgetResponse>> getAllBudgets(BudgetFilterRequest request) {
        return budgetService.getAllBudgets(request);
    }

    @PatchMapping("/{id}/activate")
    public BaseResponse<String> activateBudget(@PathVariable String id) {
        return budgetService.activateBudget(id);
    }

    @PatchMapping("/{id}/deactivate")
    public BaseResponse<String> deactivateBudget(@PathVariable String id) {
        return budgetService.deactivateBudget(id);
    }

    @DeleteMapping("/{id}")
    public BaseResponse<String> deleteBudget(@PathVariable String id) {
        return budgetService.softDeleteBudget(id);
    }
}