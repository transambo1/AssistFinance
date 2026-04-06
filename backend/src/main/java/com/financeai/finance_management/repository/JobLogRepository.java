package com.financeai.finance_management.repository;

import com.financeai.finance_management.entity.SalaryConfig;
import com.financeai.finance_management.entity.SalaryJobLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JobLogRepository extends JpaRepository<SalaryJobLog, String>, JpaSpecificationExecutor<SalaryJobLog> {
}
