package com.example.yadavsamaj.service;

import com.example.yadavsamaj.model.Admin;
import com.example.yadavsamaj.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;

    // Generate OTP and save
    public String generateOtp(String emailOrMobile) {
        Optional<Admin> adminOpt = emailOrMobile.contains("@")
                ? adminRepository.findByEmail(emailOrMobile)
                : adminRepository.findByMobile(emailOrMobile);

        if (adminOpt.isEmpty()) {
            throw new RuntimeException("Admin not found");
        }

        Admin admin = adminOpt.get();
        String otp = String.valueOf(100000 + new Random().nextInt(900000)); // 6-digit OTP
        admin.setOtp(otp);
        admin.setOtpGeneratedTime(LocalDateTime.now());
        adminRepository.save(admin);

        // Here you can send OTP via Email or SMS
        System.out.println("OTP for admin: " + otp);

        return otp;
    }

    // Verify OTP
    public boolean verifyOtp(String emailOrMobile, String otp) {
        Optional<Admin> adminOpt = emailOrMobile.contains("@")
                ? adminRepository.findByEmail(emailOrMobile)
                : adminRepository.findByMobile(emailOrMobile);

        if (adminOpt.isEmpty()) return false;

        Admin admin = adminOpt.get();
        if (admin.getOtp() == null) return false;

        // OTP valid for 5 minutes
        boolean isOtpValid = admin.getOtp().equals(otp)
                && admin.getOtpGeneratedTime().isAfter(LocalDateTime.now().minusMinutes(5));

        if (isOtpValid) {
            admin.setOtp(null); // clear OTP
            adminRepository.save(admin);
        }

        return isOtpValid;
    }
}
