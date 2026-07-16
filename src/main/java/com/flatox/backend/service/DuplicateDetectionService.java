package com.flatox.backend.service;

import com.flatox.backend.entity.Expense;
import com.flatox.backend.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Detects duplicate invoices before creating expense drafts.
 * Checks by invoice number and by vendor+amount+date combination.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DuplicateDetectionService {

    private final ExpenseRepository expenseRepository;

    /**
     * Checks if an invoice already exists for this apartment.
     *
     * @return DuplicateResult with the existing expense ID if found
     */
    public DuplicateResult checkDuplicate(
            Long apartmentId,
            String invoiceNumber,
            Long vendorId,
            BigDecimal amount,
            LocalDate billDate
    ) {
        // 1. Check by invoice number (strongest signal)
        if (invoiceNumber != null && !invoiceNumber.isBlank()) {
            Optional<Expense> byInvoice = expenseRepository
                    .findFirstByApartmentIdAndInvoiceNumber(apartmentId, invoiceNumber);
            if (byInvoice.isPresent()) {
                Expense existing = byInvoice.get();
                log.warn("Duplicate detected by invoice number '{}': existing expense ID={}",
                        invoiceNumber, existing.getId());
                return new DuplicateResult(true, existing.getId(),
                        "Invoice number " + invoiceNumber + " already exists.");
            }
        }

        // 2. Check by vendor + amount + bill date (weaker signal but useful)
        if (vendorId != null && amount != null && billDate != null) {
            List<Expense> matches = expenseRepository
                    .findByApartmentIdAndVendorIdAndAmountAndBillDate(
                            apartmentId, vendorId, amount, billDate);
            if (!matches.isEmpty()) {
                Expense existing = matches.get(0);
                log.warn("Possible duplicate: vendor={}, amount={}, date={}, existing ID={}",
                        vendorId, amount, billDate, existing.getId());
                return new DuplicateResult(true, existing.getId(),
                        "A similar expense for the same vendor, amount, and date already exists.");
            }
        }

        return new DuplicateResult(false, null, null);
    }

    /**
     * Result of a duplicate detection check.
     */
    public record DuplicateResult(
            boolean isDuplicate,
            Long existingExpenseId,
            String message
    ) {}
}
