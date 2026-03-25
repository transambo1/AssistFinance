package com.financeai.finance_management.dto.response;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
public class ListResponse<T> {
    private List<T> items;
    private int page;
    private int size;
    private long total;
    private int totalPages;

    public static <T> ListResponse<T> of(Page<T> pageData) {
        return ListResponse.<T>builder()
                .items(pageData.getContent())
                .page(pageData.getNumber() + 1)
                .size(pageData.getSize())
                .total(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .build();
    }
}