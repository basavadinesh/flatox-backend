package com.flatox.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FlatResponse {

    private Long id;

    private String flatNumber;

    private boolean occupied;
}
