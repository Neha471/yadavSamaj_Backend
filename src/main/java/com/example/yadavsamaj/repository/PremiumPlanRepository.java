package com.example.yadavsamaj.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.yadavsamaj.model.PremiumPlan;

@Repository
public interface PremiumPlanRepository extends JpaRepository<PremiumPlan, Long> {}
