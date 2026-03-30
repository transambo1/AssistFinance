package com.financeai.finance_management.specification;

import com.financeai.finance_management.entity.Budget;
import com.financeai.finance_management.enums.BudgetStatus;
import com.financeai.finance_management.enums.BudgetType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BudgetSpecification {

    private static final String FIELD_NAME = "name";
    private static final String FIELD_CATEGORY_ID = "categoryId";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_START_DATE = "startDate";
    private static final String FIELD_END_DATE = "endDate";
    private static final String FIELD_IS_ACTIVE = "isActive";
    private static final String FIELD_USER = "user";
    private static final String FIELD_ID = "id";

    private final List<Specification<Budget>> specifications = new ArrayList<>();

    private BudgetSpecification() {
    }

    public static BudgetSpecification builder() {
        return new BudgetSpecification();
    }

    public BudgetSpecification withKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return this;
        }

        specifications.add((root, query, cb) -> {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get(FIELD_NAME)), pattern);
        });

        return this;
    }

    public BudgetSpecification withUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return this;
        }

        specifications.add((root, query, cb) ->
                cb.equal(root.get(FIELD_USER).get(FIELD_ID), userId.trim()));

        return this;
    }

    public BudgetSpecification withCategoryId(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return this;
        }

        specifications.add((root, query, cb) ->
                cb.equal(root.get(FIELD_CATEGORY_ID), categoryId.trim()));

        return this;
    }

    public BudgetSpecification withType(BudgetType type) {
        if (type == null) {
            return this;
        }

        specifications.add((root, query, cb) ->
                cb.equal(root.get(FIELD_TYPE), type));

        return this;
    }

    public BudgetSpecification withStatus(BudgetStatus status) {
        if (status == null) {
            return this;
        }

        specifications.add((root, query, cb) ->
                cb.equal(root.get(FIELD_STATUS), status));

        return this;
    }

    public BudgetSpecification withActive(Boolean isActive) {
        if (isActive == null) {
            return this;
        }

        specifications.add((root, query, cb) ->
                cb.equal(root.get(FIELD_IS_ACTIVE), isActive));

        return this;
    }

    public BudgetSpecification withDateRange(Long startDate, Long endDate) {
        if (startDate == null && endDate == null) {
            return this;
        }

        if (startDate != null && endDate != null) {
            specifications.add((root, query, cb) ->
                    cb.and(
                            cb.lessThanOrEqualTo(root.get(FIELD_START_DATE), endDate),
                            cb.greaterThanOrEqualTo(root.get(FIELD_END_DATE), startDate)
                    ));
            return this;
        }

        if (startDate != null) {
            specifications.add((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get(FIELD_END_DATE), startDate));
        }

        if (endDate != null) {
            specifications.add((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get(FIELD_START_DATE), endDate));
        }

        return this;
    }

    public BudgetSpecification withMonthAndYear(Integer month, Integer year) {
        if (month == null || year == null) {
            return this;
        }

        if (month < 1 || month > 12) {
            return this;
        }

        if (year < 2000 || year > 3000) {
            return this;
        }

        Long monthStart = LocalDate.of(year, month, 1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        LocalDate firstDay = LocalDate.of(year, month, 1);
        Long monthEnd = firstDay.withDayOfMonth(firstDay.lengthOfMonth())
                .atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        specifications.add((root, query, cb) ->
                cb.and(
                        cb.lessThanOrEqualTo(root.get(FIELD_START_DATE), monthEnd),
                        cb.greaterThanOrEqualTo(root.get(FIELD_END_DATE), monthStart)
                ));

        return this;
    }

    public Specification<Budget> build() {
        return (root, query, cb) -> {
            List<Predicate> predicates = specifications.stream()
                    .map(specification -> specification.toPredicate(root, query, cb))
                    .filter(Objects::nonNull)
                    .toList();

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}