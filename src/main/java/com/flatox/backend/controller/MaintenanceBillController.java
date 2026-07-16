package com.flatox.backend.controller;

import com.flatox.backend.entity.MaintenanceBill;
import com.flatox.backend.entity.User;
import com.flatox.backend.repository.MaintenanceBillRepository;
import com.flatox.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/finance/bills")
@RequiredArgsConstructor
public class MaintenanceBillController {

    private final MaintenanceBillRepository billRepository;
    private final UserRepository userRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserBills(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOpt.get();
        if (user.getFlat() == null) {
            return ResponseEntity.badRequest().body("User is not assigned to any flat");
        }

        List<MaintenanceBill> bills = billRepository.findByFlatId(user.getFlat().getId());
        return ResponseEntity.ok(bills);
    }
}
