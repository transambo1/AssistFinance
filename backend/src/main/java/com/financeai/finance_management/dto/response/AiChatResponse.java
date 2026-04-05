package com.financeai.finance_management.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiChatResponse {
    private String actionType; // PARSE_TRANSACTION | QUERY
    private String answer;
    private String intent;
    private Object data;
}