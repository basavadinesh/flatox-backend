package com.flatox.backend.repository;

import com.flatox.backend.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApartmentRepository
        extends JpaRepository<Apartment, Long> {
}
