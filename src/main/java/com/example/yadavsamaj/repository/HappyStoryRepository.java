package com.example.yadavsamaj.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.yadavsamaj.model.HappyStory;

import java.util.List;

@Repository
public interface HappyStoryRepository extends JpaRepository<HappyStory, Long> {
   
}
