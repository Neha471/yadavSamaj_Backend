package com.example.yadavsamaj.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.yadavsamaj.model.MembershipUser;

import java.util.Optional;

public interface MembershipUserRepository extends JpaRepository<MembershipUser, Long> {
    Optional<MembershipUser> findByPhone(String phone);
    
    
}
