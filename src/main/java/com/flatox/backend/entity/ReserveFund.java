package com.flatox.backend.entity;

import com.flatox.backend.enums.ReserveFundType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reserve_funds", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"apartment_id", "fundType"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReserveFund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReserveFundType fundType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        lastUpdated = LocalDateTime.now();
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
    }
}
