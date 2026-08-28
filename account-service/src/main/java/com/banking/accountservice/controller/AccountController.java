package com.banking.accountservice.controller;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount (
            @Valid @RequestBody CreateAccountRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(request));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount (
            @PathVariable String accountNumber){
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance (
            @PathVariable String accountNumber){
        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }

    @PutMapping("/{accountNumber}/block")
    public ResponseEntity<String> blockAccount(
            @PathVariable String accountNumber){
        accountService.blockAccount(accountNumber);
        return ResponseEntity.ok("Account Blocked Successfully");
    }

    /// SAGA STEP 1 - deduct balance
    /// called by transaction service when transfer is initiated

    @PutMapping("/{accountNumber}/deduct")
    public ResponseEntity<String> deductBalance (
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount){
        accountService.deductBalance(accountNumber, amount);
        return ResponseEntity.ok("Balance Deducted Successfully");
    }

    /// SAGA STEP 4 - compensating transaction endpoint
    /// Called by transaction service in 2 scenario
    /// 1. fraud detection -> refund sender(undo step 1)
    /// 2. transaction completed -> credit receiver


    @PutMapping("/{accountNumber}/credit")
    public ResponseEntity<String> creditBalance (
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount){
        accountService.creditBalance(accountNumber,amount);
        return ResponseEntity.ok("Balance Credited Successfully");
    }


    }
