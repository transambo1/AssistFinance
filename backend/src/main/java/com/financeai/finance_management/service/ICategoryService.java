package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.request.CategoryCreationRequest;
import com.financeai.finance_management.dto.request.CategoryUpdateRequest;
import com.financeai.finance_management.dto.response.CategoryResponse;

import java.util.List;

public interface ICategoryService {
    void createDefaultCategories(String userId);
    CategoryResponse createCategory(CategoryCreationRequest request);

    CategoryResponse updateCategory(String id, CategoryUpdateRequest request);

    CategoryResponse getCategoryById(String id);

    List<CategoryResponse> getAllCategories();

    List<CategoryResponse> getCategoriesByUserId(String userId);

    List<CategoryResponse> getCategoriesByType(String userId, String type);

    List<CategoryResponse> getAvailableCategories(String userId, String type);

    void archiveCategory(String id);

    void unarchiveCategory(String id);

    void softDeleteCategory(String id, String userId);

    void increaseUsageCount(String id);
}