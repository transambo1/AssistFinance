package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.dto.request.CategoryCreationRequest;
import com.financeai.finance_management.dto.request.CategoryFilterRequest;
import com.financeai.finance_management.dto.request.CategoryUpdateRequest;
import com.financeai.finance_management.dto.response.BasePaginationResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.CategoryResponse;
import com.financeai.finance_management.entity.Category;
import com.financeai.finance_management.entity.User;
import com.financeai.finance_management.enums.CategoryType;
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
        String normalizedUserId = requireValidId(userId);

        User user = findUserById(normalizedUserId);

        if (!categoryRepository.findByUserId(normalizedUserId).isEmpty()) {
            return;
        }

        List<Category> defaultCategories = List.of(
                buildDefaultCategory(user, "Ăn uống", CategoryType.EXPENSE, "food", "#FF6B6B"),
                buildDefaultCategory(user, "Di chuyển", CategoryType.EXPENSE, "car", "#4ECDC4"),
                buildDefaultCategory(user, "Giải trí", CategoryType.EXPENSE, "game", "#1A535C"),
                buildDefaultCategory(user, "Lương", CategoryType.INCOME, "salary", "#2ECC71"),
                buildDefaultCategory(user, "Thưởng", CategoryType.INCOME, "bonus", "#27AE60")
        );

        categoryRepository.saveAll(defaultCategories);
    }

    @Override
    public BaseResponse<CategoryResponse> createCategory(CategoryCreationRequest request) {
        validateCreateRequest(request);

        String currentUserId = getCurrentUserId();
        User user = findUserById(currentUserId);

        String name = request.getName().trim();

        boolean existed = categoryRepository.existsByUserIdAndNameAndType(
                currentUserId,
                name,
                request.getType()
        );

        if (existed) {
            throw new AppException(ErrorCode.DATASOURCE_ALREADY_EXISTS);
        }

        Category category = Category.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .name(name)
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
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Category category = findOwnedCategory(id);

        if (request.getName() != null) {
            String newName = request.getName().trim();
            if (newName.isEmpty()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            category.setName(newName);
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
        Category category = findOwnedCategory(id);
        return BaseResponse.ok(mapToResponse(category));
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<BasePaginationResponse<CategoryResponse>> getAllCategories(CategoryFilterRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        String currentUserId = getCurrentUserId();
        request.setUserId(currentUserId);

        Specification<Category> specification = request.specification();
        Pageable pageable = request.pageable();

        Page<CategoryResponse> pageResponse = categoryRepository
                .findAll(specification, pageable)
                .map(this::mapToResponse);

        return BaseResponse.ok(BasePaginationResponse.of(pageResponse));
    }

    @Override
    public BaseResponse<String> archiveCategory(String id) {
        Category category = findOwnedCategory(id);
        category.setArchived(true);
        categoryRepository.save(category);

        return BaseResponse.ok("Category archived successfully");
    }

    @Override
    public BaseResponse<String> unarchiveCategory(String id) {
        Category category = findOwnedCategory(id);
        category.setArchived(false);
        categoryRepository.save(category);

        return BaseResponse.ok("Category unarchived successfully");
    }

    @Override
    public BaseResponse<String> softDeleteCategory(String id) {
        Category category = findOwnedCategory(id);
        category.setDeletedAt(Instant.now().toEpochMilli());
        category.deactivate();
        categoryRepository.save(category);

        return BaseResponse.ok("Category deleted successfully");
    }

    @Override
    public BaseResponse<String> increaseUsageCount(String id) {
        Category category = findOwnedCategory(id);

        int currentUsageCount = category.getUsageCount() == null ? 0 : category.getUsageCount();
        category.setUsageCount(currentUsageCount + 1);

        categoryRepository.save(category);

        return BaseResponse.ok("Category usage count increased");
    }

    private Category buildDefaultCategory(User user, String name, CategoryType type, String icon, String color) {
        return Category.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .name(name)
                .type(type)
                .icon(icon)
                .color(color)
                .isArchived(false)
                .usageCount(0)
                .build();
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

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String userId = authentication.getName();

        if (userId == null || userId.trim().isEmpty()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return userId.trim();
    }

    private String requireValidId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return id.trim();
    }

    private User findUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private Category findCategoryById(String id) {
        String categoryId = requireValidId(id);

        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));
    }

    private Category findOwnedCategory(String categoryId) {
        Category category = findCategoryById(categoryId);
        String currentUserId = getCurrentUserId();

        if (category.getUser() == null || category.getUser().getId() == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!currentUserId.equals(category.getUser().getId().trim())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return category;
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