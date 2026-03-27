package com.financeai.finance_management.specification;

import com.financeai.finance_management.entity.Budget;
import com.financeai.finance_management.enums.BudgetType;
import com.financeai.finance_management.enums.CategoryType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BudgetSpecification {
    private static final String FIELD_NAME = "name";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_CATEGORY_ID = "categoryId";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_START_DATE = "startDate";
    private static final String FIELD_IS_ACTIVE = "isActive";

    private final List<Specification<Budget>> specifications = new ArrayList<>();

    public static BudgetSpecification builder() {
        return new BudgetSpecification();
    }

    public BudgetSpecification withKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return this;

        specifications.add((root, query, cb) -> {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get(FIELD_NAME)), pattern);
        });

        return this;
    }

    public BudgetSpecification withUserId(String userId) {
        if (userId == null || userId.isBlank()) return this;

        specifications.add((root, query, cb) ->
                cb.equal(root.get(FIELD_USER_ID), userId.trim()));

        return this;
    }

    public BudgetSpecification withCategoryId(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) return this;

        specifications.add((root, query, cb) ->
                cb.equal(root.get(FIELD_CATEGORY_ID), categoryId.trim()));

        return this;
    }

    public BudgetSpecification withType(BudgetType type) {
        if (type == null) return this;

        specifications.add(
                (root, query, cb) ->
                        cb.equal(root.get(FIELD_TYPE), type.name()));

        return this;
    }

    public BudgetSpecification withMonthAndYear(Integer month, Integer year) {
        if (month == null || year == null) return this;

        specifications.add((root, query, cb) -> {
            Long start = buildStartDate(year, month);
            Long end = buildEndDate(year, month);

            return cb.between(root.get(FIELD_START_DATE), start, end);
        });

        return this;
    }

    public BudgetSpecification withActive(Boolean isActive) {
        if (isActive == null) return this;

        specifications.add((root, query, cb) ->
                cb.equal(root.get(FIELD_IS_ACTIVE), isActive));

        return this;
    }

    public Specification<Budget> build() {
        return (root, query, cb) -> {
            List<Predicate> predicates = specifications.stream()
                    .map(s -> s.toPredicate(root, query, cb))
                    .filter(Objects::nonNull)
                    .toList();

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    private Long buildStartDate(Integer year, Integer month) {
        return java.time.LocalDate.of(year, month, 1)
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }
    private Long buildEndDate(Integer year, Integer month) {
        java.time.LocalDate lastDay = java.time.LocalDate.of(year, month, 1)
                .withDayOfMonth(java.time.LocalDate.of(year, month, 1).lengthOfMonth());

        return lastDay.atTime(23, 59, 59)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }
}