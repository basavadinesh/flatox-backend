package com.flatox.backend.service;

import com.flatox.backend.entity.*;
import com.flatox.backend.enums.BillStatus;
import com.flatox.backend.enums.LedgerEntryType;
import com.flatox.backend.enums.PaymentStatus;
import com.flatox.backend.repository.ApartmentRepository;
import com.flatox.backend.repository.MaintenanceBillRepository;
import com.flatox.backend.repository.TransactionRepository;
import com.flatox.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import com.razorpay.RazorpayClient;
import com.razorpay.Order;
import com.razorpay.Utils;
import org.json.JSONObject;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final MaintenanceBillRepository billRepository;
    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final LedgerService ledgerService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    /**
     * Initializes a transaction order record.
     */
    @Transactional
    public Transaction initiatePayment(Long apartmentId, Long billId, Long payerUserId, BigDecimal amount, String paymentMethod) {
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));
        
        MaintenanceBill bill = billId != null ? billRepository.findById(billId).orElse(null) : null;
        User payer = payerUserId != null ? userRepository.findById(payerUserId).orElse(null) : null;

        Transaction tx = Transaction.builder()
                .apartment(apartment)
                .bill(bill)
                .payer(payer)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            // Amount in paise
            orderRequest.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

            Order order = razorpay.orders.create(orderRequest);
            tx.setGatewayOrderId(order.get("id"));
            tx.setGatewayName("RAZORPAY");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initiate Razorpay order: " + e.getMessage());
        }

        return transactionRepository.save(tx);
    }

    /**
     * Verifies the gateway payload signature and updates the transaction and ledger details.
     */
    @Transactional
    public Transaction verifyAndCapturePayment(UUID transactionId, String gatewayPaymentId, String gatewaySignature) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (tx.getStatus() == PaymentStatus.SUCCESS) {
            return tx;
        }

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", tx.getGatewayOrderId());
            options.put("razorpay_payment_id", gatewayPaymentId);
            options.put("razorpay_signature", gatewaySignature);
            
            boolean status = Utils.verifyPaymentSignature(options, razorpayKeySecret);
            if (!status) {
                throw new RuntimeException("Payment signature verification failed");
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid payment signature: " + e.getMessage());
        }

        tx.setGatewayPaymentId(gatewayPaymentId);
        tx.setGatewaySignature(gatewaySignature);
        tx.setStatus(PaymentStatus.SUCCESS);
        tx.setCompletedAt(LocalDateTime.now());
        tx.setReceiptNumber("REC-" + System.currentTimeMillis() + "-" + tx.hashCode());

        Transaction savedTx = transactionRepository.save(tx);

        // Update the linked Maintenance Bill details
        MaintenanceBill bill = tx.getBill();
        if (bill != null) {
            bill.setPaidAmount(bill.getPaidAmount().add(tx.getAmount()));
            if (bill.getPaidAmount().compareTo(bill.getTotalAmount()) >= 0) {
                bill.setStatus(BillStatus.PAID);
                bill.setPaidAt(LocalDateTime.now());
            } else {
                bill.setStatus(BillStatus.PARTIALLY_PAID);
            }
            billRepository.save(bill);
        }

        // Post to ledger: Credit Receivables, Debit Cash (increases asset cash balance)
        ledgerService.postToLedger(
                tx.getApartment(),
                savedTx,
                null,
                LedgerEntryType.CREDIT,
                "MAINTENANCE_RECEIVABLE",
                tx.getAmount(),
                "Payment cleared for bill ID: " + (bill != null ? bill.getId() : "N/A") + " via " + tx.getPaymentMethod()
        );

        return savedTx;
    }
}
