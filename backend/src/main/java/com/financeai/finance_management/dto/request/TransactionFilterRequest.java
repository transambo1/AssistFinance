package com.financeai.finance_management.dto.request;

import com.financeai.finance_management.entity.Category;
import com.financeai.finance_management.entity.Transaction;
import com.financeai.finance_management.specification.CategorySpecification;
import com.financeai.finance_management.specification.TransactionSpecification;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

@Getter
@Setter
public class TransactionFilterRequest extends FilterRequest<Transaction> {
    private String search;
    private String userId;
    private String categoryId;
    private String startDate;
    private String endDate;

    @Override
    public Specification<Transaction> specification() {
        return TransactionSpecification.builder()
                .withKeyword(getSearch())
                .withRegistrationDateRange(getStartDate(), getEndDate())
                .withUserId(getUserId())
                .withCategoryId(getCategoryId())
                .build();
    }
}
