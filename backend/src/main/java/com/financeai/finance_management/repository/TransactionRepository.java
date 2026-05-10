package com.financeai.finance_management.repository;

import com.financeai.finance_management.dto.response.CategorySumProjection;
import com.financeai.finance_management.entity.Transaction;
import com.financeai.finance_management.enums.TransactionType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository
    extends JpaRepository<Transaction, String>, JpaSpecificationExecutor<Transaction> {

  @Query(
      """
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
      @Param("endDate") Long endDate);

  @Query(
      """
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
      @Param("endDate") Long endDate);

  @Query(
      """
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
      @Param("keyword") String keyword);

  @Query(
      """
        SELECT t
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.deletedAt IS NULL
        ORDER BY t.createdAt DESC
    """)
  List<Transaction> findRecentTransactions(@Param("userId") String userId);

  @Query(
      """
        SELECT t
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = :type
          AND t.deletedAt IS NULL
        ORDER BY t.createdAt DESC
    """)
  List<Transaction> findRecentTransactionsByType(
      @Param("userId") String userId, @Param("type") TransactionType type);

  @Query(
      """
            SELECT t
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.deletedAt IS NULL
        """)
  List<Transaction> findByUserId(@Param("userId") String userId);

  List<Transaction> findByUserIdAndTypeAndTransactionDateGreaterThanEqual(
      String userId, TransactionType transactionType, long startTimestamp);

  List<Transaction> findByUserIdAndTypeAndTransactionDateBetween(
      String userId, TransactionType transactionType, long startTimestamp, long endTimestamp);

  @Query(
      """
        SELECT t.category.name, SUM(t.amount)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = com.financeai.finance_management.enums.TransactionType.EXPENSE
          AND t.deletedAt IS NULL
        GROUP BY t.category.name
    """)
  List<Object[]> getExpenseByCategory(@Param("userId") String userId);

  @Query(
      """
        SELECT t
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.isAnomaly = true
          AND t.deletedAt IS NULL
          AND FUNCTION('DATE', FUNCTION('FROM_UNIXTIME', t.transactionDate / 1000)) = CURRENT_DATE
    """)
  List<Transaction> findTodayAnomalies(@Param("userId") String userId);

  @Query(
      """
        SELECT CAST(t.amount as double)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.category.id = :categoryId
          AND t.type = com.financeai.finance_management.enums.TransactionType.EXPENSE
          AND t.deletedAt IS NULL
          AND t.amount > 0
    """)
  List<Double> findAmountsByUserAndCategory(
      @Param("userId") String userId, @Param("categoryId") String categoryId);

  @Query(
      """
        SELECT COUNT(t)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.isAnomaly = true
          AND t.deletedAt IS NULL
          AND FUNCTION('DATE', FUNCTION('FROM_UNIXTIME', t.transactionDate / 1000)) = CURRENT_DATE
    """)
  long countTodayAnomalies(@Param("userId") String userId);

  @Query(
      """
        SELECT t
        FROM Transaction t
        WHERE t.id = :transactionId
          AND t.user.id = :userId
          AND t.isAnomaly = true
          AND t.deletedAt IS NULL
    """)
  Optional<Transaction> findAnomalyDetailById(
      @Param("userId") String userId, @Param("transactionId") String transactionId);

  @Query(
      "SELECT t.category.name as name, SUM(t.amount) as amount, t.category.color as color "
          + "FROM Transaction t "
          + "WHERE t.user.id = :userId AND t.type = 'EXPENSE' "
          + "AND t.transactionDate BETWEEN :start AND :endDate "
          + "GROUP BY t.category.name, t.category.color")
  List<CategorySumProjection> sumAmountByCategory(String userId, Long start, Long endDate);
}
