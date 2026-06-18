package com.flatox.backend.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class BlockRequest {

    private String blockName;

    private List<String> flats;
}
