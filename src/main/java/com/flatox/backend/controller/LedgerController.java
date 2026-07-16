package com.flatox.backend.controller;

import com.flatox.backend.entity.AccountingLedger;
import com.flatox.backend.entity.BudgetPlan;
import com.flatox.backend.entity.ReserveFund;
import com.flatox.backend.repository.AccountingLedgerRepository;
import com.flatox.backend.repository.BudgetPlanRepository;
import com.flatox.backend.repository.ReserveFundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@CrossOrigin("*")
public class LedgerController {

    private final AccountingLedgerRepository ledgerRepository;
    private final ReserveFundRepository reserveFundRepository;
    private final BudgetPlanRepository budgetPlanRepository;

    @GetMapping("/ledger/apartment/{apartmentId}")
    public List<AccountingLedger> getLedgerEntries(@PathVariable Long apartmentId) {
        return ledgerRepository.findByApartmentId(apartmentId);
    }

    @GetMapping("/reserves/apartment/{apartmentId}")
    public List<ReserveFund> getReservesByApartment(@PathVariable Long apartmentId) {
        return reserveFundRepository.findByApartmentId(apartmentId);
    }

    @PostMapping("/budgets/apartment/{apartmentId}")
    public ResponseEntity<BudgetPlan> createBudgetPlan(
            @PathVariable Long apartmentId,
            @RequestBody BudgetPlan plan
    ) {
        com.flatox.backend.entity.Apartment apt = new com.flatox.backend.entity.Apartment();
        apt.setId(apartmentId);
        plan.setApartment(apt);
        
        if (plan.getItems() != null) {
            for (com.flatox.backend.entity.BudgetItem item : plan.getItems()) {
                item.setBudgetPlan(plan);
            }
        }
        
        return ResponseEntity.ok(budgetPlanRepository.save(plan));
    }

    @GetMapping("/budgets/apartment/{apartmentId}")
    public List<BudgetPlan> getBudgetsByApartment(@PathVariable Long apartmentId) {
        return budgetPlanRepository.findByApartmentIdOrderByIdDesc(apartmentId);
    }
}
