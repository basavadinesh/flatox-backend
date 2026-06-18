package com.flatox.backend.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class PaymentVerifyRequest {
    private UUID transactionId;
    private String gatewayPaymentId;
    private String gatewaySignature;
}
