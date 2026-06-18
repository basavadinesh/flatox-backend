package com.flatox.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String message;

    private String token;

    private String role;

    private String approvalStatus;

    private String fullName;

    private String flatNumber;

    private String apartmentName;

    private String userType;

    private String ownerStatus;
}
