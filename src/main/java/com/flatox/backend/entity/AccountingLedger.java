package com.flatox.backend.entity;

import com.flatox.backend.enums.LedgerEntryType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounting_ledger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerEntryType entryType;

    @Column(nullable = false)
    private String accountName; // e.g., 'CASH', 'MAINTENANCE_RECEIVABLE', 'VENDOR_PAYABLE'

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 64)
    private String systemHash; // Roll-forward chain SHA-256 validation

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (systemHash == null) {
            systemHash = "";
        }
    }
}
