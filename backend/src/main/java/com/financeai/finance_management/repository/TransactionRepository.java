package com.financeai.finance_management.repository;

import com.financeai.finance_management.entity.Transaction;
import com.financeai.finance_management.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface TransactionRepository extends JpaRepository<Transaction, String>, JpaSpecificationExecutor<Transaction> {

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = :type
          AND (:startDate IS NULL OR t.createdAt >= :startDate)
          AND (:endDate IS NULL OR t.createdAt <= :endDate)
          AND t.deletedAt IS NULL
    """)
    BigDecimal sumByUserIdAndTypeAndDateRange(
            @Param("userId") String userId,
            @Param("type") TransactionType type,
            @Param("startDate") Long startDate,
            @Param("endDate") Long endDate
    );

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = :type
          AND t.category.id = :categoryId
          AND (:startDate IS NULL OR t.createdAt >= :startDate)
          AND (:endDate IS NULL OR t.createdAt <= :endDate)
          AND t.deletedAt IS NULL
    """)
    BigDecimal sumByUserIdAndTypeAndCategoryIdAndDateRange(
            @Param("userId") String userId,
            @Param("type") TransactionType type,
            @Param("categoryId") String categoryId,
            @Param("startDate") Long startDate,
            @Param("endDate") Long endDate
    );

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = :type
          AND (:keyword IS NULL OR :keyword = '' OR LOWER(t.note) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND t.deletedAt IS NULL
    """)
    BigDecimal sumByKeyword(
            @Param("userId") String userId,
            @Param("type") TransactionType type,
            @Param("keyword") String keyword
    );
}