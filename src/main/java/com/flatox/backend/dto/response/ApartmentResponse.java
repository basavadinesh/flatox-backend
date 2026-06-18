package com.flatox.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApartmentResponse {

    private Long id;

    private String name;

    private String locality;

    private String city;
}
