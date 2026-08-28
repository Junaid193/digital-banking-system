package com.banking.accountservice.service;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatus;
import com.banking.accountservice.entity.AccountType;
import com.banking.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private static SecureRandom secureRandom = new SecureRandom();

    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating account for : {}", request.getEmail());

        if(accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("account already exists by email : "+ request.getEmail());
        }

        Account account = new Account();
        account.setAccountHolderName(request.getAccountHolderName());
        account.setEmail(request.getEmail());
        account.setPhone(request.getPhone());
        account.setAccountType(request.getAccountType());
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setBalance(request.getInitialDeposit());
        account.setAccountNumber(generateAccountNumber());
        account.setDailyTransactionLimit(
                request.getAccountType() == AccountType.SAVINGS
                ? new BigDecimal("100000")
                : new BigDecimal("500000")
        );

        Account savedAccount = accountRepository.save(account);
        log.info("Account created : {}", savedAccount.getAccountNumber());

        return mapToResponse(savedAccount);
    }

    public AccountResponse getAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("account not found "));

        return mapToResponse(account);
    }

    public BigDecimal getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("account not found "));

        return account.getBalance();
    }

    // block account - called by fraud detection service via kafka
    //@param accountNumber
    public void blockAccount(String accountNumber) {
        log.info("blocking account : {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("account not found "));
        account.setAccountStatus(AccountStatus.BLOCKED);
        log.info("Account blocked : {}", accountNumber);
    }

    public void deductBalance(String accountNumber, BigDecimal amount) {
        log.info("Deducting amount {} from account : {}", amount,accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("account not found "));

        if(account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("account is not ACTIVE : " + accountNumber);
        }

        if(account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("insufficient fund for account : " + accountNumber);
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        log.info("balance updated and new balance : {}", account.getBalance());
    }

    public void creditBalance(String accountNumber, BigDecimal amount) {
        log.info("crediting {} to account : {}", amount, accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("account not found "));

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        log.info("Balance credited new Balance : {}", account.getBalance());

    }

    //generate unique 12 digit account number
    private String generateAccountNumber(){

        String accountNumber;

        do{
            long number = secureRandom.nextLong(1_000_000_000_000L);
            accountNumber = String.format("%012d", number);
        }while(accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }

    private AccountResponse mapToResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setAccountNumber(account.getAccountNumber());
        response.setAccountHolderName(account.getAccountHolderName());
        response.setEmail(account.getEmail());
        response.setPhone(account.getPhone());
        response.setAccountType(account.getAccountType());
        response.setAccountStatus(account.getAccountStatus());
        response.setBalance(account.getBalance());
        response.setDailyTransactionLimit(account.getDailyTransactionLimit());
        response.setCreatedAt(account.getCreatedAt());

        return response;
    }

}
