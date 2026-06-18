package com.flatox.backend.service;

import com.flatox.backend.dto.request.BlockRequest;
import com.flatox.backend.dto.request.FlatGenerationRequest;
import com.flatox.backend.dto.response.FlatResponse;
import com.flatox.backend.entity.Apartment;
import com.flatox.backend.entity.Flat;
import com.flatox.backend.enums.FlatStatus;
import com.flatox.backend.repository.ApartmentRepository;
import com.flatox.backend.repository.FlatRepository;
import com.flatox.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlatService {

    private final FlatRepository flatRepository;

    private final ApartmentRepository apartmentRepository;

    private final UserRepository userRepository;

    // ====================================
    // GENERATE FLATS
    // ====================================

    public String generateFlats(
            FlatGenerationRequest request
    ) {

        Apartment apartment =
                apartmentRepository.findById(
                        request.getApartmentId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Apartment not found"
                        ));

        List<Flat> flatsToSave = new ArrayList<>();

        for (BlockRequest block : request.getBlocks()) {
            String blockName = block.getBlockName();
            for (String flatNumber : block.getFlats()) {
                String fullFlatNumber = (blockName != null && !blockName.trim().isEmpty() && !blockName.trim().equalsIgnoreCase("Apartment"))
                        ? blockName.trim() + "-" + flatNumber
                        : flatNumber;

                Flat flat = Flat.builder()
                        .blockName(blockName)
                        .flatNumber(fullFlatNumber)
                        .status(FlatStatus.VACANT)
                        .apartment(apartment)
                        .build();

                flatsToSave.add(flat);
            }
        }

        flatRepository.saveAll(flatsToSave);

        return "Flats generated successfully";
    }

    // ====================================
    // GET FLATS BY APARTMENT
    // ====================================

    public List<FlatResponse> getFlatsByApartment(
            Long apartmentId
    ) {

        List<Flat> flats =
                flatRepository.findByApartmentId(
                        apartmentId
                );

        return flats.stream()
                .map(flat ->
                        FlatResponse.builder()
                                .id(flat.getId())
                                .flatNumber(flat.getFlatNumber())
                                .occupied(userRepository.existsByFlatIdAndApprovalStatusNot(
                                        flat.getId(),
                                        com.flatox.backend.enums.ApprovalStatus.REJECTED
                                ))
                                .build()
                )
                .toList();
    }
}
