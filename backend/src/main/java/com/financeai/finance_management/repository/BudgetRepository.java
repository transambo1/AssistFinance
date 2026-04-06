package com.financeai.finance_management.repository;

import com.financeai.finance_management.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, String>, JpaSpecificationExecutor<Budget> {

    boolean existsByUserIdAndNameAndDeletedAtIsNull(String userId, String name);

    Optional<Budget> findByIdAndDeletedAtIsNull(String id);
    @Query("""
        SELECT b
        FROM Budget b
        WHERE b.user.id = :userId
          AND (:categoryId IS NULL OR b.categoryId = :categoryId)
          AND (:time IS NULL OR (b.startDate <= :time AND b.endDate >= :time))
          AND b.deletedAt IS NULL
        ORDER BY b.createdAt DESC
    """)
    List<Budget> findBudgets(
            @Param("userId") String userId,
            @Param("categoryId") String categoryId,
            @Param("time") Long time
    );
}