package com.flatox.backend.controller;

import com.flatox.backend.dto.request.ApartmentRequest;
import com.flatox.backend.dto.response.ApartmentResponse;
import com.flatox.backend.entity.Apartment;
import com.flatox.backend.service.ApartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/apartments")
@RequiredArgsConstructor

@CrossOrigin("*")
public class ApartmentController
{
    private final ApartmentService apartmentService;

    @PostMapping
    public Apartment createApartment(
            @RequestBody ApartmentRequest request
    ) {
        return apartmentService.createApartment(request);
    }

    @GetMapping
    public List<ApartmentResponse> getAllApartments() {
        return apartmentService.getAllApartments();
    }
}