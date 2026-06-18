package com.flatox.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDTO {

    private Long id;

    private String fullName;

    private String phone;

    private String email;

    private String role;

    private String flatNumber;

    private String userType;

    private String ownerStatus;

    private String approvalStatus;
}
