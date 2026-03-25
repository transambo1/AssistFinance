package com.financeai.finance_management.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BasePaginationResponse<T> {
  private Integer maxPage;
  private Integer nextPage;
  private Integer currentPage;
  private Integer previousPage;
  private Long total;
  private List<T> data;

  public static <T> BasePaginationResponse<T> of(Page<T> page) {
    return new BasePaginationResponse<T>(
        page.getTotalPages(),
        page.getNumber() + 2, // nextPage = currentPage + 1
        page.getNumber() + 1, // currentPage = 1-indexed
        page.getNumber(), // previousPage = currentPage - 1
        page.getTotalElements(),
        page.getContent());
  }

  public static <T> BasePaginationResponse<T> of(List<T> list) {
    return of(new PageImpl<>(list));
  }
}
