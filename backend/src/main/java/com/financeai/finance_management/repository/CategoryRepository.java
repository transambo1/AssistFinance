package com.financeai.finance_management.repository;

import com.financeai.finance_management.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String>, JpaSpecificationExecutor<Category> {

    List<Category> findByUserId(String userId);

    List<Category> findByUserIdAndType(String userId, String type);

    List<Category> findByUserIdAndTypeAndIsArchivedFalseAndIsActiveTrue(String userId, String type);

    boolean existsByUserIdAndNameAndType(String userId, String name, String type);
}