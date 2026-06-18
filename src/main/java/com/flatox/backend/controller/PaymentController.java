package com.flatox.backend.controller;

import com.flatox.backend.dto.request.PaymentInitiateRequest;
import com.flatox.backend.dto.request.PaymentVerifyRequest;
import com.flatox.backend.entity.Transaction;
import com.flatox.backend.repository.TransactionRepository;
import com.flatox.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/payments")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PaymentController {

    private final PaymentService paymentService;
    private final TransactionRepository transactionRepository;

    @PostMapping("/checkout/initiate/{apartmentId}")
    public ResponseEntity<Transaction> initiateCheckout(
            @PathVariable Long apartmentId,
            @RequestBody PaymentInitiateRequest request
    ) {
        Transaction tx = paymentService.initiatePayment(
                apartmentId,
                request.getBillId(),
                request.getPayerUserId(),
                request.getAmount(),
                request.getPaymentMethod()
        );
        return ResponseEntity.ok(tx);
    }

    @PostMapping("/checkout/verify")
    public ResponseEntity<Transaction> verifyCheckout(
            @RequestBody PaymentVerifyRequest request
    ) {
        Transaction tx = paymentService.verifyAndCapturePayment(
                request.getTransactionId(),
                request.getGatewayPaymentId(),
                request.getGatewaySignature()
        );
        return ResponseEntity.ok(tx);
    }

    @GetMapping("/transactions/apartment/{apartmentId}")
    public List<Transaction> getTransactionsByApartment(@PathVariable Long apartmentId) {
        return transactionRepository.findByApartmentId(apartmentId);
    }

    @GetMapping("/transactions/user/{payerId}")
    public List<Transaction> getTransactionsByUser(@PathVariable Long payerId) {
        return transactionRepository.findByPayerId(payerId);
    }
}
