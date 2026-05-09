package com.financeai.finance_management.dto.request;

import java.util.List;
import lombok.Data;

@Data
public class SpendingTrendRequest {

  private List<Integer> expenses;
}
