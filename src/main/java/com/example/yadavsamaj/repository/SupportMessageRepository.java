package com.example.yadavsamaj.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.yadavsamaj.model.SupportMessage;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
}
