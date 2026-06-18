package com.flatox.backend.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ExpenseRecordRequest {
    private Long budgetItemId;
    private Long vendorId;
    private String category;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String description;
    private Long recordedByUserId;
    private String invoiceUrl;
    private String receiptUrl;
}
