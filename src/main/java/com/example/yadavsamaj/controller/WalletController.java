package com.example.yadavsamaj.controller;

import com.example.yadavsamaj.model.WalletTransaction;
import com.example.yadavsamaj.service.WalletService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class WalletController {

    private final WalletService service;

    @GetMapping
    public List<WalletTransaction> getAll() {
        return service.getAll();
    }

    @GetMapping("/balance")
    public Map<String, Object> getBalance() {
        return Map.of("totalBalance", service.getTotalBalance());
    }

    @PostMapping
    public WalletTransaction addTransaction(@RequestBody WalletTransaction tx) {
        return service.addTransaction(tx);
    }
}
