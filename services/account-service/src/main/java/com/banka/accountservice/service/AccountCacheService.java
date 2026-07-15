package com.banka.accountservice.service;

import com.banka.accountservice.entity.AccountEntity;
import com.banka.accountservice.exception.AccountNotFoundException;
import com.banka.accountservice.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class AccountCacheService {

    private final AccountRepository accountRepository;

    @Autowired
    public AccountCacheService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Cacheable(value = "accounts", key = "#accountNumber")
    public AccountEntity findAccountCached(String accountNumber) {
        System.out.println(">>> VERITABANINDAN OKUNUYOR (cache miss): " + accountNumber);
        return accountRepository.findById(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }
}