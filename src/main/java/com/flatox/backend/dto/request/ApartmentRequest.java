package com.flatox.backend.dto.request;

import lombok.Data;

@Data
public class ApartmentRequest {

    private String name;

    private String address;

    private String locality;

    private String city;

    private String state;

    private String pincode;
}