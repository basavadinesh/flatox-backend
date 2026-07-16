package com.flatox.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @ManyToOne
    @JoinColumn(name = "budget_item_id")
    private BudgetItem budgetItem;

    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "recorded_by_user_id")
    private User recordedBy;

    private String invoiceUrl;

    private String receiptUrl;

    // --- AI Expense Assistant fields ---

    @Column(length = 20)
    @Builder.Default
    private String status = "CONFIRMED"; // DRAFT, CONFIRMED, REJECTED

    private String invoiceNumber;

    private LocalDate billDate;

    private LocalDate dueDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(length = 100)
    private String documentType;

    private Double aiConfidence;

    @Column(columnDefinition = "TEXT")
    private String aiRawJson;

    @Column(length = 20)
    @Builder.Default
    private String source = "MANUAL"; // MANUAL, AI_SCAN

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
