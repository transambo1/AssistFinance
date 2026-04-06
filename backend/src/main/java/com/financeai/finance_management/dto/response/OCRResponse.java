package com.financeai.finance_management.dto.response;

public class OCRResponse {

    private String text;
    private Long amount;
    private String date;

    public OCRResponse(String text, Long amount, String date) {
        this.text = text;
        this.amount = amount;

    }

    public String getText() {
        return text;
    }

    public Long getAmount() {
        return amount;
    }
    public String getDate() {
        return date;
    }
}
