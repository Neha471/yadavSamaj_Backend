package com.example.yadavsamaj.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.yadavsamaj.model.HomeSection;
import com.example.yadavsamaj.model.HomeSection.SectionType;

import java.util.List;

public interface HomeSectionRepository extends JpaRepository<HomeSection, Long> {
	 List<HomeSection> findByType(HomeSection.SectionType type);
	    List<HomeSection> findByTypeAndActiveTrue(HomeSection.SectionType type);
	    List<HomeSection> findByType(String type);
	    
}
