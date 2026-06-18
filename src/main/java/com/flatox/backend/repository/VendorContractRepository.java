package com.flatox.backend.repository;

import com.flatox.backend.entity.VendorContract;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VendorContractRepository extends JpaRepository<VendorContract, Long> {
    List<VendorContract> findByVendorId(Long vendorId);
}
