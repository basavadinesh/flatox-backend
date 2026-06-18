package com.flatox.backend.repository;

import com.flatox.backend.entity.AccountingLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AccountingLedgerRepository extends JpaRepository<AccountingLedger, Long> {
    List<AccountingLedger> findByApartmentId(Long apartmentId);
    Optional<AccountingLedger> findFirstByApartmentIdOrderByIdDesc(Long apartmentId);
}
