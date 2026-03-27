package com.financeai.finance_management.dto.request;

import com.financeai.finance_management.entity.Budget;
import com.financeai.finance_management.enums.BudgetType;
import com.financeai.finance_management.specification.BudgetSpecification;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

@Getter
@Setter
public class BudgetFilterRequest extends FilterRequest<Budget> {
    private String search;
    private String userId;
    private String categoryId;
    private BudgetType type;
    private Integer month;
    private Integer year;
    private Boolean isActive;

    @Override
    public Specification<Budget> specification() {
        return BudgetSpecification.builder()
                .withKeyword(getSearch())
                .withUserId(getUserId())
                .withCategoryId(getCategoryId())
                .withType(getType())
                .withMonthAndYear(getMonth(), getYear())
                .withActive(getIsActive())
                .build();
    }
}