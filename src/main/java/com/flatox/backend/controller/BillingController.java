package com.flatox.backend.controller;

import com.flatox.backend.dto.request.BatchBillGenerateRequest;
import com.flatox.backend.dto.request.BillingTemplateRequest;
import com.flatox.backend.entity.BillingTemplate;
import com.flatox.backend.entity.MaintenanceBill;
import com.flatox.backend.repository.BillingTemplateRepository;
import com.flatox.backend.repository.MaintenanceBillRepository;
import com.flatox.backend.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@CrossOrigin("*")
public class BillingController {

    private final BillingService billingService;
    private final BillingTemplateRepository templateRepository;
    private final MaintenanceBillRepository billRepository;

    @PostMapping("/billing-templates/apartment/{apartmentId}")
    public ResponseEntity<BillingTemplate> createTemplate(
            @PathVariable Long apartmentId,
            @RequestBody BillingTemplateRequest request
    ) {
        BillingTemplate template = BillingTemplate.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .frequency(request.getFrequency())
                .dueDayOfMonth(request.getDueDayOfMonth())
                .gracePeriodDays(request.getGracePeriodDays())
                .lateFeePenalty(request.getLateFeePenalty())
                .build();
        return ResponseEntity.ok(billingService.createTemplate(apartmentId, template));
    }

    @GetMapping("/billing-templates/apartment/{apartmentId}")
    public List<BillingTemplate> getTemplatesByApartment(@PathVariable Long apartmentId) {
        return templateRepository.findByApartmentId(apartmentId);
    }

    @PostMapping("/maintenance-bills/apartment/{apartmentId}/generate")
    public ResponseEntity<List<MaintenanceBill>> generateBills(
            @PathVariable Long apartmentId,
            @RequestBody BatchBillGenerateRequest request
    ) {
        List<MaintenanceBill> bills = billingService.batchGenerateBills(
                apartmentId,
                request.getTemplateId(),
                request.getDueDate(),
                request.getTitle()
        );
        return ResponseEntity.ok(bills);
    }

    @PostMapping("/maintenance-bills/apartment/{apartmentId}/apply-penalties")
    public ResponseEntity<String> applyPenalties(@PathVariable Long apartmentId) {
        billingService.applyOverduePenalties(apartmentId);
        return ResponseEntity.ok("Overdue penalties checked and applied successfully");
    }

    @GetMapping("/maintenance-bills/apartment/{apartmentId}")
    public List<MaintenanceBill> getBillsByApartment(@PathVariable Long apartmentId) {
        return billRepository.findByApartmentId(apartmentId);
    }

    @GetMapping("/maintenance-bills/flat/{flatId}")
    public List<MaintenanceBill> getBillsByFlat(@PathVariable Long flatId) {
        return billRepository.findByFlatId(flatId);
    }
}
