package com.financeai.finance_management.controller;

import com.financeai.finance_management.dto.request.CategoryCreationRequest;
import com.financeai.finance_management.dto.request.CategoryUpdateRequest;
import com.financeai.finance_management.dto.response.CategoryResponse;
import com.financeai.finance_management.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ICategoryService categoryService;

    @PostMapping
    public CategoryResponse createCategory(@RequestBody CategoryCreationRequest request) {
        return categoryService.createCategory(request);
    }

    @PutMapping("/{id}")
    public CategoryResponse updateCategory(@PathVariable String id,
                                           @RequestBody CategoryUpdateRequest request) {
        return categoryService.updateCategory(id, request);
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategoryById(@PathVariable String id) {
        return categoryService.getCategoryById(id);
    }

    @GetMapping
    public List<CategoryResponse> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @GetMapping("/user/{userId}")
    public List<CategoryResponse> getCategoriesByUserId(@PathVariable String userId) {
        return categoryService.getCategoriesByUserId(userId);
    }

    @GetMapping("/user/{userId}/type/{type}")
    public List<CategoryResponse> getCategoriesByType(@PathVariable String userId,
                                                      @PathVariable String type) {
        return categoryService.getCategoriesByType(userId, type);
    }

    @GetMapping("/user/{userId}/available/{type}")
    public List<CategoryResponse> getAvailableCategories(@PathVariable String userId,
                                                         @PathVariable String type) {
        return categoryService.getAvailableCategories(userId, type);
    }

    @PatchMapping("/{id}/archive")
    public String archiveCategory(@PathVariable String id) {
        categoryService.archiveCategory(id);
        return "Category archived successfully";
    }

    @PatchMapping("/{id}/unarchive")
    public String unarchiveCategory(@PathVariable String id) {
        categoryService.unarchiveCategory(id);
        return "Category unarchived successfully";
    }

    @DeleteMapping("/{id}")
    public String softDeleteCategory(@PathVariable String id,
                                     @RequestParam String userId) {
        categoryService.softDeleteCategory(id, userId);
        return "Category deleted successfully";
    }

    @PatchMapping("/{id}/increase-usage")
    public String increaseUsageCount(@PathVariable String id) {
        categoryService.increaseUsageCount(id);
        return "Category usage count increased";
    }
}