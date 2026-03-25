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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "Category APIs", description = "Grouped Category APIs")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ICategoryService categoryService;

    @Operation(summary = "Create Category", description = "Create a Category information")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "201", description = "Category created"),
                    @ApiResponse(responseCode = "400", description = "Validation failed"),
            })
    @PostMapping
    public ResponseEntity<BaseResponse<CategoryResponse>>createCategory(@RequestBody CategoryCreationRequest request) {
        return ResponseEntity.ok(categoryService.createCategory(request));
    }


    //TODO: Lanh handle lai theo style tren nha
    @PutMapping("/{id}")
    public CategoryResponse updateCategory(@PathVariable String id,
                                           @RequestBody CategoryUpdateRequest request) {
        return categoryService.updateCategory(id, request);
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategoryById(@PathVariable String id) {
        return categoryService.getCategoryById(id);
    }

    @Operation(summary = "Get list categories", description = "Get list categories by filter")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Has result, return categories."),
                    @ApiResponse(responseCode = "400", description = "Validation failed, entity not found..."),
            })
    @GetMapping
    public ResponseEntity<BaseResponse<BasePaginationResponse<CategoryResponse>>>
    getListCoursesByFilter(@ParameterObject CategoryFilterRequest request) {
        return ResponseEntity.ok(this.categoryService.getAllCategories(request));
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