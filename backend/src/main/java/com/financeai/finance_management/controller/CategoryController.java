package com.financeai.finance_management.controller;

import com.financeai.finance_management.dto.request.CategoryCreationRequest;
import com.financeai.finance_management.dto.request.CategoryFilterRequest;
import com.financeai.finance_management.dto.request.CategoryUpdateRequest;
import com.financeai.finance_management.dto.response.BasePaginationResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.CategoryResponse;
import com.financeai.finance_management.service.ICategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Category APIs", description = "Grouped Category APIs")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ICategoryService categoryService;

    @Operation(summary = "Create Category", description = "Create a category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Category created"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @PostMapping
    public ResponseEntity<BaseResponse<CategoryResponse>> createCategory(
            @RequestBody CategoryCreationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(request));
    }

    @Operation(summary = "Update Category", description = "Update category information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<CategoryResponse>> updateCategory(
            @PathVariable String id,
            @RequestBody CategoryUpdateRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @Operation(summary = "Get Category By Id", description = "Get category detail by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category found"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<CategoryResponse>> getCategoryById(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @Operation(summary = "Get list categories", description = "Get list categories by filter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Has result, return categories"),
            @ApiResponse(responseCode = "400", description = "Validation failed, entity not found")
    })
    @GetMapping
    public ResponseEntity<BaseResponse<BasePaginationResponse<CategoryResponse>>> getListCategoriesByFilter(
            @ParameterObject CategoryFilterRequest request) {
        return ResponseEntity.ok(categoryService.getAllCategories(request));
    }

    @Operation(summary = "Get Categories By User Id", description = "Get all categories of a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getCategoriesByUserId(
            @PathVariable String userId) {
        return ResponseEntity.ok(categoryService.getCategoriesByUserId(userId));
    }

    @Operation(summary = "Get Categories By Type", description = "Get categories by type of a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    })
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getCategoriesByType(
            @PathVariable String userId,
            @PathVariable String type) {
        return ResponseEntity.ok(categoryService.getCategoriesByType(userId, type));
    }

    @Operation(summary = "Get Available Categories", description = "Get available categories by type of a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Available categories retrieved successfully")
    })
    @GetMapping("/user/{userId}/available/{type}")
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getAvailableCategories(
            @PathVariable String userId,
            @PathVariable String type) {
        return ResponseEntity.ok(categoryService.getAvailableCategories(userId, type));
    }

    @Operation(summary = "Archive Category", description = "Archive a category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category archived successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @PatchMapping("/{id}/archive")
    public ResponseEntity<BaseResponse<String>> archiveCategory(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.archiveCategory(id));
    }

    @Operation(summary = "Unarchive Category", description = "Unarchive a category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category unarchived successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @PatchMapping("/{id}/unarchive")
    public ResponseEntity<BaseResponse<String>> unarchiveCategory(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.unarchiveCategory(id));
    }

    @Operation(summary = "Soft Delete Category", description = "Soft delete a category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<String>> softDeleteCategory(
            @PathVariable String id,
            @RequestParam String userId) {
        return ResponseEntity.ok(categoryService.softDeleteCategory(id, userId));
    }

    @Operation(summary = "Increase Usage Count", description = "Increase usage count of a category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category usage count increased successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @PatchMapping("/{id}/increase-usage")
    public ResponseEntity<BaseResponse<String>> increaseUsageCount(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.increaseUsageCount(id));
    }
}