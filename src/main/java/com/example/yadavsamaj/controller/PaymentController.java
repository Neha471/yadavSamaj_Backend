package com.example.yadavsamaj.controller;

import com.example.yadavsamaj.model.MatrimonialUser;
import com.example.yadavsamaj.model.MembershipUser;
import com.example.yadavsamaj.service.MatrimonialUserService;
import com.example.yadavsamaj.service.MembershipUserService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentController {

    private final MatrimonialUserService matrimonialService;
    private final MembershipUserService membershipService;

    // ---------------- GET PAYMENT AMOUNT ----------------
    @GetMapping("/amount/{phone}")
    public ResponseEntity<?> getPaymentAmount(
            @PathVariable String phone,
            @RequestParam String userType) {

        if ("MATRIMONIAL".equalsIgnoreCase(userType)) {
            Optional<MatrimonialUser> userOpt = matrimonialService.findByPhone(phone);
            if (userOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "Matrimonial user not found"));

            MatrimonialUser user = userOpt.get();

            // ❌ Remove approval check here → allow payment immediately
            if (Boolean.TRUE.equals(user.getPaymentDone()))
                return ResponseEntity.status(403).body(Map.of("message", "Payment already completed"));

            String plan = user.getMembershipPlan().toUpperCase();
            int amount = switch (plan) {
                case "1_YEAR" -> 99;
                case "LIFETIME" -> 499;
                default -> 0;
            };

            return ResponseEntity.ok(Map.of(
                    "userType", "MATRIMONIAL",
                    "fullName", user.getFullName(),
                    "phone", user.getPhone(),
                    "membershipPlan", user.getMembershipPlan(),
                    "amount", amount,
                    "isYadav", user.getIsYadav() // send Yadav info for frontend
            ));
        }

        if ("MEMBERSHIP".equalsIgnoreCase(userType)) {
            Optional<MembershipUser> userOpt = membershipService.findByPhone(phone);
            if (userOpt.isEmpty())
                return ResponseEntity.status(404).body(Map.of("message", "Membership user not found"));

            MembershipUser user = userOpt.get();
            if (Boolean.TRUE.equals(user.getPaymentDone()))
                return ResponseEntity.status(403).body(Map.of("message", "Payment already completed"));

            int amount = user.getMembershipAmount() != null ? user.getMembershipAmount() : 0;

            return ResponseEntity.ok(Map.of(
                    "userType", "MEMBERSHIP",
                    "fullName", Optional.ofNullable(user.getFullName()).orElse(""),
                    "phone", user.getPhone(),
                    "membershipPlan", Optional.ofNullable(user.getMembershipPlan()).orElse(""),
                    "amount", amount
            ));
        }

        return ResponseEntity.badRequest().body(Map.of("message", "Invalid user type"));
    }

    // ---------------- MARK PAYMENT DONE ----------------
    @PostMapping("/mark-done")
    public ResponseEntity<?> markPaymentDone(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String paymentId = request.get("paymentId");
        String userType = request.get("userType");

        if (paymentId == null || paymentId.isBlank() || userType == null || userType.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid request"));
        }

        if ("MATRIMONIAL".equalsIgnoreCase(userType)) {
            Optional<MatrimonialUser> userOpt = matrimonialService.findByPhone(phone);
            if (userOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "Matrimonial user not found"));

            MatrimonialUser user = userOpt.get();

            if (Boolean.TRUE.equals(user.getPaymentDone()))
                return ResponseEntity.status(403).body(Map.of("message", "Payment already completed"));

            // ❌ Allow payment even if approved=false
            user.setPaymentDone(true);
            user.setPaymentId(paymentId);
            user.setStatus("PENDING_APPROVAL"); // default status until admin approves/rejects
            matrimonialService.save(user);

            return ResponseEntity.ok(Map.of("success", true, "message", "Payment successful for Matrimonial user: " + user.getFullName()));
        }

        if ("MEMBERSHIP".equalsIgnoreCase(userType)) {
            Optional<MembershipUser> userOpt = membershipService.findByPhone(phone);
            if (userOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "Membership user not found"));

            MembershipUser user = userOpt.get();
            if (Boolean.TRUE.equals(user.getPaymentDone()))
                return ResponseEntity.status(403).body(Map.of("message", "Payment already completed"));

            user.setPaymentDone(true);
            user.setPaymentId(paymentId);
            user.setStatus("PENDING_APPROVAL");
            membershipService.save(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment successful for Membership user: " + Optional.ofNullable(user.getFullName()).orElse("User")
            ));
        }

        return ResponseEntity.badRequest().body(Map.of("message", "Invalid user type"));
    }
}
