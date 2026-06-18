package com.flatox.backend.service;

import com.flatox.backend.entity.*;
import com.flatox.backend.enums.BillStatus;
import com.flatox.backend.enums.LedgerEntryType;
import com.flatox.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillingTemplateRepository templateRepository;
    private final MaintenanceBillRepository billRepository;
    private final FlatRepository flatRepository;
    private final ApartmentRepository apartmentRepository;
    private final LedgerService ledgerService;

    /**
     * Creates a recurring billing template for an apartment.
     */
    @Transactional
    public BillingTemplate createTemplate(Long apartmentId, BillingTemplate template) {
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));
        template.setApartment(apartment);
        return templateRepository.save(template);
    }

    /**
     * Batch generates bills for all flats inside an apartment.
     * Posts the total receivables to the ledger.
     */
    @Transactional
    public List<MaintenanceBill> batchGenerateBills(Long apartmentId, Long templateId, LocalDate dueDate, String title) {
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));
        
        BillingTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Billing template not found"));

        List<Flat> flats = flatRepository.findByApartmentId(apartmentId);
        List<MaintenanceBill> generatedBills = new ArrayList<>();

        for (Flat flat : flats) {
            MaintenanceBill bill = MaintenanceBill.builder()
                    .apartment(apartment)
                    .flat(flat)
                    .billingTemplate(template)
                    .title(title)
                    .baseAmount(template.getAmount())
                    .dueDate(dueDate)
                    .status(BillStatus.UNPAID)
                    .penaltyAmount(BigDecimal.ZERO)
                    .paidAmount(BigDecimal.ZERO)
                    .build();

            MaintenanceBill savedBill = billRepository.save(bill);
            generatedBills.add(savedBill);

            // Post to double-entry ledger: Debit Maintenance Dues Receivable
            ledgerService.postToLedger(
                    apartment,
                    null,
                    null,
                    LedgerEntryType.DEBIT,
                    "MAINTENANCE_RECEIVABLE",
                    template.getAmount(),
                    "Invoiced flat: " + flat.getFlatNumber() + " for " + title
            );
        }

        return generatedBills;
    }

    /**
     * Scans for overdue unpaid bills and applies the late penalty fees.
     */
    @Transactional
    public void applyOverduePenalties(Long apartmentId) {
        List<MaintenanceBill> unpaidBills = billRepository.findByApartmentIdAndStatus(apartmentId, BillStatus.UNPAID);
        LocalDate today = LocalDate.now();

        for (MaintenanceBill bill : unpaidBills) {
            if (today.isAfter(bill.getDueDate())) {
                BillingTemplate template = bill.getBillingTemplate();
                if (template != null && template.getLateFeePenalty() != null && template.getLateFeePenalty().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal penalty = template.getLateFeePenalty();
                    bill.setPenaltyAmount(bill.getPenaltyAmount().add(penalty));
                    bill.setStatus(BillStatus.OVERDUE);
                    billRepository.save(bill);

                    // Post penalty debit to ledger
                    ledgerService.postToLedger(
                            bill.getApartment(),
                            null,
                            null,
                            LedgerEntryType.DEBIT,
                            "PENALTY_RECEIVABLE",
                            penalty,
                            "Late penalty applied on overdue bill: " + bill.getTitle() + " for Flat " + bill.getFlat().getFlatNumber()
                    );
                }
            }
        }
    }
}
