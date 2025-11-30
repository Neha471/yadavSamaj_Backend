package com.example.yadavsamaj.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.yadavsamaj.model.PaymentRecord;

import java.util.Optional;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    Optional<PaymentRecord> findByRazorpayOrderId(String orderId);
}
