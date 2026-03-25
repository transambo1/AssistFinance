package com.financeai.finance_management.specification;

import com.financeai.finance_management.entity.Category;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CategorySpecification {
    private static final String FIELD_NAME = "name";

    private final List<Specification<Category>> specifications = new ArrayList<>();

    public static CategorySpecification builder() {
        return new CategorySpecification();
    }

    public CategorySpecification withKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) return this;
        specifications.add(
                (root, query, cb) -> {
                    String pattern = "%" + keyword.toLowerCase() + "%";
                    return cb.or(cb.like(cb.lower(root.get(FIELD_NAME)), pattern));
                });
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
