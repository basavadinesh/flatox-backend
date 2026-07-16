package com.flatox.backend.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ExpenseDraftRequest {
    private String documentType;
    private String vendor;
    private String category;
    private String invoiceNumber;
    private String billDate;   // ISO format: yyyy-MM-dd
    private String dueDate;    // ISO format: yyyy-MM-dd
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private Double confidence;
    private String ocrText;
    private String imageUrl;
    private String description;
    private Long vendorId;
    private Long budgetItemId;
}
