package com.flatox.backend.controller;

import com.flatox.backend.dto.request.FlatGenerationRequest;
import com.flatox.backend.dto.response.FlatResponse;
import com.flatox.backend.entity.Flat;
import com.flatox.backend.repository.FlatRepository;
import com.flatox.backend.service.FlatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flats")
@RequiredArgsConstructor
@CrossOrigin("*")
public class FlatController {

    private final FlatRepository flatRepository;
    private final FlatService flatService;

    @GetMapping
    public List<Flat> getAllFlats() {
        return flatRepository.findAll();
    }

    @PostMapping("/generate")
    public String generateFlats(
            @RequestBody FlatGenerationRequest request
    ) {
        return flatService.generateFlats(request);
    }

    @GetMapping("/apartment/{apartmentId}")
    public List<FlatResponse> getFlatsByApartment(
            @PathVariable Long apartmentId
    ) {
        return flatService.getFlatsByApartment(apartmentId);
    }
}
