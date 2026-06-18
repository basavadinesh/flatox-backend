package com.flatox.backend.dto.request;

import lombok.Data;

@Data
public class RegisterRequest {

    private String fullName;

    private String phone;

    private String email;

    private String password;

    private String flatNumber;

    private Long flatId;

    private String role;

    private String userType;

    private String ownerStatus;
}