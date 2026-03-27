package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.dto.request.CategoryCreationRequest;
import com.financeai.finance_management.dto.request.CategoryFilterRequest;
import com.financeai.finance_management.dto.request.CategoryUpdateRequest;
import com.financeai.finance_management.dto.response.BasePaginationResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.CategoryResponse;
import com.financeai.finance_management.entity.Category;
import com.financeai.finance_management.entity.User;
import com.financeai.finance_management.exception.exception.AppException;
import com.financeai.finance_management.exception.exception.ErrorCode;
import com.financeai.finance_management.repository.CategoryRepository;
import com.financeai.finance_management.repository.UserRepository;
import com.financeai.finance_management.service.ICategoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    UserRepository userRepository;

    @Override
    public void createDefaultCategories(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        User user = userRepository.findById(userId.trim())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!categoryRepository.findByUserId(userId.trim()).isEmpty()) {
            return;
        }

        List<Category> defaultCategories = List.of(
                Category.builder()
                        .id(UUID.randomUUID().toString())
                        .user(user)
                        .name("Ăn uống")
                        .type(com.financeai.finance_management.enums.CategoryType.EXPENSE)
                        .icon("food")
                        .color("#FF6B6B")
                        .isArchived(false)
                        .usageCount(0)
                        .build(),

                Category.builder()
                        .id(UUID.randomUUID().toString())
                        .user(user)
                        .name("Di chuyển")
                        .type(com.financeai.finance_management.enums.CategoryType.EXPENSE)
                        .icon("car")
                        .color("#4ECDC4")
                        .isArchived(false)
                        .usageCount(0)
                        .build(),

                Category.builder()
                        .id(UUID.randomUUID().toString())
                        .user(user)
                        .name("Giải trí")
                        .type(com.financeai.finance_management.enums.CategoryType.EXPENSE)
                        .icon("game")
                        .color("#1A535C")
                        .isArchived(false)
                        .usageCount(0)
                        .build(),

                Category.builder()
                        .id(UUID.randomUUID().toString())
                        .user(user)
                        .name("Lương")
                        .type(com.financeai.finance_management.enums.CategoryType.INCOME)
                        .icon("salary")
                        .color("#2ECC71")
                        .isArchived(false)
                        .usageCount(0)
                        .build(),

                Category.builder()
                        .id(UUID.randomUUID().toString())
                        .user(user)
                        .name("Thưởng")
                        .type(com.financeai.finance_management.enums.CategoryType.INCOME)
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

        String currentUserId = getCurrentUserId().trim();

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean existed = categoryRepository.existsByUserIdAndNameAndType(
                currentUserId,
                request.getName().trim(),
                request.getType()
        );

        if (existed) {
            throw new AppException(ErrorCode.DATASOURCE_ALREADY_EXISTS);
        }

        Category category = Category.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .name(request.getName().trim())
                .type(request.getType())
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

        Category category = getCategoryOrThrow(id);
        checkCategoryOwnership(category);

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isEmpty()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            category.setName(name);
        }

        if (request.getType() != null) {
            category.setType(request.getType());
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
        Category category = getCategoryOrThrow(id);
        checkCategoryOwnership(category);

        return BaseResponse.ok(mapToResponse(category));
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<BasePaginationResponse<CategoryResponse>> getAllCategories(CategoryFilterRequest request) {
        String currentUserId = getCurrentUserId();
        request.setUserId(currentUserId);

        Specification<Category> spec = request.specification();
        Pageable pageable = request.pageable();

        Page<CategoryResponse> pageResponse =
                categoryRepository.findAll(spec, pageable).map(this::mapToResponse);

        return BaseResponse.ok(BasePaginationResponse.of(pageResponse));
    }

    @Override
    public BaseResponse<String> archiveCategory(String id) {
        Category category = getCategoryOrThrow(id);
        checkCategoryOwnership(category);

        category.setArchived(true);
        categoryRepository.save(category);

        return BaseResponse.ok("Category archived successfully");
    }

    @Override
    public BaseResponse<String> unarchiveCategory(String id) {
        Category category = getCategoryOrThrow(id);
        checkCategoryOwnership(category);

        category.setArchived(false);
        categoryRepository.save(category);

        return BaseResponse.ok("Category unarchived successfully");
    }

    @Override
    public BaseResponse<String> softDeleteCategory(String id) {
        Category category = getCategoryOrThrow(id);
        checkCategoryOwnership(category);

        category.setDeletedAt(Instant.now().toEpochMilli());
        category.deactivate();

        categoryRepository.save(category);

        return BaseResponse.ok("Category deleted successfully");
    }

    @Override
    public BaseResponse<String> increaseUsageCount(String id) {
        Category category = getCategoryOrThrow(id);
        checkCategoryOwnership(category);

        Integer currentUsage = category.getUsageCount() == null ? 0 : category.getUsageCount();
        category.setUsageCount(currentUsage + 1);

        categoryRepository.save(category);

        return BaseResponse.ok("Category usage count increased");
    }

    private void validateCreateRequest(CategoryCreationRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getType() == null) {
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

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return authentication.getName();
    }

    private Category getCategoryOrThrow(String id) {
        if (id == null || id.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        return categoryRepository.findById(id.trim())
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));
    }

    private void checkCategoryOwnership(Category category) {
        String currentUserId = getCurrentUserId().trim();
        if (!category.getUser().getId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .userId(category.getUser().getId())
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