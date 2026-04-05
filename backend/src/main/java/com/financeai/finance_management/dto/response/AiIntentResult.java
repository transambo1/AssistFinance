package com.financeai.finance_management.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiIntentResult {
    private String intent;
    private String category;
    private String type;
    private String timeRange;
    private  String keyword;
}