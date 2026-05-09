package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.request.AnomalyRequest;
import com.financeai.finance_management.dto.response.AnomalyResponse;

public interface AnomalyService {

    AnomalyResponse detect(AnomalyRequest request);

}