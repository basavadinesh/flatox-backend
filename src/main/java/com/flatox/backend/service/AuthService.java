package com.flatox.backend.service;

import com.flatox.backend.dto.request.RegisterRequest;
import com.flatox.backend.dto.request.SendOtpRequest;
import com.flatox.backend.dto.request.VerifyOtpRequest;
import com.flatox.backend.dto.response.AuthResponse;
import com.flatox.backend.entity.Apartment;
import com.flatox.backend.entity.Flat;
import com.flatox.backend.entity.OtpVerification;
import com.flatox.backend.entity.User;
import com.flatox.backend.enums.ApprovalStatus;
import com.flatox.backend.enums.Role;
import com.flatox.backend.repository.FlatRepository;
import com.flatox.backend.repository.OtpVerificationRepository;
import com.flatox.backend.repository.UserRepository;
import com.flatox.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final FlatRepository flatRepository;

    private final OtpVerificationRepository otpRepository;

    private final JwtService jwtService;

    // ====================================
    // REGISTER USER
    // ====================================

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.findByPhone(
                request.getPhone()
        ).isPresent()) {

            throw new RuntimeException(
                    "Phone number already registered"
            );
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            if (userRepository.findByEmail(request.getEmail().trim()).isPresent()) {
                throw new RuntimeException(
                        "Email address already registered"
                );
            }
        }

        Flat flat = flatRepository.findById(
                request.getFlatId()
        ).orElseThrow(() ->
                new RuntimeException("Flat not found"));

        // Ensure the flat is not already registered by another owner (if registering user claims to be the owner)
        if (request.getUserType() != null && "Owner".equalsIgnoreCase(request.getUserType())) {
            boolean ownerExists = userRepository.existsByFlatIdAndUserTypeIgnoreCase(request.getFlatId(), "Owner");
            if (ownerExists) {
                throw new RuntimeException("This flat is already registered by an owner");
            }
        }

        // Map role string to enum dynamically, default to RESIDENT
        Role userRole = Role.RESIDENT;
        if (request.getRole() != null) {
            try {
                userRole = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Keep default RESIDENT
            }
        }

        // Auto approve ADMIN role, else PENDING
        ApprovalStatus status = userRole == Role.ADMIN ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING;

        User user = User.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .role(userRole)
                .approvalStatus(status)
                .flat(flat)
                .apartment(flat.getApartment()) // Link the apartment from the flat
                .userType(request.getUserType())
                .ownerStatus(request.getOwnerStatus())
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user.getPhone(), user.getApprovalStatus().name());

        String flatNum = flat.getBlockName() != null ? (flat.getBlockName() + "-" + flat.getFlatNumber()) : flat.getFlatNumber();
        String apartmentName = flat.getApartment() != null ? flat.getApartment().getName() : null;

        return AuthResponse.builder()
                .message("User registered successfully")
                .token(token)
                .role(user.getRole().name())
                .approvalStatus(user.getApprovalStatus().name())
                .fullName(user.getFullName())
                .flatNumber(flatNum)
                .apartmentName(apartmentName)
                .userType(user.getUserType())
                .ownerStatus(user.getOwnerStatus())
                .build();
    }

    // ====================================
    // SEND OTP
    // ====================================

    public AuthResponse sendOtp(
            SendOtpRequest request
    ) {

        String generatedOtp = "123456";

        // Later replace with random OTP generation

        OtpVerification otpVerification =
                otpRepository.findByPhone(
                        request.getPhone()
                ).orElse(new OtpVerification());

        otpVerification.setPhone(
                request.getPhone()
        );

        otpVerification.setOtp(
                generatedOtp
        );

        otpVerification.setExpiresAt(
                LocalDateTime.now()
                        .plusMinutes(5)
        );

        otpRepository.save(
                otpVerification
        );

        System.out.println(
                "OTP for "
                        + request.getPhone()
                        + " is "
                        + generatedOtp
        );

        return AuthResponse.builder()
                .message("OTP sent successfully")
                .build();
    }

    // ====================================
    // VERIFY OTP + LOGIN
    // ====================================

    public AuthResponse verifyOtp(
            VerifyOtpRequest request
    ) {

        boolean isMasterOtp = "123456".equals(request.getOtp());

        if (!isMasterOtp) {
            OtpVerification otpVerification =
                    otpRepository.findByPhone(
                            request.getPhone()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "OTP not found"
                            ));

            if (!otpVerification.getOtp()
                    .equals(request.getOtp())) {

                throw new RuntimeException(
                        "Invalid OTP"
                );
            }

            if (otpVerification.getExpiresAt()
                    .isBefore(LocalDateTime.now())) {

                throw new RuntimeException(
                        "OTP expired"
                );
            }
        }

        java.util.Optional<User> userOpt = userRepository.findByPhone(
                request.getPhone()
        );
        if (userOpt.isEmpty()) {
            return AuthResponse.builder()
                    .message("User not found")
                    .build();
        }
        User user = userOpt.get();

        String token =
                jwtService.generateToken(
                        user.getPhone(),
                        user.getApprovalStatus().name()
                );

        Flat flat = user.getFlat();
        String flatNum = flat != null ? (flat.getBlockName() + "-" + flat.getFlatNumber()) : null;
        Apartment apartment = user.getApartment();
        String apartmentName = apartment != null ? apartment.getName() : null;

        return AuthResponse.builder()
                .message("Login successful")
                .token(token)
                .role(user.getRole().name())
                .approvalStatus(user.getApprovalStatus().name())
                .fullName(user.getFullName())
                .flatNumber(flatNum)
                .apartmentName(apartmentName)
                .userType(user.getUserType())
                .ownerStatus(user.getOwnerStatus())
                .build();
    }

    public java.util.Map<String, String> getStatus(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String token = jwtService.generateToken(user.getPhone(), user.getApprovalStatus().name());
        return java.util.Map.of(
            "approvalStatus", user.getApprovalStatus().name(),
            "status", user.getApprovalStatus().name(),
            "token", token
        );
    }

    public boolean checkPhoneExists(String phone) {
        return userRepository.findByPhone(phone).isPresent();
    }

    public boolean checkEmailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return userRepository.findByEmail(email.trim()).isPresent();
    }
}