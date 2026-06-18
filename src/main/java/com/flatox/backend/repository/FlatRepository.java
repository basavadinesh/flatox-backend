package com.flatox.backend.repository;

import com.flatox.backend.entity.Flat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FlatRepository
        extends JpaRepository<Flat, Long> {

    List<Flat> findByApartmentId(Long apartmentId);
}