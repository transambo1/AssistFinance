package com.financeai.finance_management.dto.request;

import com.financeai.finance_management.entity.Budget;
import com.financeai.finance_management.enums.BudgetStatus;
import com.financeai.finance_management.enums.BudgetType;
import com.financeai.finance_management.specification.BudgetSpecification;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@Getter
@Setter
public class BudgetFilterRequest {

    private String keyword;
    private String userId;
    private String categoryId;
    private BudgetType type;
    private BudgetStatus status;
    private Boolean isActive;

    // filter theo khoảng ngày bất kỳ
    private Long startDate;
    private Long endDate;

    // filter theo tháng
    private Integer month;
    private Integer year;

    private Integer page = 0;
    private Integer size = 10;

    public Specification<Budget> specification() {
        return BudgetSpecification.builder()
                .withKeyword(keyword)
                .withUserId(userId)
                .withCategoryId(categoryId)
                .withType(type)
                .withStatus(status)
                .withActive(isActive)
                .withDateRange(startDate, endDate)
                .withMonthAndYear(month, year)
                .build();
    }

    public Pageable pageable() {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 ? 10 : size;
        return PageRequest.of(safePage, safeSize);
    }
}