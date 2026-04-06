package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.request.SalaryConfigReq;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.entity.SalaryConfig;

public interface ISalaryConfigService {
    BaseResponse<Void> upsertConfig(SalaryConfigReq request);

    BaseResponse<Void> deleteConfig(String id);

    BaseResponse<Void> toggleActive(String id);

    void executeAutoJob(SalaryConfig config);
}
