package com.financeai.finance_management.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiChatIntentResult {
    private String mode;       // PARSE_TRANSACTION | QUERY
    private String intent;     // TOTAL_EXPENSE, BALANCE...
    private String category;   // ăn uống...
    private String type;       // EXPENSE | INCOME
    private String timeRange;  // THIS_MONTH, LAST_MONTH...
    private  String keyword;
}