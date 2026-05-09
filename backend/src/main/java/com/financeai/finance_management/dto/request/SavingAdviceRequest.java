package com.financeai.finance_management.dto.request;

import java.util.Map;
import lombok.Data;

@Data
public class SavingAdviceRequest {

  private Map<String, Double> categories;
}
