package com.flatox.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Column(nullable = false)
    private LocalDate fiscalYearStart;

    @Column(nullable = false)
    private LocalDate fiscalYearEnd;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalEstimatedRevenue;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAllocatedExpense;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
