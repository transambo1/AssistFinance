package com.financeai.finance_management.dto.request;

import com.financeai.finance_management.entity.Category;
import com.financeai.finance_management.enums.CategoryType;
import com.financeai.finance_management.specification.CategorySpecification;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

@Getter
@Setter
public class CategoryFilterRequest extends FilterRequest<Category> {
    private String search;
    private String userId;
    private CategoryType type;
    private Boolean isArchived;
    private Boolean isActive;

    @Override
    public Specification<Category> specification() {
        return CategorySpecification.builder()
                .withKeyword(getSearch())
                .withUserId(getUserId())
                .withType(getType())
                .withArchived(getIsArchived())
                .withActive(getIsActive())
                .build();
    }
}