package com.flatox.backend.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class FlatGenerationRequest {

    private Long apartmentId;

    private List<BlockRequest> blocks;
}