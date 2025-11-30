package com.example.yadavsamaj.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.yadavsamaj.model.WalletTransaction;

public interface WalletRepository extends JpaRepository<WalletTransaction, Long> {}
