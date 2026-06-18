package com.flatox.backend.service;

import com.flatox.backend.dto.response.UserResponseDTO;
import com.flatox.backend.entity.User;
import com.flatox.backend.enums.ApprovalStatus;
import com.flatox.backend.enums.Role;
import com.flatox.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SocietyService {

    private final UserRepository userRepository;

    public List<UserResponseDTO> getApprovals(
            String phone
    ) {

        User admin = userRepository
                .findByPhone(phone)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Admin not found"
                        ));

        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException(
                    "Access denied"
            );
        }

        Long apartmentId =
                admin.getApartment().getId();

        List<User> residents =
                userRepository
                        .findByApartmentIdAndRole(
                                apartmentId,
                                Role.RESIDENT
                        );

        return residents.stream()
                .filter(user -> user.getApprovalStatus() == ApprovalStatus.PENDING)
                .map(this::mapToDTO)
                .toList();
    }

    public Map<String, String> approveUser(Long id, String adminPhone) {
        User admin = userRepository.findByPhone(adminPhone)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }

        User resident = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resident not found"));

        if (!resident.getApartment().getId().equals(admin.getApartment().getId())) {
            throw new RuntimeException("Access denied: Resident not in your apartment");
        }

        resident.setApprovalStatus(ApprovalStatus.APPROVED);
        userRepository.save(resident);

        return Map.of("message", "User approved successfully");
    }

    public Map<String, String> rejectUser(Long id, String adminPhone) {
        User admin = userRepository.findByPhone(adminPhone)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }

        User resident = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resident not found"));

        if (!resident.getApartment().getId().equals(admin.getApartment().getId())) {
            throw new RuntimeException("Access denied: Resident not in your apartment");
        }

        resident.setApprovalStatus(ApprovalStatus.REJECTED);
        userRepository.save(resident);

        return Map.of("message", "User rejected successfully");
    }

    public List<UserResponseDTO> getResidents(String phone) {
        User admin = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }

        Long apartmentId = admin.getApartment().getId();
        List<User> residents = userRepository.findByApartmentIdAndRole(apartmentId, Role.RESIDENT);

        return residents.stream()
                .map(this::mapToDTO)
                .toList();
    }

    public Map<String, String> deleteResident(Long id, String adminPhone) {
        User admin = userRepository.findByPhone(adminPhone)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }

        User resident = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resident not found"));

        if (!resident.getApartment().getId().equals(admin.getApartment().getId())) {
            throw new RuntimeException("Access denied: Resident not in your apartment");
        }

        if (resident.getRole() != Role.RESIDENT) {
            throw new RuntimeException("Access denied: Can only delete residents");
        }

        if (resident.getApprovalStatus() != ApprovalStatus.REJECTED) {
            throw new RuntimeException("Access denied: Can only delete rejected residents");
        }

        userRepository.delete(resident);
        return Map.of("message", "Resident deleted successfully");
    }

    private UserResponseDTO mapToDTO(
            User user
    ) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .flatNumber(user.getFlat() != null ? user.getFlat().getFlatNumber() : null)
                .userType(user.getUserType())
                .ownerStatus(user.getOwnerStatus())
                .approvalStatus(user.getApprovalStatus() != null ? user.getApprovalStatus().name() : null)
                .build();
    }
}

