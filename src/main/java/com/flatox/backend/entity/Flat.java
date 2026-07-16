package com.flatox.backend.entity;

import com.flatox.backend.enums.FlatStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "flats")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String blockName;

    private String flatNumber;

    @Enumerated(EnumType.STRING)
    private FlatStatus status;

    @ManyToOne
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    @Column(name = "square_footage")
    private Double squareFootage;

    @Column(name = "bhk_type")
    private String bhkType;

}
