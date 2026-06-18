package com.flatox.backend.repository;

import com.flatox.backend.entity.ReserveFund;
import com.flatox.backend.enums.ReserveFundType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReserveFundRepository extends JpaRepository<ReserveFund, Long> {
    List<ReserveFund> findByApartmentId(Long apartmentId);
    Optional<ReserveFund> findByApartmentIdAndFundType(Long apartmentId, ReserveFundType fundType);
}
