package com.flatox.backend.service;

import com.flatox.backend.dto.response.AiExtractionResponse;
import com.flatox.backend.entity.*;
import com.flatox.backend.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Core AI Extraction Service — orchestrates the full scan pipeline:
 *
 * 1. Receive OCR text from mobile
 * 2. Run through VendorRuleEngine (deterministic, free)
 * 3. If low confidence → call Gemini API (LLM)
 * 4. Run duplicate detection
 * 5. Create expense draft (status = DRAFT)
 * 6. Return structured response to mobile
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiExtractionService {

    private final VendorRuleEngine vendorRuleEngine;
    private final DuplicateDetectionService duplicateDetectionService;
    private final ExpenseRepository expenseRepository;
    private final ApartmentRepository apartmentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${flatox.ai.groq-api-key:}")
    private String groqApiKey;

    @Value("${flatox.ai.groq-model:llama-3.3-70b-versatile}")
    private String groqModel;

    private static final double RULE_ENGINE_CONFIDENCE_THRESHOLD = 0.80;

    private HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Processes OCR text or a base64 image from a scanned document and creates an expense draft.
     */
    @Transactional
    public AiExtractionResponse processOcrText(Long apartmentId, String ocrText, String base64Image, Long recordedByUserId) {
        String textToProcess = ocrText;

        if (base64Image != null && !base64Image.isBlank()) {
            log.info("Received base64 image, performing backend OCR using OCR.space...");
            textToProcess = performOcr(base64Image);
        }

        if (textToProcess == null || textToProcess.isBlank()) {
            throw new RuntimeException("Could not extract any text from the scanned document.");
        }

        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment not found: " + apartmentId));

        // ── Step 1: Try the Rule Engine first (free, instant) ──
        Optional<VendorRuleEngine.MatchResult> ruleMatch = vendorRuleEngine.match(textToProcess, apartmentId);

        if (ruleMatch.isPresent() && ruleMatch.get().getConfidence() >= RULE_ENGINE_CONFIDENCE_THRESHOLD) {
            BigDecimal amount = extractAmount(textToProcess);
            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                log.info("Rule engine matched with confidence {} and found amount {}, skipping LLM",
                        ruleMatch.get().getConfidence(), amount);
                return buildFromRuleEngine(apartment, ruleMatch.get(), textToProcess, recordedByUserId);
            } else {
                log.info("Rule engine matched but failed to extract a valid amount. Falling back to Groq LLM...");
            }
        }

        // ── Step 2: Fall back to Groq LLM ──
        log.info("Rule engine insufficient confidence or no amount found, calling Groq AI...");
        return buildFromAi(apartment, textToProcess, recordedByUserId);
    }

    /**
     * Build extraction response from the rule engine match.
     */
    private AiExtractionResponse buildFromRuleEngine(
            Apartment apartment,
            VendorRuleEngine.MatchResult match,
            String ocrText,
            Long recordedByUserId
    ) {
        // Extract amounts from OCR text using simple regex patterns
        BigDecimal amount = extractAmount(ocrText);
        BigDecimal taxAmount = extractTaxAmount(ocrText);
        String invoiceNumber = extractInvoiceNumber(ocrText);
        LocalDate billDate = extractDate(ocrText, "bill date", "invoice date", "date of issue");
        LocalDate dueDate = extractDate(ocrText, "due date", "payment due", "last date");

        // Create the draft expense
        Expense draft = createDraftExpense(
                apartment, match.getVendorName(), match.getCategory(),
                amount, taxAmount, invoiceNumber, billDate, dueDate,
                match.getConfidence(), ocrText, "RULE_ENGINE", recordedByUserId
        );

        // Check for duplicates
        DuplicateDetectionService.DuplicateResult dupResult = duplicateDetectionService.checkDuplicate(
                apartment.getId(), invoiceNumber, match.getVendorId(), amount, billDate
        );

        return AiExtractionResponse.builder()
                .documentType(match.getCategory() + " Bill")
                .vendor(match.getVendorName())
                .category(match.getCategory())
                .invoiceNumber(invoiceNumber)
                .billDate(billDate != null ? billDate.toString() : null)
                .dueDate(dueDate != null ? dueDate.toString() : null)
                .amount(amount)
                .taxAmount(taxAmount)
                .confidence(match.getConfidence())
                .duplicateDetected(dupResult.isDuplicate())
                .duplicateExpenseId(dupResult.existingExpenseId())
                .draftExpenseId(draft.getId())
                .source("RULE_ENGINE")
                .build();
    }

    /**
     * Build extraction response from Groq AI understanding.
     */
    private AiExtractionResponse buildFromAi(Apartment apartment, String ocrText, Long recordedByUserId) {
        String aiJson = callGroqApi(ocrText);

        // Parse the AI response
        String documentType = "Unknown";
        String vendor = "Unknown";
        String category = "GENERAL";
        String invoiceNumber = null;
        String billDateStr = null;
        String dueDateStr = null;
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal taxAmount = null;
        double confidence = 0.70;

        try {
            JsonNode root = objectMapper.readTree(aiJson);
            documentType = getJsonField(root, "documentType", "Unknown");
            vendor = getJsonField(root, "vendor", "Unknown");
            category = getJsonField(root, "category", "GENERAL");
            invoiceNumber = getJsonField(root, "invoiceNumber", null);
            billDateStr = getJsonField(root, "billDate", null);
            dueDateStr = getJsonField(root, "dueDate", null);

            if (root.has("amount") && !root.get("amount").isNull()) {
                amount = new BigDecimal(root.get("amount").asText("0"));
            }
            if (root.has("gst") && !root.get("gst").isNull()) {
                taxAmount = new BigDecimal(root.get("gst").asText("0"));
            } else if (root.has("taxAmount") && !root.get("taxAmount").isNull()) {
                taxAmount = new BigDecimal(root.get("taxAmount").asText("0"));
            }
            if (root.has("confidence") && !root.get("confidence").isNull()) {
                confidence = root.get("confidence").asDouble(0.70);
            }
        } catch (Exception e) {
            log.error("Failed to parse Gemini AI response: {}", e.getMessage());
        }

        LocalDate billDate = parseDate(billDateStr);
        LocalDate dueDate = parseDate(dueDateStr);

        // Create the draft expense
        Expense draft = createDraftExpense(
                apartment, vendor, category, amount, taxAmount,
                invoiceNumber, billDate, dueDate,
                confidence, aiJson, "AI_LLM", recordedByUserId
        );

        // Check for duplicates
        DuplicateDetectionService.DuplicateResult dupResult = duplicateDetectionService.checkDuplicate(
                apartment.getId(), invoiceNumber, null, amount, billDate
        );

        return AiExtractionResponse.builder()
                .documentType(documentType)
                .vendor(vendor)
                .category(category)
                .invoiceNumber(invoiceNumber)
                .billDate(billDateStr)
                .dueDate(dueDateStr)
                .amount(amount)
                .taxAmount(taxAmount)
                .confidence(confidence)
                .duplicateDetected(dupResult.isDuplicate())
                .duplicateExpenseId(dupResult.existingExpenseId())
                .draftExpenseId(draft.getId())
                .source("AI_LLM")
                .build();
    }

    /**
     * Creates a DRAFT expense entry (not yet posted to ledger).
     */
    private Expense createDraftExpense(
            Apartment apartment, String vendorName, String category,
            BigDecimal amount, BigDecimal taxAmount,
            String invoiceNumber, LocalDate billDate, LocalDate dueDate,
            double confidence, String rawJson, String source,
            Long recordedByUserId
    ) {
        Expense draft = Expense.builder()
                .apartment(apartment)
                .category(category)
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .taxAmount(taxAmount)
                .invoiceNumber(invoiceNumber)
                .billDate(billDate)
                .dueDate(dueDate)
                .expenseDate(billDate != null ? billDate : LocalDate.now())
                .description("AI-scanned: " + vendorName + " — " + category)
                .documentType(category + " Bill")
                .aiConfidence(confidence)
                .aiRawJson(rawJson)
                .status("DRAFT")
                .source("AI_SCAN")
                .createdAt(LocalDateTime.now())
                .build();

        return expenseRepository.save(draft);
    }

    /**
     * Calls the Groq API with a structured extraction prompt using Llama models.
     */
    private String callGroqApi(String ocrText) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            log.warn("No Groq API key configured — returning mock AI response");
            return generateMockAiResponse(ocrText);
        }

        try {
            String prompt = buildExtractionPrompt(ocrText);

            // Build standard OpenAI/Groq chat completions payload with JSON Mode
            var requestMap = java.util.Map.of(
                    "model", groqModel,
                    "messages", java.util.List.of(
                            java.util.Map.of("role", "user", "content", prompt)
                    ),
                    "response_format", java.util.Map.of("type", "json_object"),
                    "temperature", 0.1
            );
            String requestBody = objectMapper.writeValueAsString(requestMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + groqApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode responseJson = objectMapper.readTree(response.body());
                String text = responseJson
                        .path("choices").get(0)
                        .path("message").path("content").asText("");

                return extractJsonFromResponse(text);
            } else {
                log.error("Groq API returned status {}: {}", response.statusCode(), response.body());
                return generateMockAiResponse(ocrText);
            }
        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage(), e);
            return generateMockAiResponse(ocrText);
        }
    }

    private String buildExtractionPrompt(String ocrText) {
        return """
                You are an Indian apartment society expense assistant. Analyze the following OCR text from a scanned bill or invoice and extract structured financial information.

                OCR Text:
                ---
                %s
                ---

                Extract the following and return ONLY a valid JSON object (no markdown, no explanation):
                {
                  "documentType": "type of document (e.g., Electricity Bill, Water Bill, Internet Bill, Vendor Invoice, GST Invoice, Maintenance Invoice, etc.)",
                  "vendor": "vendor or company name",
                  "category": "one of: ELECTRICITY, WATER, INTERNET, GAS, LIFT_MAINTENANCE, SECURITY, CLEANING, REPAIRS, OFFICE_PURCHASE, GST_INVOICE, TAX_RECEIPT, GENERAL",
                  "invoiceNumber": "invoice or bill number (null if not found)",
                  "billDate": "bill date in yyyy-MM-dd format (null if not found)",
                  "dueDate": "due date in yyyy-MM-dd format (null if not found)",
                  "amount": total amount as a number (without currency symbols),
                  "gst": GST or tax amount as a number (null if not found),
                  "confidence": confidence score between 0.0 and 1.0
                }

                Important:
                - Use Indian date formats (dd/MM/yyyy or dd-MMM-yyyy) when parsing dates
                - Amount should be the total payable amount
                - If GST is mentioned separately, extract it; otherwise set to null
                - Set confidence based on how clearly the information was found
                """.formatted(ocrText);
    }

    /**
     * Generates a mock AI response when no Gemini API key is configured.
     * Uses simple heuristics on the OCR text to produce a plausible result.
     */
    private String generateMockAiResponse(String ocrText) {
        String upper = ocrText.toUpperCase();
        String category = "GENERAL";
        String vendor = "Unknown Vendor";
        String docType = "Invoice";

        if (upper.contains("ELECTRIC") || upper.contains("POWER") || upper.contains("DISCOM")) {
            category = "ELECTRICITY";
            vendor = "Electricity Provider";
            docType = "Electricity Bill";
        } else if (upper.contains("WATER") || upper.contains("SEWERAGE")) {
            category = "WATER";
            vendor = "Water Board";
            docType = "Water Bill";
        } else if (upper.contains("INTERNET") || upper.contains("BROADBAND") || upper.contains("FIBER")) {
            category = "INTERNET";
            vendor = "Internet Provider";
            docType = "Internet Bill";
        }

        BigDecimal amount = extractAmount(ocrText);
        BigDecimal tax = extractTaxAmount(ocrText);
        String invoiceNum = extractInvoiceNumber(ocrText);

        return String.format("""
                {
                  "documentType": "%s",
                  "vendor": "%s",
                  "category": "%s",
                  "invoiceNumber": %s,
                  "billDate": null,
                  "dueDate": null,
                  "amount": %s,
                  "gst": %s,
                  "confidence": 0.75
                }
                """,
                docType, vendor, category,
                invoiceNum != null ? "\"" + invoiceNum + "\"" : "null",
                amount != null ? amount.toString() : "0",
                tax != null ? tax.toString() : "null"
        );
    }

    // ── Utility: extract amounts from OCR text ──

    private BigDecimal extractAmount(String text) {
        if (text == null) return BigDecimal.ZERO;
        // Look for patterns like ₹12,580 or Rs. 12580 or Amount: 12580.00
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?:₹|Rs\\.?|INR|Amount|Total|Payable|Net Amount)[\\s:]*([\\d,]+\\.?\\d*)",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                String amountStr = matcher.group(1).replace(",", "");
                return new BigDecimal(amountStr);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal extractTaxAmount(String text) {
        if (text == null) return null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?:GST|CGST|SGST|IGST|Tax|Service Tax)[\\s:]*(?:₹|Rs\\.?|INR)?[\\s]*([\\d,]+\\.?\\d*)",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                String amountStr = matcher.group(1).replace(",", "");
                return new BigDecimal(amountStr);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return null;
    }

    private String extractInvoiceNumber(String text) {
        if (text == null) return null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?:Bill No|Invoice No|Bill Number|Invoice Number|Receipt No|Ref No)[.:\\s]*([A-Za-z0-9\\-/]+)",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private LocalDate extractDate(String text, String... labels) {
        // Simplified date extraction — production would use more robust parsing
        if (text == null) return null;
        for (String label : labels) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    label + "[\\s:]*([\\d]{1,2}[\\s/\\-][A-Za-z]{3,9}[\\s/\\-][\\d]{2,4})",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return parseDate(matcher.group(1).trim());
            }
        }
        return null;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            // Try ISO format first (yyyy-MM-dd)
            return LocalDate.parse(dateStr);
        } catch (Exception e1) {
            try {
                // Try Indian format (dd/MM/yyyy)
                java.time.format.DateTimeFormatter fmt =
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                return LocalDate.parse(dateStr, fmt);
            } catch (Exception e2) {
                try {
                    // Try format like "05 Aug 2026"
                    java.time.format.DateTimeFormatter fmt =
                            java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy",
                                    java.util.Locale.ENGLISH);
                    return LocalDate.parse(dateStr, fmt);
                } catch (Exception e3) {
                    return null;
                }
            }
        }
    }

    private String getJsonField(JsonNode root, String field, String defaultValue) {
        if (root.has(field) && !root.get(field).isNull()) {
            return root.get(field).asText(defaultValue);
        }
        return defaultValue;
    }

    private String extractJsonFromResponse(String text) {
        // Strip markdown code fences if present
        if (text.contains("```json")) {
            int start = text.indexOf("```json") + 7;
            int end = text.indexOf("```", start);
            if (end > start) {
                return text.substring(start, end).trim();
            }
        }
        if (text.contains("```")) {
            int start = text.indexOf("```") + 3;
            int end = text.indexOf("```", start);
            if (end > start) {
                return text.substring(start, end).trim();
            }
        }
        return text.trim();
    }

    /**
     * Extracts text from a base64 image using the free OCR.space API.
     */
    private String performOcr(String base64Image) {
        try {
            String prefixedBase64 = base64Image;
            if (!prefixedBase64.startsWith("data:")) {
                prefixedBase64 = "data:image/jpeg;base64," + prefixedBase64;
            }

            // Url encode the base64 parameter
            String requestBody = "language=eng" +
                    "&isOverlayRequired=false" +
                    "&ocrengine=3" +
                    "&base64Image=" + java.net.URLEncoder.encode(prefixedBase64, java.nio.charset.StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.ocr.space/parse/image"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("apikey", "helloworld")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode parsedResults = root.path("ParsedResults");
                if (parsedResults.isArray() && parsedResults.size() > 0) {
                    String extractedText = parsedResults.get(0).path("ParsedText").asText("");
                    log.info("OCR.space successfully recognized text: {}", extractedText);
                    return extractedText;
                } else {
                    log.warn("OCR.space response did not contain ParsedResults: {}", response.body());
                }
            } else {
                log.error("OCR.space returned error status {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("OCR.space request failed: {}", e.getMessage(), e);
        }
        return "";
    }
}
