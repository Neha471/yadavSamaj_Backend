package com.example.yadavsamaj.controller;

import com.example.yadavsamaj.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> req) {
        try {
            String emailOrMobile = req.get("emailOrMobile");
            String otp = adminService.generateOtp(emailOrMobile);
            return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> req) {
        String emailOrMobile = req.get("emailOrMobile");
        String otp = req.get("otp");

        boolean isValid = adminService.verifyOtp(emailOrMobile, otp);
        if (isValid) {
            return ResponseEntity.ok(Map.of("message", "Login successful"));
        } else {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid or expired OTP"));
        }
    }
}
