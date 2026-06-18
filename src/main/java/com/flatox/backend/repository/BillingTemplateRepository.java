package com.flatox.backend.repository;

import com.flatox.backend.entity.BillingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BillingTemplateRepository extends JpaRepository<BillingTemplate, Long> {
    List<BillingTemplate> findByApartmentId(Long apartmentId);
    List<BillingTemplate> findByApartmentIdAndIsActiveTrue(Long apartmentId);
}
