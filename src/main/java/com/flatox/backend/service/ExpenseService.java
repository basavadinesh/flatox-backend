package com.flatox.backend.service;

import com.flatox.backend.entity.*;
import com.flatox.backend.enums.LedgerEntryType;
import com.flatox.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final VendorRepository vendorRepository;
    private final ApartmentRepository apartmentRepository;
    private final UserRepository userRepository;
    private final LedgerService ledgerService;

    /**
     * Records an expense payout, logs it in the ledger, and charges it against the budget allocations.
     */
    @Transactional
    public Expense recordExpense(
            Long apartmentId,
            Long budgetItemId,
            Long vendorId,
            String category,
            BigDecimal amount,
            LocalDate expenseDate,
            String description,
            Long recordedByUserId,
            String invoiceUrl,
            String receiptUrl
    ) {
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));

        BudgetItem budgetItem = budgetItemId != null ? budgetItemRepository.findById(budgetItemId).orElse(null) : null;
        Vendor vendor = vendorId != null ? vendorRepository.findById(vendorId).orElse(null) : null;
        User recorder = recordedByUserId != null ? userRepository.findById(recordedByUserId).orElse(null) : null;

        Expense expense = Expense.builder()
                .apartment(apartment)
                .budgetItem(budgetItem)
                .vendor(vendor)
                .category(category)
                .amount(amount)
                .expenseDate(expenseDate)
                .description(description)
                .recordedBy(recorder)
                .invoiceUrl(invoiceUrl)
                .receiptUrl(receiptUrl)
                .createdAt(LocalDateTime.now())
                .build();

        Expense saved = expenseRepository.save(expense);

        // Update the actual spending in the linked Budget line item
        if (budgetItem != null) {
            BigDecimal actual = budgetItem.getActualSpending() != null ? budgetItem.getActualSpending() : BigDecimal.ZERO;
            budgetItem.setActualSpending(actual.add(amount));
            budgetItemRepository.save(budgetItem);
        }

        // Post to double-entry ledger: Debit Expense account, Credit Cash account
        ledgerService.postToLedger(
                apartment,
                null,
                saved,
                LedgerEntryType.DEBIT,
                category.toUpperCase(), // e.g., 'SECURITY', 'UTILITIES', 'REPAIRS'
                amount,
                "Expense payout to " + (vendor != null ? vendor.getCompanyName() : "vendor") + " for " + description
        );

        return saved;
    }
}
