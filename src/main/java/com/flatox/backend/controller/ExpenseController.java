package com.flatox.backend.controller;

import com.flatox.backend.dto.request.ExpenseRecordRequest;
import com.flatox.backend.entity.Expense;
import com.flatox.backend.entity.Vendor;
import com.flatox.backend.entity.VendorContract;
import com.flatox.backend.repository.ExpenseRepository;
import com.flatox.backend.repository.VendorContractRepository;
import com.flatox.backend.repository.VendorRepository;
import com.flatox.backend.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepository;
    private final VendorRepository vendorRepository;
    private final VendorContractRepository contractRepository;

    @PostMapping("/expenses/{apartmentId}")
    public ResponseEntity<Expense> recordExpense(
            @PathVariable Long apartmentId,
            @RequestBody ExpenseRecordRequest request
    ) {
        Expense expense = expenseService.recordExpense(
                apartmentId,
                request.getBudgetItemId(),
                request.getVendorId(),
                request.getCategory(),
                request.getAmount(),
                request.getExpenseDate(),
                request.getDescription(),
                request.getRecordedByUserId(),
                request.getInvoiceUrl(),
                request.getReceiptUrl()
        );
        return ResponseEntity.ok(expense);
    }

    @GetMapping("/expenses/apartment/{apartmentId}")
    public List<Expense> getExpensesByApartment(@PathVariable Long apartmentId) {
        return expenseRepository.findByApartmentId(apartmentId);
    }

    @PostMapping("/vendors/apartment/{apartmentId}")
    public ResponseEntity<Vendor> createVendor(
            @PathVariable Long apartmentId,
            @RequestBody Vendor vendor
    ) {
        com.flatox.backend.entity.Apartment apt = new com.flatox.backend.entity.Apartment();
        apt.setId(apartmentId);
        vendor.setApartment(apt);
        return ResponseEntity.ok(vendorRepository.save(vendor));
    }

    @GetMapping("/vendors/apartment/{apartmentId}")
    public List<Vendor> getVendorsByApartment(@PathVariable Long apartmentId) {
        return vendorRepository.findByApartmentId(apartmentId);
    }

    @PostMapping("/vendor-contracts/vendor/{vendorId}")
    public ResponseEntity<VendorContract> createContract(
            @PathVariable Long vendorId,
            @RequestBody VendorContract contract
    ) {
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        contract.setVendor(vendor);
        return ResponseEntity.ok(contractRepository.save(contract));
    }

    @GetMapping("/vendor-contracts/vendor/{vendorId}")
    public List<VendorContract> getContractsByVendor(@PathVariable Long vendorId) {
        return contractRepository.findByVendorId(vendorId);
    }
}
