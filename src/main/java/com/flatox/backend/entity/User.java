package com.flatox.backend.entity;

import com.flatox.backend.enums.ApprovalStatus;
import com.flatox.backend.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Column(unique = true, nullable = false)
    private String phone;

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    @ManyToOne
    @JoinColumn(name = "flat_id")
    private Flat flat;

    private String userType;

    private String ownerStatus;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();

        if (role == null) {
            role = Role.RESIDENT;
        }
    }
}