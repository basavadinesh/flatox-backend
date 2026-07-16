package com.flatox.backend.controller;

import com.flatox.backend.dto.request.ExpenseDraftRequest;
import com.flatox.backend.dto.response.AiExtractionResponse;
import com.flatox.backend.entity.Expense;
import com.flatox.backend.entity.VendorRule;
import com.flatox.backend.entity.Apartment;
import com.flatox.backend.entity.Vendor;
import com.flatox.backend.repository.ExpenseRepository;
import com.flatox.backend.repository.VendorRuleRepository;
import com.flatox.backend.service.AiExtractionService;
import com.flatox.backend.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/ai-expense")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AiExpenseController {

    private final AiExtractionService aiExtractionService;
    private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepository;
    private final VendorRuleRepository vendorRuleRepository;

    /**
     * Scans OCR text from a bill/invoice and creates an expense draft.
     *
     * POST /api/finance/ai-expense/{apartmentId}/scan
     * Body: { "ocrText": "...", "imageUrl": "..." }
     */
    @PostMapping("/{apartmentId}/scan")
    public ResponseEntity<AiExtractionResponse> scanDocument(
            @PathVariable Long apartmentId,
            @RequestBody Map<String, String> body
    ) {
        String ocrText = body.getOrDefault("ocrText", "");
        String base64Image = body.getOrDefault("base64Image", "");
        String userId = body.getOrDefault("userId", null);
        Long recordedByUserId = userId != null ? Long.parseLong(userId) : null;

        AiExtractionResponse response = aiExtractionService.processOcrText(
                apartmentId, ocrText, base64Image, recordedByUserId
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Lists all DRAFT expenses for an apartment.
     *
     * GET /api/finance/ai-expense/{apartmentId}/drafts
     */
    @GetMapping("/{apartmentId}/drafts")
    public List<Expense> getDrafts(@PathVariable Long apartmentId) {
        return expenseRepository.findByApartmentIdAndStatus(apartmentId, "DRAFT");
    }

    /**
     * Updates a draft expense with admin corrections.
     *
     * PUT /api/finance/ai-expense/drafts/{expenseId}
     */
    @PutMapping("/drafts/{expenseId}")
    public ResponseEntity<Expense> updateDraft(
            @PathVariable Long expenseId,
            @RequestBody ExpenseDraftRequest request
    ) {
        Expense updated = expenseService.updateDraft(expenseId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Confirms a draft expense — posts to accounting ledger.
     *
     * POST /api/finance/ai-expense/drafts/{expenseId}/confirm
     */
    @PostMapping("/drafts/{expenseId}/confirm")
    public ResponseEntity<Expense> confirmDraft(@PathVariable Long expenseId) {
        Expense confirmed = expenseService.confirmDraft(expenseId);
        return ResponseEntity.ok(confirmed);
    }

    /**
     * Rejects a draft expense.
     *
     * POST /api/finance/ai-expense/drafts/{expenseId}/reject
     */
    @PostMapping("/drafts/{expenseId}/reject")
    public ResponseEntity<Expense> rejectDraft(@PathVariable Long expenseId) {
        Expense rejected = expenseService.rejectDraft(expenseId);
        return ResponseEntity.ok(rejected);
    }

    /**
     * Lists vendor recognition rules for an apartment.
     *
     * GET /api/finance/ai-expense/{apartmentId}/vendor-rules
     */
    @GetMapping("/{apartmentId}/vendor-rules")
    public List<VendorRule> getVendorRules(@PathVariable Long apartmentId) {
        return vendorRuleRepository.findByApartmentId(apartmentId);
    }

    /**
     * Adds a custom vendor recognition rule.
     *
     * POST /api/finance/ai-expense/{apartmentId}/vendor-rules
     * Body: { "keyword": "...", "category": "...", "vendorId": ... }
     */
    @PostMapping("/{apartmentId}/vendor-rules")
    public ResponseEntity<VendorRule> addVendorRule(
            @PathVariable Long apartmentId,
            @RequestBody Map<String, Object> body
    ) {
        VendorRule rule = VendorRule.builder()
                .apartment(Apartment.builder().id(apartmentId).build())
                .keyword((String) body.get("keyword"))
                .category((String) body.get("category"))
                .confidence(body.containsKey("confidence")
                        ? Double.parseDouble(body.get("confidence").toString()) : 1.0)
                .build();

        if (body.containsKey("vendorId") && body.get("vendorId") != null) {
            rule.setVendor(Vendor.builder()
                    .id(Long.parseLong(body.get("vendorId").toString()))
                    .build());
        }

        return ResponseEntity.ok(vendorRuleRepository.save(rule));
    }
}
