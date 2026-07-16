package com.flatox.backend.repository;

import com.flatox.backend.entity.VendorRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorRuleRepository extends JpaRepository<VendorRule, Long> {
    List<VendorRule> findByApartmentId(Long apartmentId);
}
