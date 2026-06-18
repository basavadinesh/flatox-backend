package com.flatox.backend.service;

import com.flatox.backend.dto.request.ApartmentRequest;
import com.flatox.backend.dto.response.ApartmentResponse;
import com.flatox.backend.entity.Apartment;
import com.flatox.backend.repository.ApartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApartmentService {

    private final ApartmentRepository apartmentRepository;

    public Apartment createApartment(ApartmentRequest request) {

        Apartment apartment = Apartment.builder()
                .name(request.getName())
                .address(request.getLocality() != null ? request.getLocality() : request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .build();

        return apartmentRepository.save(apartment);
    }

    public List<ApartmentResponse> getAllApartments() {
        return apartmentRepository.findAll().stream()
                .map(apartment -> ApartmentResponse.builder()
                        .id(apartment.getId())
                        .name(apartment.getName())
                        .locality(apartment.getAddress())
                        .city(apartment.getCity())
                        .build())
                .collect(Collectors.toList());
    }
}