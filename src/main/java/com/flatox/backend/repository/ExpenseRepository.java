package com.flatox.backend.repository;

import com.flatox.backend.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByApartmentId(Long apartmentId);
    List<Expense> findByBudgetItemId(Long budgetItemId);

    // AI Expense Assistant queries
    List<Expense> findByApartmentIdAndStatus(Long apartmentId, String status);

    Optional<Expense> findFirstByApartmentIdAndInvoiceNumber(Long apartmentId, String invoiceNumber);

    List<Expense> findByApartmentIdAndVendorIdAndAmountAndBillDate(
            Long apartmentId, Long vendorId, BigDecimal amount, LocalDate billDate);
}

