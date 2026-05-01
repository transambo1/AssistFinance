package com.financeai.finance_management.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiChatIntentResult extends AiIntentResult {
    private String mode; // PARSE_TRANSACTION | QUERY
}