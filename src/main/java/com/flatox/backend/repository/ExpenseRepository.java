package com.flatox.backend.repository;

import com.flatox.backend.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByApartmentId(Long apartmentId);
    List<Expense> findByBudgetItemId(Long budgetItemId);
}
