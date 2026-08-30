package com.banking.transactionservice.repository;

import com.banking.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRespository extends JpaRepository<Transaction, String> {

    List<Transaction> findBySenderAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
