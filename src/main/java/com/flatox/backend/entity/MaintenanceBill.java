package com.flatox.backend.entity;

import com.flatox.backend.enums.BillStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "maintenance_bills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @ManyToOne
    @JoinColumn(name = "flat_id", nullable = false)
    private Flat flat;

    @ManyToOne
    @JoinColumn(name = "billing_template_id")
    private BillingTemplate billingTemplate;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal baseAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal paidAmount;

    @Column(nullable = false)
    private LocalDate dueDate;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MaintenanceBillItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillStatus status;

    private LocalDateTime generatedAt;

    private LocalDateTime paidAt;

    @PrePersist
    public void prePersist() {
        generatedAt = LocalDateTime.now();
        if (penaltyAmount == null) {
            penaltyAmount = BigDecimal.ZERO;
        }
        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }
        if (status == null) {
            status = BillStatus.UNPAID;
        }
    }

    public BigDecimal getTotalAmount() {
        return baseAmount.add(penaltyAmount);
    }
}
