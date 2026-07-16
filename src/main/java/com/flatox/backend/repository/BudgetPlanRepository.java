package com.flatox.backend.repository;

import com.flatox.backend.entity.BudgetPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BudgetPlanRepository extends JpaRepository<BudgetPlan, Long> {
    List<BudgetPlan> findByApartmentIdOrderByIdDesc(Long apartmentId);
}
