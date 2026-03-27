package com.financeai.finance_management.dto.request;

import com.financeai.finance_management.entity.Category;
import com.financeai.finance_management.specification.CategorySpecification;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

@Getter
@Setter
public class CategoryFilterRequest extends FilterRequest<Category> {
    private String search;
    //TODO: mot co field gi can filter thi khai bao o day nha

    @Override
    public Specification<Category> specification() {
        return CategorySpecification.builder()
                .withKeyword(getSearch())
                .build();
    }
}
