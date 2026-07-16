package com.flatox.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Column(nullable = false)
    private String keyword; // e.g. "APSPDCL", "BSNL", "JioFiber"

    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @Column(nullable = false, length = 100)
    private String category; // e.g. "ELECTRICITY", "INTERNET"

    @Builder.Default
    private Double confidence = 1.0;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
