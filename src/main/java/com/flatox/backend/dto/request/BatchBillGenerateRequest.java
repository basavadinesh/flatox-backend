package com.flatox.backend.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class BatchBillGenerateRequest {
    private Long templateId;
    private String title;
    private LocalDate dueDate;
}
