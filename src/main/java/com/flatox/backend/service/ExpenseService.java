package com.flatox.backend.service;

import com.flatox.backend.dto.request.ExpenseDraftRequest;
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
    private final AiCorrectionRepository aiCorrectionRepository;

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
                .status("CONFIRMED")
                .source("MANUAL")
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

    // ── AI Expense Assistant: Draft Lifecycle Methods ──

    /**
     * Confirms a DRAFT expense — posts it to the accounting ledger.
     */
    @Transactional
    public Expense confirmDraft(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + expenseId));

        if (!"DRAFT".equals(expense.getStatus())) {
            throw new RuntimeException("Only DRAFT expenses can be confirmed. Current status: " + expense.getStatus());
        }

        expense.setStatus("CONFIRMED");
        Expense saved = expenseRepository.save(expense);

        // Now post to the double-entry ledger
        ledgerService.postToLedger(
                expense.getApartment(),
                null,
                saved,
                LedgerEntryType.DEBIT,
                expense.getCategory().toUpperCase(),
                expense.getAmount(),
                "AI-scanned expense confirmed: " + expense.getDescription()
        );

        // Update budget actual spending if linked
        if (expense.getBudgetItem() != null) {
            BudgetItem budgetItem = expense.getBudgetItem();
            BigDecimal actual = budgetItem.getActualSpending() != null ? budgetItem.getActualSpending() : BigDecimal.ZERO;
            budgetItem.setActualSpending(actual.add(expense.getAmount()));
            budgetItemRepository.save(budgetItem);
        }

        return saved;
    }

    /**
     * Rejects a DRAFT expense — removes it from the drafts queue.
     */
    @Transactional
    public Expense rejectDraft(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + expenseId));

        if (!"DRAFT".equals(expense.getStatus())) {
            throw new RuntimeException("Only DRAFT expenses can be rejected. Current status: " + expense.getStatus());
        }

        expense.setStatus("REJECTED");
        return expenseRepository.save(expense);
    }

    /**
     * Updates a DRAFT expense with admin corrections.
     * Stores corrections in ai_corrections table for future learning.
     */
    @Transactional
    public Expense updateDraft(Long expenseId, ExpenseDraftRequest updates) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + expenseId));

        if (!"DRAFT".equals(expense.getStatus())) {
            throw new RuntimeException("Only DRAFT expenses can be updated. Current status: " + expense.getStatus());
        }

        // Track corrections for learning
        if (updates.getCategory() != null && !updates.getCategory().equals(expense.getCategory())) {
            trackCorrection(expense, "category", expense.getCategory(), updates.getCategory());
            expense.setCategory(updates.getCategory());
        }
        if (updates.getAmount() != null && updates.getAmount().compareTo(expense.getAmount()) != 0) {
            trackCorrection(expense, "amount", expense.getAmount().toString(), updates.getAmount().toString());
            expense.setAmount(updates.getAmount());
        }
        if (updates.getVendor() != null) {
            String oldDesc = expense.getDescription();
            expense.setDescription("AI-scanned: " + updates.getVendor() + " — " + expense.getCategory());
            trackCorrection(expense, "vendor", oldDesc, expense.getDescription());
        }
        if (updates.getInvoiceNumber() != null) {
            trackCorrection(expense, "invoiceNumber",
                    expense.getInvoiceNumber(), updates.getInvoiceNumber());
            expense.setInvoiceNumber(updates.getInvoiceNumber());
        }
        if (updates.getTaxAmount() != null) {
            trackCorrection(expense, "taxAmount",
                    expense.getTaxAmount() != null ? expense.getTaxAmount().toString() : null,
                    updates.getTaxAmount().toString());
            expense.setTaxAmount(updates.getTaxAmount());
        }
        if (updates.getBillDate() != null) {
            LocalDate newBillDate = LocalDate.parse(updates.getBillDate());
            expense.setBillDate(newBillDate);
            expense.setExpenseDate(newBillDate);
        }
        if (updates.getDueDate() != null) {
            expense.setDueDate(LocalDate.parse(updates.getDueDate()));
        }
        if (updates.getDescription() != null) {
            expense.setDescription(updates.getDescription());
        }
        if (updates.getVendorId() != null) {
            Vendor vendor = vendorRepository.findById(updates.getVendorId()).orElse(null);
            expense.setVendor(vendor);
        }
        if (updates.getBudgetItemId() != null) {
            BudgetItem budgetItem = budgetItemRepository.findById(updates.getBudgetItemId()).orElse(null);
            expense.setBudgetItem(budgetItem);
        }

        return expenseRepository.save(expense);
    }

    private void trackCorrection(Expense expense, String fieldName, String originalValue, String correctedValue) {
        AiCorrection correction = AiCorrection.builder()
                .apartment(expense.getApartment())
                .expense(expense)
                .fieldName(fieldName)
                .originalValue(originalValue)
                .correctedValue(correctedValue)
                .build();
        aiCorrectionRepository.save(correction);
    }
}

