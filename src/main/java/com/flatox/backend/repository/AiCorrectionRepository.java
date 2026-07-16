package com.flatox.backend.repository;

import com.flatox.backend.entity.AiCorrection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiCorrectionRepository extends JpaRepository<AiCorrection, Long> {
    List<AiCorrection> findByExpenseId(Long expenseId);
    List<AiCorrection> findByApartmentId(Long apartmentId);
}
