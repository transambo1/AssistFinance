package com.financeai.finance_management.controller;

import com.financeai.finance_management.dto.request.CategoryCreationRequest;
import com.financeai.finance_management.dto.request.CategoryUpdateRequest;
import com.financeai.finance_management.dto.response.CategoryResponse;
import com.financeai.finance_management.service.ICategoryService;
import lombok.RequiredArgsConstructor;
<<<<<<< Updated upstream
=======
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
>>>>>>> Stashed changes
import org.springframework.web.bind.annotation.*;

import java.util.List;

<<<<<<< Updated upstream
=======
@Tag(name = "Category APIs", description = "Grouped Category APIs")
>>>>>>> Stashed changes
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ICategoryService categoryService;

<<<<<<< Updated upstream
    @PostMapping
    public CategoryResponse createCategory(@RequestBody CategoryCreationRequest request) {
        return categoryService.createCategory(request);
    }

=======
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
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
    @GetMapping
    public List<CategoryResponse> getAllCategories() {
        return categoryService.getAllCategories();
=======
    @Operation(summary = "Get list categories", description = "Get list categories by filter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Has result, return categories"),
            @ApiResponse(responseCode = "400", description = "Validation failed, entity not found")
    })
    @GetMapping
    public ResponseEntity<BaseResponse<BasePaginationResponse<CategoryResponse>>> getListCategoriesByFilter(
            @ParameterObject CategoryFilterRequest request) {
        return ResponseEntity.ok(categoryService.getAllCategories(request));
>>>>>>> Stashed changes
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
    public ResponseEntity<BaseResponse<String>> softDeleteCategory(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.softDeleteCategory(id));
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