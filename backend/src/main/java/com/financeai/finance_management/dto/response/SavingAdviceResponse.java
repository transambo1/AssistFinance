package com.financeai.finance_management.dto.response;

import java.util.List;
import lombok.Data;

@Data
public class SavingAdviceResponse {

  private String analysis;

  private List<String> tips;
}
