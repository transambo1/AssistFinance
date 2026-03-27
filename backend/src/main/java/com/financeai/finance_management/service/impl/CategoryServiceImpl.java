package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.dto.request.CategoryCreationRequest;
import com.financeai.finance_management.dto.request.CategoryFilterRequest;
import com.financeai.finance_management.dto.request.CategoryUpdateRequest;
import com.financeai.finance_management.dto.response.BasePaginationResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.CategoryResponse;
import com.financeai.finance_management.entity.Category;
import com.financeai.finance_management.enums.CategoryType;
import com.financeai.finance_management.exception.exception.AppException;
import com.financeai.finance_management.exception.exception.ErrorCode;
import com.financeai.finance_management.repository.CategoryRepository;
import com.financeai.finance_management.service.ICategoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class CategoryServiceImpl implements ICategoryService {

    CategoryRepository categoryRepository;

    @Override
    public void createDefaultCategories(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (!categoryRepository.findByUserId(userId.trim()).isEmpty()) {
            return;
        }

        List<Category> defaultCategories = List.of(
                Category.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId.trim())
                        .name("Ăn uống")
                        .type("EXPENSE")
                        .icon("food")
                        .color("#FF6B6B")
                        .isArchived(false)
                        .usageCount(0)
                        .build(),

                Category.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId.trim())
                        .name("Di chuyển")
                        .type("EXPENSE")
                        .icon("car")
                        .color("#4ECDC4")
                        .isArchived(false)
                        .usageCount(0)
                        .build(),

                Category.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId.trim())
                        .name("Giải trí")
                        .type("EXPENSE")
                        .icon("game")
                        .color("#1A535C")
                        .isArchived(false)
                        .usageCount(0)
                        .build(),

                Category.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId.trim())
                        .name("Lương")
                        .type("INCOME")
                        .icon("salary")
                        .color("#2ECC71")
                        .isArchived(false)
                        .usageCount(0)
                        .build(),

                Category.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId.trim())
                        .name("Thưởng")
                        .type("INCOME")
                        .icon("bonus")
                        .color("#27AE60")
                        .isArchived(false)
                        .usageCount(0)
                        .build()
        );

        categoryRepository.saveAll(defaultCategories);
    }

    @Override
    public BaseResponse<CategoryResponse> createCategory(CategoryCreationRequest request) {
        validateCreateRequest(request);

        CategoryType categoryType = parseCategoryType(request.getType());

        boolean existed = categoryRepository.existsByUserIdAndNameAndType(
                request.getUserId().trim(),
                request.getName().trim(),
                categoryType.name()
        );

        if (existed) {
            throw new AppException(ErrorCode.DATASOURCE_ALREADY_EXISTS);
        }

        Category category = Category.builder()
                .id(UUID.randomUUID().toString())
                .userId(request.getUserId().trim())
                .name(request.getName().trim())
                .type(categoryType.name())
                .icon(normalize(request.getIcon()))
                .color(normalize(request.getColor()))
                .isArchived(false)
                .usageCount(0)
                .build();

        categoryRepository.save(category);
        return BaseResponse.ok(mapToResponse(category));
    }

    @Override
    public BaseResponse<CategoryResponse> updateCategory(String id, CategoryUpdateRequest request) {
        if (id == null || id.isBlank() || request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Category category = categoryRepository.findById(id.trim())
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isEmpty()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            category.setName(name);
        }

        if (request.getType() != null) {
            CategoryType categoryType = parseCategoryType(request.getType());
            category.setType(categoryType.name());
        }

        if (request.getIcon() != null) {
            category.setIcon(normalize(request.getIcon()));
        }

        if (request.getColor() != null) {
            category.setColor(normalize(request.getColor()));
        }

        if (request.getIsArchived() != null) {
            category.setArchived(request.getIsArchived());
        }

        categoryRepository.save(category);
        return BaseResponse.ok(mapToResponse(category));
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<CategoryResponse> getCategoryById(String id) {
        if (id == null || id.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Category category = categoryRepository.findById(id.trim())
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

        return BaseResponse.ok(mapToResponse(category));
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<BasePaginationResponse<CategoryResponse>> getAllCategories(CategoryFilterRequest request) {
        Specification<Category> spec = request.specification();
        Pageable pageable = request.pageable();

        Page<CategoryResponse> pageResponse =
                categoryRepository.findAll(spec, pageable).map(this::mapToResponse);

        return BaseResponse.ok(BasePaginationResponse.of(pageResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<List<CategoryResponse>> getCategoriesByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        List<CategoryResponse> responses = categoryRepository.findByUserId(userId.trim())
                .stream()
                .map(this::mapToResponse)
                .toList();

        return BaseResponse.ok(responses);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<List<CategoryResponse>> getCategoriesByType(String userId, String type) {
        if (userId == null || userId.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        CategoryType categoryType = parseCategoryType(type);

        List<CategoryResponse> responses = categoryRepository.findByUserIdAndType(userId.trim(), categoryType.name())
                .stream()
                .map(this::mapToResponse)
                .toList();

        return BaseResponse.ok(responses);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<List<CategoryResponse>> getAvailableCategories(String userId, String type) {
        if (userId == null || userId.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        CategoryType categoryType = parseCategoryType(type);

        List<CategoryResponse> responses = categoryRepository
                .findByUserIdAndTypeAndIsArchivedFalseAndIsActiveTrue(userId.trim(), categoryType.name())
                .stream()
                .map(this::mapToResponse)
                .toList();

        return BaseResponse.ok(responses);
    }

    @Override
    public BaseResponse<String> archiveCategory(String id) {
        if (id == null || id.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Category category = categoryRepository.findById(id.trim())
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

        category.setArchived(true);
        categoryRepository.save(category);

        return BaseResponse.ok("Category archived successfully");
    }

    @Override
    public BaseResponse<String> unarchiveCategory(String id) {
        if (id == null || id.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Category category = categoryRepository.findById(id.trim())
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

        category.setArchived(false);
        categoryRepository.save(category);

        return BaseResponse.ok("Category unarchived successfully");
    }

    @Override
    public BaseResponse<String> softDeleteCategory(String id, String userId) {
        if (id == null || id.isBlank() || userId == null || userId.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Category category = categoryRepository.findById(id.trim())
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

        if (!category.getUserId().equals(userId.trim())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        category.setDeletedAt(Instant.now().toEpochMilli());
        category.deactivate();

        categoryRepository.save(category);

        return BaseResponse.ok("Category deleted successfully");
    }

    @Override
    public BaseResponse<String> increaseUsageCount(String id) {
        if (id == null || id.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Category category = categoryRepository.findById(id.trim())
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

        Integer currentUsage = category.getUsageCount() == null ? 0 : category.getUsageCount();
        category.setUsageCount(currentUsage + 1);

        categoryRepository.save(category);

        return BaseResponse.ok("Category usage count increased");
    }

    private void validateCreateRequest(CategoryCreationRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getType() == null || request.getType().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private CategoryType parseCategoryType(String type) {
        try {
            return CategoryType.valueOf(type.trim().toUpperCase());
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .userId(category.getUserId())
                .name(category.getName())
                .type(category.getType())
                .icon(category.getIcon())
                .color(category.getColor())
                .isArchived(category.isArchived())
                .usageCount(category.getUsageCount())
                .isActive(category.isActive())
                .deletedAt(category.getDeletedAt())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}