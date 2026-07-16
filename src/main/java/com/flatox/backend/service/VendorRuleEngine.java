package com.flatox.backend.service;

import com.flatox.backend.entity.VendorRule;
import com.flatox.backend.repository.VendorRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Deterministic rule engine that attempts to classify invoices
 * by matching known vendor keywords against OCR text.
 *
 * If the rule engine can confidently classify the document,
 * we skip the LLM entirely — saving cost and latency.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VendorRuleEngine {

    private final VendorRuleRepository vendorRuleRepository;

    /**
     * Default vendor keyword → category mappings for common Indian vendors.
     * These are applied when no apartment-specific rules exist.
     */
    private static final Map<String, String> DEFAULT_RULES = new LinkedHashMap<>();

    static {
        // Electricity providers
        DEFAULT_RULES.put("APSPDCL", "ELECTRICITY");
        DEFAULT_RULES.put("APEPDCL", "ELECTRICITY");
        DEFAULT_RULES.put("TSSPDCL", "ELECTRICITY");
        DEFAULT_RULES.put("TSNPDCL", "ELECTRICITY");
        DEFAULT_RULES.put("BESCOM", "ELECTRICITY");
        DEFAULT_RULES.put("MSEDCL", "ELECTRICITY");
        DEFAULT_RULES.put("CESC", "ELECTRICITY");
        DEFAULT_RULES.put("TATA POWER", "ELECTRICITY");
        DEFAULT_RULES.put("ADANI ELECTRICITY", "ELECTRICITY");
        DEFAULT_RULES.put("ELECTRICITY BILL", "ELECTRICITY");

        // Water utilities
        DEFAULT_RULES.put("HMWSSB", "WATER");
        DEFAULT_RULES.put("WATER BOARD", "WATER");
        DEFAULT_RULES.put("WATER SUPPLY", "WATER");
        DEFAULT_RULES.put("BWSSB", "WATER");

        // Internet / Telecom
        DEFAULT_RULES.put("BSNL", "INTERNET");
        DEFAULT_RULES.put("AIRTEL", "INTERNET");
        DEFAULT_RULES.put("JIOFIBER", "INTERNET");
        DEFAULT_RULES.put("JIO FIBER", "INTERNET");
        DEFAULT_RULES.put("ACT FIBERNET", "INTERNET");
        DEFAULT_RULES.put("HATHWAY", "INTERNET");
        DEFAULT_RULES.put("TIKONA", "INTERNET");
        DEFAULT_RULES.put("YOU BROADBAND", "INTERNET");

        // Gas
        DEFAULT_RULES.put("BHAGYANAGAR GAS", "GAS");
        DEFAULT_RULES.put("MAHANAGAR GAS", "GAS");
        DEFAULT_RULES.put("INDRAPRASTHA GAS", "GAS");
        DEFAULT_RULES.put("IGL", "GAS");
        DEFAULT_RULES.put("PIPED GAS", "GAS");

        // Purchases
        DEFAULT_RULES.put("AMAZON", "OFFICE_PURCHASE");
        DEFAULT_RULES.put("FLIPKART", "OFFICE_PURCHASE");

        // Lift / Elevator
        DEFAULT_RULES.put("OTIS", "LIFT_MAINTENANCE");
        DEFAULT_RULES.put("KONE", "LIFT_MAINTENANCE");
        DEFAULT_RULES.put("SCHINDLER", "LIFT_MAINTENANCE");
        DEFAULT_RULES.put("THYSSENKRUPP", "LIFT_MAINTENANCE");
        DEFAULT_RULES.put("ELEVATOR", "LIFT_MAINTENANCE");
        DEFAULT_RULES.put("LIFT MAINTENANCE", "LIFT_MAINTENANCE");
    }

    /**
     * Attempts to match the OCR text against known vendor rules.
     *
     * @param ocrText       The raw OCR text from the scanned document
     * @param apartmentId   The apartment context for custom rules
     * @return              MatchResult with vendor, category, and confidence; or empty if no match
     */
    public Optional<MatchResult> match(String ocrText, Long apartmentId) {
        if (ocrText == null || ocrText.isBlank()) {
            return Optional.empty();
        }

        String normalizedText = ocrText.toUpperCase();

        // 1. Check apartment-specific custom rules first (higher priority)
        List<VendorRule> customRules = vendorRuleRepository.findByApartmentId(apartmentId);
        for (VendorRule rule : customRules) {
            if (normalizedText.contains(rule.getKeyword().toUpperCase())) {
                log.info("Rule engine matched custom rule: keyword='{}', category='{}'",
                        rule.getKeyword(), rule.getCategory());
                return Optional.of(MatchResult.builder()
                        .vendorName(rule.getKeyword())
                        .vendorId(rule.getVendor() != null ? rule.getVendor().getId() : null)
                        .category(rule.getCategory())
                        .confidence(rule.getConfidence() != null ? rule.getConfidence() : 0.95)
                        .matchedKeyword(rule.getKeyword())
                        .build());
            }
        }

        // 2. Fall back to default hardcoded rules
        for (Map.Entry<String, String> entry : DEFAULT_RULES.entrySet()) {
            if (normalizedText.contains(entry.getKey())) {
                log.info("Rule engine matched default rule: keyword='{}', category='{}'",
                        entry.getKey(), entry.getValue());
                return Optional.of(MatchResult.builder()
                        .vendorName(entry.getKey())
                        .vendorId(null)
                        .category(entry.getValue())
                        .confidence(0.90)
                        .matchedKeyword(entry.getKey())
                        .build());
            }
        }

        log.info("Rule engine found no match for OCR text (length={})", ocrText.length());
        return Optional.empty();
    }

    /**
     * Result of a vendor rule engine match.
     */
    @lombok.Builder
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class MatchResult {
        private String vendorName;
        private Long vendorId;
        private String category;
        private double confidence;
        private String matchedKeyword;
    }
}
