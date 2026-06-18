package com.flatox.backend.repository;

import com.flatox.backend.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    List<Vendor> findByApartmentId(Long apartmentId);
}
