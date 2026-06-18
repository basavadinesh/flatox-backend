package com.flatox.backend.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class PaymentInitiateRequest {
    private Long billId;
    private Long payerUserId;
    private BigDecimal amount;
    private String paymentMethod;
}
