package com.flatox.backend.entity;

import com.flatox.backend.enums.BillFrequency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillFrequency frequency;

    @Column(nullable = false)
    private Integer dueDayOfMonth;

    private Integer gracePeriodDays;

    @Column(precision = 10, scale = 2)
    private BigDecimal lateFeePenalty;

    private Boolean isActive;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (gracePeriodDays == null) {
            gracePeriodDays = 5;
        }
        if (lateFeePenalty == null) {
            lateFeePenalty = BigDecimal.ZERO;
        }
    }
}
