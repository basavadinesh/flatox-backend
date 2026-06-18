package com.flatox.backend.repository;

import com.flatox.backend.entity.Transaction;
import com.flatox.backend.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByApartmentId(Long apartmentId);
    List<Transaction> findByPayerId(Long payerId);
    Optional<Transaction> findByGatewayPaymentId(String gatewayPaymentId);
    List<Transaction> findByApartmentIdAndStatus(Long apartmentId, PaymentStatus status);
}
