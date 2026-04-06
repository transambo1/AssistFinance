package com.financeai.finance_management.repository;

import com.financeai.finance_management.entity.SalaryConfig;
import com.financeai.finance_management.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SalaryConfigRepository extends JpaRepository<SalaryConfig, String>, JpaSpecificationExecutor<SalaryConfig> {
    @Query("""
        SELECT s FROM SalaryConfig s 
        JOIN FETCH s.user 
        JOIN FETCH s.category
        WHERE s.deletedAt IS NULL 
        AND s.isActive = true
        AND (
            (s.frequency = 'MONTHLY' AND s.payDay = :dayOfMonth) 
            OR 
            (s.frequency = 'WEEKLY' AND s.payDay = :dayOfWeek)
        )
    """)
    List<SalaryConfig> findConfigsToExecute(
            @Param("dayOfMonth") int dayOfMonth,
            @Param("dayOfWeek") int dayOfWeek
    );

    @Query("""
        SELECT s FROM SalaryConfig s 
        JOIN FETCH s.user 
        JOIN FETCH s.category
        WHERE s.deletedAt IS NULL 
        AND s.isActive = true
        AND s.frequency = 'MONTHLY' 
        AND s.payDay > :lastDayOfMonth
    """)
    List<SalaryConfig> findOverflowMonthlyConfigs(@Param("lastDayOfMonth") int lastDayOfMonth);
}

