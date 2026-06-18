package com.flatox.backend.controller;

import com.flatox.backend.dto.request.RegisterRequest;
import com.flatox.backend.dto.request.SendOtpRequest;
import com.flatox.backend.dto.request.VerifyOtpRequest;
import com.flatox.backend.dto.response.AuthResponse;
import com.flatox.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor

@CrossOrigin("*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(
            @RequestBody RegisterRequest request
    ) {
        return authService.register(request);
    }

    @PostMapping("/send-otp")
    public AuthResponse sendOtp(
            @RequestBody SendOtpRequest request
    ) {
        return authService.sendOtp(request);
    }

    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(
            @RequestBody VerifyOtpRequest request
    ) {
        return authService.verifyOtp(request);
    }

    @GetMapping("/check-phone")
    public java.util.Map<String, Boolean> checkPhone(@RequestParam String phone) {
        boolean exists = authService.checkPhoneExists(phone);
        return java.util.Map.of("exists", exists);
    }

    @GetMapping("/check-email")
    public java.util.Map<String, Boolean> checkEmail(@RequestParam String email) {
        boolean exists = authService.checkEmailExists(email);
        return java.util.Map.of("exists", exists);
    }

    @GetMapping("/status")
    public java.util.Map<String, String> getStatus(java.security.Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }
        return authService.getStatus(principal.getName());
    }
}