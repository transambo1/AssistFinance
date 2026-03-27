package com.financeai.finance_management.specification;

import com.financeai.finance_management.entity.Category;
import com.financeai.finance_management.enums.CategoryType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CategorySpecification {
    private static final String FIELD_NAME = "name";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_IS_ARCHIVED = "isArchived";
    private static final String FIELD_IS_ACTIVE = "isActive";

    private final List<Specification<Category>> specifications = new ArrayList<>();

    public static CategorySpecification builder() {
        return new CategorySpecification();
    }

    public CategorySpecification withKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return this;

        specifications.add(
                (root, query, cb) -> {
                    String pattern = "%" + keyword.trim().toLowerCase() + "%";
                    return cb.like(cb.lower(root.get(FIELD_NAME)), pattern);
                });

        return this;
    }

    public CategorySpecification withUserId(String userId) {
        if (userId == null || userId.isBlank()) return this;

        specifications.add(
                (root, query, cb) ->
                        cb.equal(root.get(FIELD_USER_ID), userId.trim()));

        return this;
    }

    public CategorySpecification withType(CategoryType type) {
        if (type == null) return this;

        specifications.add(
                (root, query, cb) ->
                        cb.equal(root.get(FIELD_TYPE), type.name()));

        return this;
    }

    public CategorySpecification withArchived(Boolean isArchived) {
        if (isArchived == null) return this;

        specifications.add(
                (root, query, cb) ->
                        cb.equal(root.get(FIELD_IS_ARCHIVED), isArchived));

        return this;
    }

    public CategorySpecification withActive(Boolean isActive) {
        if (isActive == null) return this;

        specifications.add(
                (root, query, cb) ->
                        cb.equal(root.get(FIELD_IS_ACTIVE), isActive));

        return this;
    }

    public Specification<Category> build() {
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