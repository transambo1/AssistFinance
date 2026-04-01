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
import com.financeai.finance_management.enums.BudgetStatus;
import com.financeai.finance_management.enums.BudgetType;
import com.financeai.finance_management.enums.CategoryType;
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
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class BudgetServiceImpl implements IBudgetService {

    private static final Set<Integer> ALLOWED_DURATION_MONTHS = Set.of(1, 3, 12, 24);

    BudgetRepository budgetRepository;
    UserRepository userRepository;
    CategoryRepository categoryRepository;

    @Override
    public BaseResponse<BudgetResponse> createBudget(BudgetCreationRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        normalizeCreateDate(request);
        validateCreateRequest(request);

        String currentUserId = getCurrentUserId();
        User user = findUserById(currentUserId);

        BudgetType type = request.getType();
        String categoryId = normalize(request.getCategoryId());

        if (type == BudgetType.LIMIT) {
            validateLimitCategory(categoryId, currentUserId);
        } else if (type == BudgetType.SAVING) {
            if (categoryId != null) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        } else {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        boolean existed = budgetRepository.existsByUserIdAndNameAndDeletedAtIsNull(
                currentUserId,
                request.getName().trim()
        );

        if (existed) {
            throw new AppException(ErrorCode.DATASOURCE_ALREADY_EXISTS);
        }

        BigDecimal currentAmount = request.getCurrentAmount() == null
                ? BigDecimal.ZERO
                : request.getCurrentAmount();

        Budget budget = Budget.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .categoryId(categoryId)
                .name(request.getName().trim())
                .type(type)
                .targetAmount(request.getTargetAmount())
                .currentAmount(currentAmount)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(resolveStatus(
                        type,
                        currentAmount,
                        request.getTargetAmount(),
                        true
                ))
                .build();

        budget.setActive(true);

        System.out.println(">>> CREATE BUDGET API HIT");
        Budget savedBudget = budgetRepository.saveAndFlush(budget);
        System.out.println(">>> SAVED ID: " + savedBudget.getId());
        System.out.println(">>> COUNT AFTER SAVE: " + budgetRepository.count());

        return BaseResponse.ok(mapToResponse(savedBudget));
    }

    @Override
    public BaseResponse<BudgetResponse> updateBudget(String id, BudgetUpdateRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Budget budget = findOwnedBudget(id);

        BudgetType currentType = budget.getType();

        if (request.getType() != null && request.getType() != currentType) {
            throw new AppException(ErrorCode.CANNOT_CHANGE_BUDGET_TYPE);
        }
        if (request.getStartDate() != null && !request.getStartDate().equals(budget.getStartDate())) {
            throw new AppException(ErrorCode.CANNOT_CHANGE_START_DATE);
        }
        normalizeUpdateDate(request, budget);
        if (request.getEndDate() != null) budget.setEndDate(request.getEndDate());

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

        //LOGIC TUNG TYPE
        if (currentType == BudgetType.LIMIT) {
            if (request.getCategoryId() != null) {
                String categoryId = normalize(request.getCategoryId());
                validateLimitCategory(categoryId, budget.getUser().getId());
                budget.setCategoryId(categoryId);
            }

        }  else if (currentType == BudgetType.SAVING) {
                if (request.getCategoryId() != null && normalize(request.getCategoryId()) != null) {
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }
                budget.setCategoryId(null);
            }

        if (request.getIsActive() != null) {
            budget.setActive(request.getIsActive());
        }

        budget.setStatus(resolveStatus(
                budget.getType(),
                budget.getCurrentAmount(),
                budget.getTargetAmount(),
                budget.isActive()
        ));

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

        Specification<Budget> specification = request.specification();
        Pageable pageable = request.pageable();

        Page<BudgetResponse> pageResponse = budgetRepository
                .findAll(specification, pageable)
                .map(this::mapToResponse);

        return BaseResponse.ok(BasePaginationResponse.of(pageResponse));
    }

    @Override
    public BaseResponse<String> activateBudget(String id) {
        Budget budget = findOwnedBudget(id);
        budget.setActive(true);
        budget.setStatus(resolveStatus(
                budget.getType(),
                budget.getCurrentAmount(),
                budget.getTargetAmount(),
                true
        ));

        budgetRepository.save(budget);
        return BaseResponse.ok("Budget activated successfully");
    }

    @Override
    public BaseResponse<String> deactivateBudget(String id) {
        Budget budget = findOwnedBudget(id);
        budget.setActive(false);
        budget.setStatus(BudgetStatus.CANCELLED);

        budgetRepository.save(budget);
        return BaseResponse.ok("Budget deactivated successfully");
    }

    @Override
    public BaseResponse<String> softDeleteBudget(String id) {
        Budget budget = findOwnedBudget(id);
        budget.setDeletedAt(Instant.now().toEpochMilli());
        budget.setActive(false);
        budget.setStatus(BudgetStatus.CANCELLED);
        budget.deactivate();

        budgetRepository.save(budget);
        return BaseResponse.ok("Budget deleted successfully");
    }

    private void validateCreateRequest(BudgetCreationRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getType() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getType() != BudgetType.LIMIT && request.getType() != BudgetType.SAVING) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getTargetAmount() == null || request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getCurrentAmount() != null && request.getCurrentAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        validateDurationMonths(request.getDurationMonths());
        validateDateRange(request.getStartDate(), request.getEndDate());

        if (request.getType() == BudgetType.LIMIT) {
            String categoryId = normalize(request.getCategoryId());
            if (categoryId == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        }

        if (request.getType() == BudgetType.SAVING) {
            if (normalize(request.getCategoryId()) != null) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        }
    }

    private void validateLimitCategory(String categoryId, String currentUserId) {
        String normalizedCategoryId = requireValidId(categoryId);

        Category category = categoryRepository.findById(normalizedCategoryId)
                .orElseThrow(() -> new AppException(ErrorCode.DATASOURCE_NOT_FOUND));

        if (category.getUser() == null || category.getUser().getId() == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!category.getUser().getId().trim().equals(currentUserId.trim())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (category.getType() != CategoryType.EXPENSE) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateDateRange(Long startDate, Long endDate) {
        if (startDate == null || endDate == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (endDate < startDate) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        LocalDate start = toLocalDate(startDate);
        LocalDate end = toLocalDate(endDate);

        if (end.isBefore(start)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateDurationMonths(Integer durationMonths) {
        if (durationMonths == null) {
            return;
        }

        if (!ALLOWED_DURATION_MONTHS.contains(durationMonths)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void normalizeCreateDate(BudgetCreationRequest request) {
        validateDurationMonths(request.getDurationMonths());

        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        if (request.getStartDate() == null && request.getEndDate() == null) {
            LocalDate end = today.plusMonths(request.getDurationMonths() != null ? request.getDurationMonths() : 1)
                    .minusDays(1);

            request.setStartDate(toStartOfDayEpochMilli(today));
            request.setEndDate(toEndOfDayEpochMilli(end));
            return;
        }

        if (request.getStartDate() != null && request.getEndDate() == null) {
            LocalDate start = toLocalDate(request.getStartDate());
            LocalDate end = start.plusMonths(request.getDurationMonths() != null ? request.getDurationMonths() : 1)
                    .minusDays(1);

            request.setStartDate(toStartOfDayEpochMilli(start));
            request.setEndDate(toEndOfDayEpochMilli(end));
            return;
        }

        if (request.getStartDate() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        LocalDate start = toLocalDate(request.getStartDate());
        LocalDate end = toLocalDate(request.getEndDate());

        request.setStartDate(toStartOfDayEpochMilli(start));
        request.setEndDate(toEndOfDayEpochMilli(end));
    }

    private void normalizeUpdateDate(BudgetUpdateRequest request, Budget currentBudget) {
        validateDurationMonths(request.getDurationMonths());

        if (request.getStartDate() == null && request.getEndDate() == null && request.getDurationMonths() == null) {
            return;
        }

        LocalDate start = request.getStartDate() != null
                ? toLocalDate(request.getStartDate())
                : toLocalDate(currentBudget.getStartDate());

        if (request.getEndDate() != null) {
            LocalDate end = toLocalDate(request.getEndDate());
            request.setStartDate(toStartOfDayEpochMilli(start));
            request.setEndDate(toEndOfDayEpochMilli(end));
            return;
        }

        if (request.getDurationMonths() != null) {
            LocalDate end = start.plusMonths(request.getDurationMonths()).minusDays(1);
            request.setStartDate(toStartOfDayEpochMilli(start));
            request.setEndDate(toEndOfDayEpochMilli(end));
            return;
        }

        if (request.getStartDate() != null) {
            LocalDate currentEnd = toLocalDate(currentBudget.getEndDate());
            request.setStartDate(toStartOfDayEpochMilli(start));
            request.setEndDate(toEndOfDayEpochMilli(currentEnd));
        }
    }

    private Long toStartOfDayEpochMilli(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    private Long toEndOfDayEpochMilli(LocalDate date) {
        return date.atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    private LocalDate toLocalDate(Long epochMilli) {
        return Instant.ofEpochMilli(epochMilli)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
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

    private String requireValidId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return id.trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private BudgetStatus resolveStatus(
            BudgetType type,
            BigDecimal currentAmount,
            BigDecimal targetAmount,
            boolean isActive
    ) {
        if (!isActive) {
            return BudgetStatus.CANCELLED;
        }

        BigDecimal safeCurrent = currentAmount == null ? BigDecimal.ZERO : currentAmount;
        BigDecimal safeTarget = targetAmount == null ? BigDecimal.ZERO : targetAmount;

        if (safeTarget.compareTo(BigDecimal.ZERO) <= 0) {
            return BudgetStatus.ACTIVE;
        }

        if (type == BudgetType.LIMIT) {
            if (safeCurrent.compareTo(safeTarget) > 0) {
                return BudgetStatus.EXCEEDED;
            }
            if (safeCurrent.compareTo(safeTarget) == 0) {
                return BudgetStatus.COMPLETED;
            }
            return BudgetStatus.ACTIVE;
        }

        if (type == BudgetType.SAVING) {
            if (safeCurrent.compareTo(safeTarget) >= 0) {
                return BudgetStatus.COMPLETED;
            }
            return BudgetStatus.ACTIVE;
        }

        return BudgetStatus.ACTIVE;
    }

    private BudgetResponse mapToResponse(Budget budget) {
        return BudgetResponse.builder()
                .id(budget.getId())
                .userId(budget.getUser() != null ? budget.getUser().getId() : null)
                .categoryId(budget.getCategoryId())
                .name(budget.getName())
                .type(budget.getType())
                .targetAmount(budget.getTargetAmount())
                .currentAmount(budget.getCurrentAmount())
                .startDate(budget.getStartDate())
                .endDate(budget.getEndDate())
                .isActive(budget.isActive())
                .status(budget.getStatus())
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .deletedAt(budget.getDeletedAt())
                .build();
    }
}