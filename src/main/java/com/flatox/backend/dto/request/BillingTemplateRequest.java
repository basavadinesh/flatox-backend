package com.flatox.backend.dto.request;

import com.flatox.backend.enums.BillFrequency;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class BillingTemplateRequest {
    private String title;
    private BigDecimal amount;
    private BillFrequency frequency;
    private Integer dueDayOfMonth;
    private Integer gracePeriodDays;
    private BigDecimal lateFeePenalty;
}
