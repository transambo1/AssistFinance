package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.request.BudgetCreationRequest;
import com.financeai.finance_management.dto.request.BudgetFilterRequest;
import com.financeai.finance_management.dto.request.BudgetUpdateRequest;
import com.financeai.finance_management.dto.response.BasePaginationResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.BudgetResponse;

public interface IBudgetService {
    BaseResponse<BudgetResponse> createBudget(BudgetCreationRequest request);

    BaseResponse<BudgetResponse> updateBudget(String id, BudgetUpdateRequest request);

    BaseResponse<BudgetResponse> getBudgetById(String id);

    BaseResponse<BasePaginationResponse<BudgetResponse>> getAllBudgets(BudgetFilterRequest request);

    BaseResponse<String> activateBudget(String id);

    BaseResponse<String> deactivateBudget(String id);

    BaseResponse<String> softDeleteBudget(String id);
}