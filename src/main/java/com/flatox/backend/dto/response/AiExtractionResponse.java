package com.flatox.backend.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiExtractionResponse {
    private String documentType;
    private String vendor;
    private String category;
    private String invoiceNumber;
    private String billDate;
    private String dueDate;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private Double confidence;
    private boolean duplicateDetected;
    private Long duplicateExpenseId;
    private Long draftExpenseId;
    private String source; // RULE_ENGINE or AI_LLM
}
