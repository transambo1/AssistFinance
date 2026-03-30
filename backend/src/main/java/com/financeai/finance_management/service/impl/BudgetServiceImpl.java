package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.dto.request.BudgetCreationRequest;
import com.financeai.finance_management.dto.request.BudgetFilterRequest;
import com.financeai.finance_management.dto.request.BudgetUpdateRequest;
import com.financeai.finance_management.dto.response.BasePaginationResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.BudgetResponse;
import com.financeai.finance_management.entity.Budget;
import com.financeai.finance_management.entity.Category;
import com.financeai.finance_management.entity.User;
import com.financeai.finance_management.enums.BudgetType;
import com.financeai.finance_management.exception.exception.AppException;
import com.financeai.finance_management.exception.exception.ErrorCode;
import com.financeai.finance_management.repository.BudgetRepository;
import com.financeai.finance_management.repository.CategoryRepository;
import com.financeai.finance_management.repository.UserRepository;
import com.financeai.finance_management.service.IBudgetService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class BudgetServiceImpl implements IBudgetService {

    BudgetRepository budgetRepository;
    UserRepository userRepository;
    CategoryRepository categoryRepository;

    @Override
    public BaseResponse<BudgetResponse> createBudget(BudgetCreationRequest request) {
        validateCreateRequest(request);

        String currentUserId = getCurrentUserId();
        User user = findUserById(currentUserId);

        BudgetType budgetType = request.getType();
        String categoryId = normalize(request.getCategoryId());

        if (budgetType == BudgetType.LIMIT) {
            validateCategoryOwnership(categoryId, currentUserId);
        } else {
            categoryId = null;
        }

        boolean existed = budgetRepository.existsByUserIdAndNameAndDeletedAtIsNull(
                currentUserId,
                request.getName().trim()
        );

        if (existed) {
            throw new AppException(ErrorCode.DATASOURCE_ALREADY_EXISTS);
        }

        Budget budget = Budget.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .categoryId(categoryId)
                .name(request.getName().trim())
                .type(budgetType)
                .targetAmount(request.getTargetAmount())
                .currentAmount(request.getCurrentAmount() == null ? BigDecimal.ZERO : request.getCurrentAmount())
                .startDate(buildStartDate(request.getYear(), request.getMonth()))
                .endDate(buildEndDate(request.getYear(), request.getMonth()))
                .status("ACTIVE")
                .build();

        budgetRepository.save(budget);
        return BaseResponse.ok(mapToResponse(budget));
    }

    @Override
    public BaseResponse<BudgetResponse> updateBudget(String id, BudgetUpdateRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Budget budget = findOwnedBudget(id);

        BudgetType finalType = budget.getType();
        if (request.getType() != null) {
            finalType = request.getType();
            budget.setType(finalType);
        }

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isEmpty()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            budget.setName(name);
        }

        if (request.getTargetAmount() != null) {
            if (request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            budget.setTargetAmount(request.getTargetAmount());
        }

        if (request.getCurrentAmount() != null) {
            if (request.getCurrentAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            budget.setCurrentAmount(request.getCurrentAmount());
        }

        if (finalType == BudgetType.LIMIT) {
            String finalCategoryId = request.getCategoryId() != null
                    ? normalize(request.getCategoryId())
                    : budget.getCategoryId();

            validateCategoryOwnership(finalCategoryId, budget.getUser().getId());
            budget.setCategoryId(finalCategoryId);
        } else {
            budget.setCategoryId(null);
        }

        if (request.getMonth() != null || request.getYear() != null) {
            Integer month = request.getMonth();
            Integer year = request.getYear();

            if (month == null || year == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            validateMonthAndYear(month, year);
            budget.setStartDate(buildStartDate(year, month));
            budget.setEndDate(buildEndDate(year, month));
        }

        if (request.getIsActive() != null) {
            budget.setStatus(request.getIsActive() ? "ACTIVE" : "INACTIVE");
        }

        budgetRepository.save(budget);
        return BaseResponse.ok(mapToResponse(budget));
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<BudgetResponse> getBudgetById(String id) {
        Budget budget = findOwnedBudget(id);
        return BaseResponse.ok(mapToResponse(budget));
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<BasePaginationResponse<BudgetResponse>> getAllBudgets(BudgetFilterRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        String currentUserId = getCurrentUserId();
        request.setUserId(currentUserId);

        Specification<Budget> spec = request.specification();
        Pageable pageable = request.pageable();

        Page<BudgetResponse> pageResponse = budgetRepository
                .findAll(spec, pageable)
                .map(this::mapToResponse);

        return BaseResponse.ok(BasePaginationResponse.of(pageResponse));
    }

    @Override
    public BaseResponse<String> activateBudget(String id) {
        Budget budget = findOwnedBudget(id);
        budget.setStatus("ACTIVE");
        budgetRepository.save(budget);

        return BaseResponse.ok("Budget activated successfully");
    }

    @Override
    public BaseResponse<String> deactivateBudget(String id) {
        Budget budget = findOwnedBudget(id);
        budget.setStatus("INACTIVE");
        budgetRepository.save(budget);

        return BaseResponse.ok("Budget deactivated successfully");
    }

    @Override
    public BaseResponse<String> softDeleteBudget(String id) {
        Budget budget = findOwnedBudget(id);
        budget.setDeletedAt(Instant.now().toEpochMilli());
        budget.deactivate();
        budget.setStatus("INACTIVE");

        budgetRepository.save(budget);
        return BaseResponse.ok("Budget deleted successfully");
    }

    private void validateCreateRequest(BudgetCreationRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getType() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getTargetAmount() == null || request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getCurrentAmount() != null && request.getCurrentAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        validateMonthAndYear(request.getMonth(), request.getYear());

        if (request.getType() == BudgetType.LIMIT) {
            String categoryId = normalize(request.getCategoryId());
            if (categoryId == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        }
    }

    private void validateMonthAndYear(Integer month, Integer year) {
        if (month == null || year == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (month < 1 || month > 12) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (year < 2000 || year > 3000) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof String userId) {
            if (userId.isBlank()) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }
            return userId.trim();
        }

        if (principal instanceof Jwt jwt) {
            String userId = jwt.getSubject();
            if (userId == null || userId.isBlank()) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }
            return userId.trim();
        }

        String userId = authentication.getName();
        if (userId == null || userId.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return userId.trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requireValidId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return id.trim();
    }

    private User findUserById(String userId) {
        return userRepository.findById(requireValidId(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private Budget findBudgetById(String id) {
        return budgetRepository.findByIdAndDeletedAtIsNull(requireValidId(id))
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));
    }

    private Budget findOwnedBudget(String id) {
        Budget budget = findBudgetById(id);
        String currentUserId = getCurrentUserId();

        if (budget.getUser() == null || budget.getUser().getId() == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!currentUserId.equals(budget.getUser().getId().trim())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return budget;
    }

    private void validateCategoryOwnership(String categoryId, String currentUserId) {
        String normalizedCategoryId = requireValidId(categoryId);

        Category category = categoryRepository.findById(normalizedCategoryId)
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

        if (category.getUser() == null || category.getUser().getId() == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!category.getUser().getId().trim().equals(currentUserId.trim())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private Long buildStartDate(Integer year, Integer month) {
        return LocalDate.of(year, month, 1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    private Long buildEndDate(Integer year, Integer month) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

        return lastDay.atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    private BudgetResponse mapToResponse(Budget budget) {
        Integer month = null;
        Integer year = null;

        if (budget.getStartDate() != null) {
            LocalDate date = Instant.ofEpochMilli(budget.getStartDate())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            month = date.getMonthValue();
            year = date.getYear();
        }

        return BudgetResponse.builder()
                .id(budget.getId())
                .userId(budget.getUser() != null ? budget.getUser().getId() : null)
                .categoryId(budget.getCategoryId())
                .name(budget.getName())
                .type(budget.getType())
                .targetAmount(budget.getTargetAmount())
                .currentAmount(budget.getCurrentAmount())
                .month(month)
                .year(year)
                .isActive("ACTIVE".equalsIgnoreCase(budget.getStatus()))
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .deletedAt(budget.getDeletedAt())
                .build();
    }
}