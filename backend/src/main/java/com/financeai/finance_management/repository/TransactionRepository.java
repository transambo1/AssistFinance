package com.financeai.finance_management.repository;

import com.financeai.finance_management.entity.Category;
import com.financeai.finance_management.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository extends JpaRepository<Transaction, String>, JpaSpecificationExecutor<Transaction> {
}
