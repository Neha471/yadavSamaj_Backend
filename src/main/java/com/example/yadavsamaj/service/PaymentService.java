package com.example.yadavsamaj.service;

import com.example.yadavsamaj.model.PaymentRecord;
import com.example.yadavsamaj.repository.PaymentRecordRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRecordRepository paymentRepository;


    @Transactional
    public PaymentRecord savePayment(PaymentRecord payment) {
        return paymentRepository.save(payment);
    }

    public List<PaymentRecord> getAllPayments() {
        return paymentRepository.findAll();
    }

  
    public double getTotalPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(PaymentRecord::getAmount)
                .filter(amount -> amount != null)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    public Optional<PaymentRecord> findByOrderId(String orderId) {
        return paymentRepository.findByRazorpayOrderId(orderId);
    }

 
    public PaymentRecord findByOrderIdOrNull(String orderId) {
        return findByOrderId(orderId).orElse(null);
    }
}
