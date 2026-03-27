package com.financeai.finance_management.repository;

import com.financeai.finance_management.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, String>, JpaSpecificationExecutor<Budget> {
    Optional<Budget> findByIdAndDeletedAtIsNull(String id);

    boolean existsByUserIdAndNameAndDeletedAtIsNull(String userId, String name);
}