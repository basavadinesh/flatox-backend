package com.flatox.backend.repository;

import com.flatox.backend.entity.BudgetItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BudgetItemRepository extends JpaRepository<BudgetItem, Long> {
    List<BudgetItem> findByBudgetPlanId(Long budgetPlanId);
}
