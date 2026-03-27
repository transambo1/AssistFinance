package com.financeai.finance_management.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public abstract class FilterRequest<T> {

  public static final String DEFAULT_SORT = "createdAt";
  public static final String DEFAULT_SORT_NATIVE = "created_at";

  @Schema(defaultValue = "1")
  private int page = 1;

  @Schema(defaultValue = "11")
  private int size = 11;

  @Parameter(hidden = true)
  private Map<String, String> orders = new HashMap<>();

  public abstract Specification<T> specification();

  public Pageable pageable() {
    Sort sortable = sortable(orders);
    return PageRequest.of(page - 1, size, sortable);
  }

  public Pageable pageableWithoutSort() {
    return PageRequest.of(page - 1, size, Sort.unsorted());
  }

  public Sort sortable(Map<String, String> orders) {
    List<Sort.Order> sortableList = new ArrayList<>();
    orders.forEach(
        (key, value) -> {
          Sort.Direction direction =
              Sort.Direction.DESC.name().equals(value) ? Sort.Direction.DESC : Sort.Direction.ASC;
          Sort.Order order = new Sort.Order(direction, key);
          sortableList.add(order);
        });
    sortableList.add(new Sort.Order(Sort.Direction.DESC, DEFAULT_SORT));
    return Sort.by(sortableList);
  }
}
