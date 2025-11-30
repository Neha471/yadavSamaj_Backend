package com.example.yadavsamaj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.yadavsamaj.model.Donation;


@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {
	 Optional<Donation> findByPaymentId(String paymentId);
	  List<Donation> findByPaymentMethod(String method);
	    List<Donation> findByPaymentMethodNot(String method);
}
