package com.example.yadavsamaj.controller;

import com.example.yadavsamaj.model.MatrimonialUser;
import com.example.yadavsamaj.repository.MatrimonialUserRepository;
import com.example.yadavsamaj.service.MatrimonialUserService;
import com.example.yadavsamaj.service.OtpUtil;
import com.example.yadavsamaj.service.PaymentService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.IOException; // ✅ for IOException
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap; // ✅ for HashMap
import java.util.List; // ✅ for List
import java.util.Map; // ✅ for Map
import java.util.Optional; // ✅ for Optional
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/matrimonial")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class MatrimonialUserController {

    private final MatrimonialUserService matrimonialUserService;
    private final MatrimonialUserRepository repository;
    private final String UPLOAD_DIR = "/var/www/angular/uploads/matrimonial/";
    @Autowired
    private PaymentService paymentService;

    private static final long OTP_EXPIRATION_MILLIS = 5 * 60 * 1000; // 5 minutes

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> register(
            @RequestPart("user") String userJson,
            @RequestPart(value = "photo", required = false) MultipartFile photoFile) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            MatrimonialUser user = mapper.readValue(userJson, MatrimonialUser.class);

            // ------------------ CHECK IS YADAV ------------------
            if (user.getIsYadav() == null || !user.getIsYadav()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "You must confirm you are Yadav to register. Registration denied."));
            }

            if (user.getMembershipPlan() == null || user.getMembershipPlan().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Please select a membership plan."));
            }

            // Set membership amount & expiry
            switch (user.getMembershipPlan()) {
                case "1_YEAR" -> {
                    user.setMembershipAmount(99);
                    user.setMembershipExpiryDate(LocalDate.now().plusYears(1));
                }
                case "LIFETIME" -> {
                    user.setMembershipAmount(499);
                    user.setMembershipExpiryDate(null);
                }
                default -> {
                    return ResponseEntity.badRequest().body(Map.of("message", "Invalid membership plan selected."));
                }
            }

            // Initial status
            user.setStatus("PENDING");
            user.setApproved(false);
            user.setPaymentDone(false);

            MatrimonialUser savedUser = matrimonialUserService.register(user, photoFile, UPLOAD_DIR);

            // Redirect user to payment immediately
            return ResponseEntity.ok(Map.of(
                    "message", "Registered successfully. Proceed to payment.",
                    "user", savedUser,
                    "amount", savedUser.getMembershipAmount()));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Registration failed: " + e.getMessage()));
        }
    }

    @GetMapping("/send-otp/{phone}")
    public ResponseEntity<?> sendOtp(@PathVariable String phone) {
        Optional<MatrimonialUser> userOpt = matrimonialUserService.findByPhone(phone);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "User not found"));
        }

        try {
            String otp = matrimonialUserService.generateOtp(userOpt.get());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "otp", otp,
                    "message", "OTP generated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // MatrimonialUserController.java
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> payload) {
        String phone = payload.get("phone");
        String otp = payload.get("otp");

        MatrimonialUser user = matrimonialUserService.findByPhone(phone)
                .orElse(null);

        if (user == null)
            return ResponseEntity.ok(Map.of("verified", false, "message", "User not registered"));

        if (!otp.equals(user.getCurrentOtp())) {
            return ResponseEntity.ok(Map.of("verified", false, "message", "Invalid OTP"));
        }

        // Check if payment done
        if (!Boolean.TRUE.equals(user.getPaymentDone())) {
            return ResponseEntity.ok(Map.of("verified", false, "message", "Please complete your payment first"));
        }

        // Check if admin rejected
        if ("REJECTED".equalsIgnoreCase(user.getApprovalStatus())) {
            return ResponseEntity.ok(Map.of("verified", false, "message", "You are rejected by admin"));
        }

        // Approved or pending (active)
        Map<String, Object> response = new HashMap<>();
        response.put("verified", true);
        response.put("message", "Login successful");
        response.put("status", user.getStatus());
        response.put("approved", user.getApproved());
        response.put("role", user.getRole());
        response.put("user", user);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingAndPaymentPendingUsers() {
        List<MatrimonialUser> pendingUsers = matrimonialUserService.getPendingUsers();
        List<MatrimonialUser> approvedButNotPaid = matrimonialUserService.getApprovedButNotPaidUsers();

        // Map pending users
        List<Map<String, Object>> pendingMapped = pendingUsers.stream()
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("fullName", u.getFullName());
                    m.put("phone", u.getPhone());
                    m.put("email", u.getEmail());
                    m.put("type", "pending");
                    m.put("paymentPending", false);
                    return m;
                })
                .collect(Collectors.toList());

        // Map approved but not paid users
        List<Map<String, Object>> paymentPendingMapped = approvedButNotPaid.stream()
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("fullName", u.getFullName());
                    m.put("phone", u.getPhone());
                    m.put("email", u.getEmail());
                    m.put("type", "approved");
                    m.put("paymentPending", true);
                    return m;
                })
                .collect(Collectors.toList());

        // Combine both lists
        List<Map<String, Object>> result = new ArrayList<>();
        result.addAll(pendingMapped);
        result.addAll(paymentPendingMapped);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/all")
    public List<MatrimonialUser> getAllUsers() {
        return matrimonialUserService.getAllUsers();
    }

    @GetMapping("/details/{phone}")
    public ResponseEntity<?> getUserDetails(@PathVariable String phone) {
        Optional<MatrimonialUser> optionalUser = matrimonialUserService.findByPhone(phone);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }
        return ResponseEntity.ok(optionalUser.get());
    }

    @GetMapping("/status/{phone}")
    public ResponseEntity<?> getStatus(@PathVariable String phone) {
        Optional<MatrimonialUser> optionalUser = matrimonialUserService.findByPhone(phone);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.ok(Map.of("found", false));
        }

        MatrimonialUser user = optionalUser.get();

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("found", true);
        response.put("status", user.getStatus() != null ? user.getStatus() : "PENDING");
        response.put("approved", user.getApproved() != null ? user.getApproved() : false);
        response.put("paymentDone", user.getPaymentDone() != null ? user.getPaymentDone() : false);
        response.put("membershipPlan", user.getMembershipPlan() != null ? user.getMembershipPlan() : "NONE");
        response.put("fullName", user.getFullName());

        return ResponseEntity.ok(response);
    }

    // MatrimonialUserController.java
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestPart("user") MatrimonialUser user,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {

        Optional<MatrimonialUser> optionalUser = matrimonialUserService.findById(id);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }

        MatrimonialUser existingUser = optionalUser.get();

        // Update fields
        existingUser.setFullName(user.getFullName());
        existingUser.setEmail(user.getEmail());
        existingUser.setDob(user.getDob());
        existingUser.setGender(user.getGender());
        existingUser.setEducation(user.getEducation());
        existingUser.setProfession(user.getProfession());
        existingUser.setAbout(user.getAbout());
        existingUser.setPhone(user.getPhone());
        existingUser.setFatherName(user.getFatherName());
        existingUser.setMotherName(user.getMotherName());
        existingUser.setBrothers(user.getBrothers());
        existingUser.setSisters(user.getSisters());
        existingUser.setAddress(user.getAddress());
        existingUser.setCity(user.getCity());
        existingUser.setState(user.getState());
        existingUser.setCountry(user.getCountry());
        existingUser.setMaritalStatus(user.getMaritalStatus());
        existingUser.setMotherTongue(user.getMotherTongue());
        existingUser.setHobbies(user.getHobbies());
        // existingUser.setReligion(user.getReligion());
        existingUser.setCaste(user.getCaste());
        existingUser.setDiet(user.getDiet());
        existingUser.setLifestyle(user.getLifestyle());
        existingUser.setHeight(user.getHeight());
        existingUser.setWeight(user.getWeight());
        existingUser.setBloodGroup(user.getBloodGroup());

        // Handle photo upload
        if (photo != null && !photo.isEmpty()) {
            try {
                // Unique filename
                String filename = System.currentTimeMillis() + "_" + photo.getOriginalFilename();

                // Path object
                Path uploadPath = Paths.get(UPLOAD_DIR + filename);

                // Create directories if not exist
                Files.createDirectories(uploadPath.getParent());

                // Save file
                photo.transferTo(uploadPath.toFile());

                // Update user photo path
                existingUser.setPhotoFileName("/uploads/matrimonial/" + filename);

            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Error saving photo: " + e.getMessage()));
            }
        }

        // Save updated user
        MatrimonialUser updatedUser = matrimonialUserService.save(existingUser);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/approve/{id}")
    public void approve(@PathVariable Long id) {
        matrimonialUserService.approveUser(id);
    }

    @PostMapping("/reject/{id}")
    public ResponseEntity<?> rejectUser(@PathVariable Long id) {
        MatrimonialUser user = matrimonialUserService.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setApproved(false);
        user.setStatus("REJECTED");
        user.setApprovalStatus("REJECTED");
        matrimonialUserService.save(user);

        return ResponseEntity.ok(Map.of("message", "User rejected by admin"));
    }

    @PostMapping(value = "/finalize-registration", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> finalizeRegistration(
            @RequestPart("user") String userJson,
            @RequestPart(value = "photo", required = false) MultipartFile photoFile,
            @RequestParam("paymentId") String paymentId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            MatrimonialUser user = mapper.readValue(userJson, MatrimonialUser.class);

            // Mark payment success
            user.setPaymentDone(true);
            user.setPaymentId(paymentId);
            user.setStatus("ACTIVE");
            user.setApprovalStatus("PENDING");
            user.setApproved(false);

            MatrimonialUser savedUser = matrimonialUserService.register(user, photoFile, UPLOAD_DIR);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Registration completed successfully!",
                    "user", savedUser));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error finalizing registration: " + e.getMessage()));
        }
    }

    // ---------------- PAYMENT by phone ----------------

    @PostMapping("/payment-done")
    public ResponseEntity<?> paymentDone(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String paymentId = request.get("paymentId");

        MatrimonialUser user = matrimonialUserService.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPaymentDone(true);
        user.setPaymentId(paymentId);
        user.setStatus("ACTIVE"); // ✅ Only now user is active
        user.setPaymentStatus("SUCCESS");
        matrimonialUserService.save(user);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Payment successful. You can now log in.",
                "user", user));
    }

    // ---------------- Private OTP verification ----------------
    private boolean verifyOtpForUser(MatrimonialUser user, String otp) {
        if (user == null || user.getCurrentOtp() == null || user.getOtpGeneratedAtMillis() == null)
            return false;

        long now = System.currentTimeMillis();
        if (now - user.getOtpGeneratedAtMillis() > OTP_EXPIRATION_MILLIS)
            return false;

        return otp.equals(user.getCurrentOtp());
    }

}
