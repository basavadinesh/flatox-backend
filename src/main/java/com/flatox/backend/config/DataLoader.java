package com.flatox.backend.config;

import com.flatox.backend.entity.*;
import com.flatox.backend.enums.*;
import com.flatox.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final ApartmentRepository apartmentRepository;
    private final FlatRepository flatRepository;
    private final UserRepository userRepository;
    private final ReserveFundRepository reserveFundRepository;
    private final BillingTemplateRepository templateRepository;
    private final MaintenanceBillRepository billRepository;
    private final AccountingLedgerRepository ledgerRepository;
    private final BudgetPlanRepository budgetPlanRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final VendorRepository vendorRepository;
    private final ExpenseRepository expenseRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("[DataLoader] Checking and seeding Flatox Finance tables...");

        // 1. Ensure Apartment exists
        Apartment apt;
        if (apartmentRepository.count() == 0) {
            apt = Apartment.builder()
                    .name("Skyline Apartments")
                    .address("Whitefield Main Road")
                    .city("Bengaluru")
                    .state("Karnataka")
                    .pincode("560066")
                    .build();
            apt = apartmentRepository.save(apt);
            System.out.println("[DataLoader] Created default apartment: " + apt.getName());
        } else {
            apt = apartmentRepository.findAll().get(0);
            System.out.println("[DataLoader] Using existing apartment: " + apt.getName() + " (ID: " + apt.getId() + ")");
        }

        // 2. Ensure Flats exist
        List<Flat> flatsInApt = flatRepository.findByApartmentId(apt.getId());
        Flat flat1, flat2, flat3;
        if (flatsInApt.isEmpty()) {
            flat1 = Flat.builder()
                    .blockName("Block C")
                    .flatNumber("402")
                    .status(FlatStatus.OCCUPIED)
                    .apartment(apt)
                    .build();
            flat1 = flatRepository.save(flat1);

            flat2 = Flat.builder()
                    .blockName("Block A")
                    .flatNumber("101")
                    .status(FlatStatus.OCCUPIED)
                    .apartment(apt)
                    .build();
            flat2 = flatRepository.save(flat2);

            flat3 = Flat.builder()
                    .blockName("Block B")
                    .flatNumber("205")
                    .status(FlatStatus.OCCUPIED)
                    .apartment(apt)
                    .build();
            flat3 = flatRepository.save(flat3);
            System.out.println("[DataLoader] Seeded default flats (C-402, A-101, B-205)");
        } else {
            flat1 = flatsInApt.get(0);
            flat2 = flatsInApt.size() > 1 ? flatsInApt.get(1) : flat1;
            flat3 = flatsInApt.size() > 2 ? flatsInApt.get(2) : flat2;
            System.out.println("[DataLoader] Using " + flatsInApt.size() + " existing flats.");
        }

        // 3. Ensure Default Users exist
        ensureUser("Secretary Smith", "9999999999", "secretary@flatox.com", Role.ADMIN, ApprovalStatus.APPROVED, apt, null, "Owner", "Residing");
        User arjun = ensureUser("Arjun Sharma", "8888888888", "arjun.v@community.com", Role.RESIDENT, ApprovalStatus.APPROVED, apt, flat1, "Owner", "Residing");
        ensureUser("Priya Sharma", "7777777777", "priya@gmail.com", Role.RESIDENT, ApprovalStatus.PENDING, apt, flat2, "Owner", "Residing");
        ensureUser("Rajesh Patel", "6666666666", "rajesh@patel.com", Role.RESIDENT, ApprovalStatus.REJECTED, apt, flat3, "Tenant", null);

        // 4. Ensure Reserve Funds exist
        List<ReserveFund> reserves = reserveFundRepository.findByApartmentId(apt.getId());
        if (reserves.isEmpty()) {
            for (ReserveFundType fundType : ReserveFundType.values()) {
                BigDecimal balance = BigDecimal.ZERO;
                if (fundType == ReserveFundType.CORPUS) {
                    balance = new BigDecimal("245000.00");
                } else if (fundType == ReserveFundType.SINKING) {
                    balance = new BigDecimal("120000.00");
                } else if (fundType == ReserveFundType.EMERGENCY) {
                    balance = new BigDecimal("50000.00");
                } else if (fundType == ReserveFundType.MAINTENANCE_RESERVE) {
                    balance = new BigDecimal("85500.00");
                }

                ReserveFund fund = ReserveFund.builder()
                        .apartment(apt)
                        .fundType(fundType)
                        .balance(balance)
                        .lastUpdated(LocalDateTime.now())
                        .build();
                reserveFundRepository.save(fund);
            }
            System.out.println("[DataLoader] Seeded default reserve fund balances");
        }

        // 5. Ensure Billing Templates exist
        List<BillingTemplate> templates = templateRepository.findByApartmentId(apt.getId());
        BillingTemplate temp1, temp2;
        if (templates.isEmpty()) {
            temp1 = BillingTemplate.builder()
                    .apartment(apt)
                    .title("Monthly Maintenance Charge")
                    .amount(new BigDecimal("2500.00"))
                    .frequency(BillFrequency.MONTHLY)
                    .dueDayOfMonth(5)
                    .gracePeriodDays(5)
                    .lateFeePenalty(new BigDecimal("150.00"))
                    .isActive(true)
                    .build();
            temp1.getItems().add(BillingTemplateItem.builder().template(temp1).label("Maintenance").amount(new BigDecimal("1800.00")).build());
            temp1.getItems().add(BillingTemplateItem.builder().template(temp1).label("Water Charges").amount(new BigDecimal("300.00")).build());
            temp1.getItems().add(BillingTemplateItem.builder().template(temp1).label("Parking").amount(new BigDecimal("400.00")).build());
            temp1 = templateRepository.save(temp1);

            temp2 = BillingTemplate.builder()
                    .apartment(apt)
                    .title("Annual Sinking Fund Contribution")
                    .amount(new BigDecimal("1200.00"))
                    .frequency(BillFrequency.ANNUALLY)
                    .dueDayOfMonth(15)
                    .gracePeriodDays(10)
                    .lateFeePenalty(new BigDecimal("200.00"))
                    .isActive(true)
                    .build();
            temp2.getItems().add(BillingTemplateItem.builder().template(temp2).label("Sinking Fund").amount(new BigDecimal("1200.00")).build());
            temp2 = templateRepository.save(temp2);
            System.out.println("[DataLoader] Seeded default billing templates");
        } else {
            temp1 = templates.get(0);
            temp2 = templates.size() > 1 ? templates.get(1) : temp1;
        }

        // 6. Ensure Maintenance Bills exist
        List<MaintenanceBill> bills = billRepository.findByApartmentId(apt.getId());
        if (bills.isEmpty()) {
            MaintenanceBill bill1 = MaintenanceBill.builder()
                    .apartment(apt)
                    .flat(flat1)
                    .billingTemplate(temp1)
                    .title("Monthly Maintenance - August 2026")
                    .baseAmount(new BigDecimal("2500.00"))
                    .penaltyAmount(BigDecimal.ZERO)
                    .paidAmount(BigDecimal.ZERO)
                    .dueDate(LocalDate.of(2026, 8, 5))
                    .status(BillStatus.UNPAID)
                    .build();
            bill1.getItems().add(MaintenanceBillItem.builder().bill(bill1).label("Maintenance").amount(new BigDecimal("1800.00")).build());
            bill1.getItems().add(MaintenanceBillItem.builder().bill(bill1).label("Water Charges").amount(new BigDecimal("300.00")).build());
            bill1.getItems().add(MaintenanceBillItem.builder().bill(bill1).label("Parking").amount(new BigDecimal("400.00")).build());
            billRepository.save(bill1);

            MaintenanceBill bill2 = MaintenanceBill.builder()
                    .apartment(apt)
                    .flat(flat1)
                    .billingTemplate(temp2)
                    .title("Sinking Fund contribution - 2026")
                    .baseAmount(new BigDecimal("1200.00"))
                    .penaltyAmount(BigDecimal.ZERO)
                    .paidAmount(BigDecimal.ZERO)
                    .dueDate(LocalDate.of(2026, 6, 15))
                    .status(BillStatus.UNPAID)
                    .build();
            bill2.getItems().add(MaintenanceBillItem.builder().bill(bill2).label("Sinking Fund").amount(new BigDecimal("1200.00")).build());
            billRepository.save(bill2);
            System.out.println("[DataLoader] Seeded sample unpaid bills for Flat ID: " + flat1.getId());
        }

        // 7. Ensure Ledger Entries exist
        List<AccountingLedger> ledger = ledgerRepository.findByApartmentId(apt.getId());
        if (ledger.isEmpty()) {
            AccountingLedger entry1 = AccountingLedger.builder()
                    .apartment(apt)
                    .entryType(LedgerEntryType.CREDIT)
                    .accountName("CASH")
                    .amount(new BigDecimal("2500.00"))
                    .description("Maintenance payment Flat C-402 (Arjun Sharma)")
                    .createdAt(LocalDateTime.of(2026, 5, 4, 10, 0, 0))
                    .systemHash("a57fe7d890b12bc12bc12bc12bc12bc12bc12bc12bc12bc12bc12bc12bc1fbc2")
                    .build();
            ledgerRepository.save(entry1);

            AccountingLedger entry2 = AccountingLedger.builder()
                    .apartment(apt)
                    .entryType(LedgerEntryType.DEBIT)
                    .accountName("SECURITY_PAYABLE")
                    .amount(new BigDecimal("45000.00"))
                    .description("Security Salary payout for May 2026")
                    .createdAt(LocalDateTime.of(2026, 5, 15, 15, 30, 0))
                    .systemHash("8b21c43ef10228a228a228a228a228a228a228a228a228a228a228a228a228a")
                    .build();
            ledgerRepository.save(entry2);
            System.out.println("[DataLoader] Seeded baseline ledger entries");
        }

        // 8. Ensure Budget Plans exist
        List<BudgetPlan> budgets = budgetPlanRepository.findByApartmentIdOrderByIdDesc(apt.getId());
        BudgetPlan budget;
        if (budgets.isEmpty()) {
            budget = BudgetPlan.builder()
                    .apartment(apt)
                    .fiscalYearStart(LocalDate.of(2026, 4, 1))
                    .fiscalYearEnd(LocalDate.of(2027, 3, 31))
                    .totalEstimatedRevenue(new BigDecimal("540000.00"))
                    .totalAllocatedExpense(new BigDecimal("380000.00"))
                    .build();
            budget = budgetPlanRepository.save(budget);

            BudgetItem item1 = BudgetItem.builder()
                    .budgetPlan(budget)
                    .category("SECURITY")
                    .allocatedAmount(new BigDecimal("150000.00"))
                    .actualSpending(new BigDecimal("45000.00"))
                    .build();
            item1 = budgetItemRepository.save(item1);

            BudgetItem item2 = BudgetItem.builder()
                    .budgetPlan(budget)
                    .category("UTILITIES")
                    .allocatedAmount(new BigDecimal("100000.00"))
                    .actualSpending(new BigDecimal("12000.00"))
                    .build();
            item2 = budgetItemRepository.save(item2);

            BudgetItem item3 = BudgetItem.builder()
                    .budgetPlan(budget)
                    .category("REPAIRS")
                    .allocatedAmount(new BigDecimal("80000.00"))
                    .actualSpending(BigDecimal.ZERO)
                    .build();
            budgetItemRepository.save(item3);

            BudgetItem item4 = BudgetItem.builder()
                    .budgetPlan(budget)
                    .category("GENERAL")
                    .allocatedAmount(new BigDecimal("50000.00"))
                    .actualSpending(BigDecimal.ZERO)
                    .build();
            budgetItemRepository.save(item4);
            System.out.println("[DataLoader] Seeded default fiscal budget plan & items");
        } else {
            budget = budgets.get(0);
        }

        // 9. Ensure Vendors exist
        List<Vendor> vList = vendorRepository.findByApartmentId(apt.getId());
        Vendor vendor1, vendor2;
        if (vList.isEmpty()) {
            vendor1 = Vendor.builder()
                    .apartment(apt)
                    .companyName("Sharp Security Services")
                    .contactPerson("Inspector Bannerjee")
                    .email("contact@sharpsec.com")
                    .phone("9845012345")
                    .build();
            vendor1 = vendorRepository.save(vendor1);

            vendor2 = Vendor.builder()
                    .apartment(apt)
                    .companyName("Everclear Waters & Cleaning")
                    .contactPerson("Ramesh Kumar")
                    .email("sales@everclear.in")
                    .phone("9880123456")
                    .build();
            vendor2 = vendorRepository.save(vendor2);
            System.out.println("[DataLoader] Seeded default vendor directory");
        } else {
            vendor1 = vList.get(0);
            vendor2 = vList.size() > 1 ? vList.get(1) : vendor1;
        }

        // 10. Ensure Expenses exist
        List<Expense> expList = expenseRepository.findByApartmentId(apt.getId());
        if (expList.isEmpty()) {
            Expense exp1 = Expense.builder()
                    .apartment(apt)
                    .vendor(vendor1)
                    .category("SECURITY")
                    .amount(new BigDecimal("45000.00"))
                    .expenseDate(LocalDate.of(2026, 6, 15))
                    .description("Security guards monthly wage settlement")
                    .build();
            expenseRepository.save(exp1);

            Expense exp2 = Expense.builder()
                    .apartment(apt)
                    .vendor(vendor2)
                    .category("UTILITIES")
                    .amount(new BigDecimal("12000.00"))
                    .expenseDate(LocalDate.of(2026, 6, 12))
                    .description("Borewell cleaning & chlorine wash")
                    .build();
            expenseRepository.save(exp2);
            System.out.println("[DataLoader] Seeded default expense logs");
        }

        System.out.println("[DataLoader] Seeding execution check complete.");
    }

    private User ensureUser(String fullName, String phone, String email, Role role, ApprovalStatus status, Apartment apt, Flat flat, String userType, String ownerStatus) {
        Optional<User> existing = userRepository.findByPhone(phone);
        if (existing.isEmpty()) {
            User user = User.builder()
                    .fullName(fullName)
                    .phone(phone)
                    .email(email)
                    .role(role)
                    .approvalStatus(status)
                    .apartment(apt)
                    .flat(flat)
                    .userType(userType)
                    .ownerStatus(ownerStatus)
                    .build();
            user = userRepository.save(user);
            System.out.println("[DataLoader] Seeded user: " + fullName + " (" + phone + ")");
            return user;
        }
        return existing.get();
    }
}
