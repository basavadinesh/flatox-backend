package com.flatox.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "budget_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "budget_plan_id", nullable = false)
    private BudgetPlan budgetPlan;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal allocatedAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal actualSpending;

    @PrePersist
    public void prePersist() {
        if (actualSpending == null) {
            actualSpending = BigDecimal.ZERO;
        }
    }
}
