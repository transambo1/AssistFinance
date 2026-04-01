package com.financeai.finance_management.specification;

import com.financeai.finance_management.entity.Category;
import com.financeai.finance_management.entity.Transaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TransactionSpecification {
    private static final String FIELD_NAME = "name";
    private static final String FIELD_REGISTRATION_DATE = "createdAt";
    private static final String FIELD_USER = "user";
    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_ID = "id";

    private final List<Specification<Transaction>> specifications = new ArrayList<>();

    public static TransactionSpecification builder() {
        return new TransactionSpecification();
    }

    public TransactionSpecification withKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) return this;
        specifications.add(
                (root, query, cb) -> {
                    String pattern = "%" + keyword.toLowerCase() + "%";
                    return cb.or(cb.like(cb.lower(root.get(FIELD_NAME)), pattern));
                });
        return this;
    }

    public TransactionSpecification withRegistrationDateRange(String fromDate, String toDate) {
        boolean hasFrom = fromDate != null && !fromDate.isBlank();
        boolean hasTo = toDate != null && !toDate.isBlank();

        if (!hasFrom && !hasTo) return this;

        specifications.add(
                (root, query, cb) -> {
                    try {
                        Predicate predicate = cb.conjunction();

                        if (hasFrom) {
                            long start =
                                    LocalDate.parse(fromDate)
                                            .atStartOfDay(ZoneId.systemDefault())
                                            .toInstant()
                                            .toEpochMilli();
                            predicate =
                                    cb.and(
                                            predicate, cb.greaterThanOrEqualTo(root.get(FIELD_REGISTRATION_DATE), start));
                        }

                        if (hasTo) {
                            long end =
                                    LocalDate.parse(toDate)
                                            .atTime(LocalTime.MAX)
                                            .atZone(ZoneId.systemDefault())
                                            .toInstant()
                                            .toEpochMilli();
                            predicate =
                                    cb.and(predicate, cb.lessThanOrEqualTo(root.get(FIELD_REGISTRATION_DATE), end));
                        }

                        return predicate;
                    } catch (DateTimeParseException e) {
                        // log.error("Invalid date format", e);
                        return null;
                    }
                });
        return this;
    }

    public TransactionSpecification withUserId(String userId) {
        if (userId == null || userId.isEmpty()) return this;
        specifications.add((root, query, cb) -> cb.equal(root.get(FIELD_USER).get(FIELD_ID), userId));
        return this;
    }

    public TransactionSpecification withCategoryId(String categoryId) {
        if (categoryId == null || categoryId.isEmpty()) return this;
        specifications.add((root, query, cb) -> cb.equal(root.get(FIELD_CATEGORY).get(FIELD_ID), categoryId));
        return this;
    }

    public Specification<Transaction> build() {
        return (root, query, cb) -> {

            List<Predicate> predicates =
                    specifications.stream()
                            .map(s -> s.toPredicate(root, query, cb))
                            .filter(Objects::nonNull)
                            .toList();
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
