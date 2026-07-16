package com.flatox.backend.repository;

import com.flatox.backend.entity.MaintenanceBill;
import com.flatox.backend.enums.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaintenanceBillRepository extends JpaRepository<MaintenanceBill, Long> {
    List<MaintenanceBill> findByApartmentId(Long apartmentId);
    List<MaintenanceBill> findByFlatId(Long flatId);
    List<MaintenanceBill> findByFlatIdAndStatusIn(Long flatId, List<BillStatus> statuses);
    List<MaintenanceBill> findByApartmentIdAndStatus(Long apartmentId, BillStatus status);
    List<MaintenanceBill> findByBillingTemplateId(Long templateId);
}
