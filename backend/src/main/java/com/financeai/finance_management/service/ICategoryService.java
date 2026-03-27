package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.request.CategoryCreationRequest;
import com.financeai.finance_management.dto.request.CategoryFilterRequest;
import com.financeai.finance_management.dto.request.CategoryUpdateRequest;
import com.financeai.finance_management.dto.response.BasePaginationResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.CategoryResponse;

import java.util.List;

public interface ICategoryService {
    void createDefaultCategories(String userId);
    BaseResponse<CategoryResponse> createCategory(CategoryCreationRequest request);

    BaseResponse<CategoryResponse> createCategory(CategoryCreationRequest request);

    BaseResponse<CategoryResponse> updateCategory(String id, CategoryUpdateRequest request);

    BaseResponse<CategoryResponse> getCategoryById(String id);

    BaseResponse<BasePaginationResponse<CategoryResponse>> getAllCategories(CategoryFilterRequest request);

    BaseResponse<List<CategoryResponse>> getCategoriesByUserId(String userId);

    BaseResponse<List<CategoryResponse>> getCategoriesByType(String userId, String type);

    BaseResponse<List<CategoryResponse>> getAvailableCategories(String userId, String type);

    BaseResponse<String> archiveCategory(String id);

    BaseResponse<String> unarchiveCategory(String id);

    BaseResponse<String> softDeleteCategory(String id, String userId);

    BaseResponse<String> increaseUsageCount(String id);
}