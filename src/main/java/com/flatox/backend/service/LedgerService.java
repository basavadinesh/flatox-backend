package com.flatox.backend.service;

import com.flatox.backend.entity.*;
import com.flatox.backend.enums.LedgerEntryType;
import com.flatox.backend.enums.ReserveFundType;
import com.flatox.backend.repository.AccountingLedgerRepository;
import com.flatox.backend.repository.ReserveFundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final AccountingLedgerRepository ledgerRepository;
    private final ReserveFundRepository reserveFundRepository;

    /**
     * Posts an entry to the double-entry accounting ledger.
     * Computes a rolling cryptographic hash chain to prevent tampering.
     */
    @Transactional
    public AccountingLedger postToLedger(
            Apartment apartment,
            Transaction transaction,
            Expense expense,
            LedgerEntryType entryType,
            String accountName,
            BigDecimal amount,
            String description
    ) {
        // 1. Get the last entry's hash for this apartment to chain the checksum
        Optional<AccountingLedger> lastLedgerOpt = ledgerRepository
                .findFirstByApartmentIdOrderByIdDesc(apartment.getId());
        String previousHash = lastLedgerOpt.map(AccountingLedger::getSystemHash).orElse("0000000000000000000000000000000000000000000000000000000000000000");

        // 2. Build the new ledger entry
        AccountingLedger ledgerEntry = AccountingLedger.builder()
                .apartment(apartment)
                .transaction(transaction)
                .expense(expense)
                .entryType(entryType)
                .accountName(accountName)
                .amount(amount)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();

        // 3. Compute rolling anti-tamper SHA-256 hash
        String hashPayload = String.format("%d|%s|%s|%.2f|%s",
                apartment.getId(),
                entryType.name(),
                accountName,
                amount.doubleValue(),
                previousHash
        );
        String systemHash = calculateSHA256(hashPayload);
        ledgerEntry.setSystemHash(systemHash);

        AccountingLedger saved = ledgerRepository.save(ledgerEntry);

        // 4. Update the matching Reserve Fund balance
        updateReserveFunds(apartment, entryType, accountName, amount);

        return saved;
    }

    private void updateReserveFunds(Apartment apartment, LedgerEntryType entryType, String accountName, BigDecimal amount) {
        ReserveFundType fundType = ReserveFundType.MAINTENANCE_RESERVE;

        // Route accounts to appropriate reserves
        if (accountName.equalsIgnoreCase("CORPUS_FUND")) {
            fundType = ReserveFundType.CORPUS;
        } else if (accountName.equalsIgnoreCase("SINKING_FUND")) {
            fundType = ReserveFundType.SINKING;
        } else if (accountName.equalsIgnoreCase("EMERGENCY")) {
            fundType = ReserveFundType.EMERGENCY;
        }

        final ReserveFundType finalFundType = fundType;
        ReserveFund reserve = reserveFundRepository
                .findByApartmentIdAndFundType(apartment.getId(), finalFundType)
                .orElseGet(() -> ReserveFund.builder()
                        .apartment(apartment)
                        .fundType(finalFundType)
                        .balance(BigDecimal.ZERO)
                        .build());

        BigDecimal currentBalance = reserve.getBalance();
        if (entryType == LedgerEntryType.CREDIT) {
            reserve.setBalance(currentBalance.add(amount));
        } else {
            reserve.setBalance(currentBalance.subtract(amount));
        }

        reserveFundRepository.save(reserve);
    }

    private String calculateSHA256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("SHA-256 algorithm not available", ex);
        }
    }
}
