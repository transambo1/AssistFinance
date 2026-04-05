package com.financeai.finance_management.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiQueryResponse {
    private String answer;
    private String intent;
    private Object data;
}