package com.banka.accountservice.endpoint;

import com.banka.accountservice.entity.AccountEntity;
import com.banka.accountservice.exception.AccountNotFoundException;
import com.banka.accountservice.generated.*;
import com.banka.accountservice.repository.AccountRepository;
import com.banka.accountservice.service.AccountCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.util.UUID;

@Endpoint
public class AccountEndpoint {

    private static final String NAMESPACE_URI = "http://banka.com/accountservice";

    private final AccountRepository accountRepository;
    private final AccountCacheService accountCacheService;

    @Autowired
    public AccountEndpoint(AccountRepository accountRepository, AccountCacheService accountCacheService) {
        this.accountRepository = accountRepository;
        this.accountCacheService = accountCacheService;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getAccountDetailsRequest")
    @ResponsePayload
    public GetAccountDetailsResponse getAccountDetails(@RequestPayload GetAccountDetailsRequest request) {
        AccountEntity account = accountCacheService.findAccountCached(request.getAccountNumber());

        GetAccountDetailsResponse response = new GetAccountDetailsResponse();
        response.setAccountNumber(account.getAccountNumber());
        response.setOwnerName(account.getOwnerName());
        response.setBalance(account.getBalance());
        response.setCurrency(CurrencyType.valueOf(account.getCurrency().name()));

        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "createAccountRequest")
    @ResponsePayload
    public CreateAccountResponse createAccount(@RequestPayload CreateAccountRequest request) {
        String newAccountNumber = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        AccountEntity newAccount = new AccountEntity(
                newAccountNumber,
                request.getOwnerName(),
                request.getInitialBalance(),
                AccountEntity.Currency.valueOf(request.getCurrency().value())
        );

        accountRepository.save(newAccount);

        CreateAccountResponse response = new CreateAccountResponse();
        response.setAccountNumber(newAccountNumber);
        response.setStatus("SUCCESS");

        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "updateBalanceRequest")
    @ResponsePayload
    @CacheEvict(value = "accounts", key = "#request.accountNumber")
    public UpdateBalanceResponse updateBalance(@RequestPayload UpdateBalanceRequest request) {
        AccountEntity account = accountRepository.findById(request.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(request.getAccountNumber()));

        if (request.getOperationType() == com.banka.accountservice.generated.OperationType.DEBIT) {
            account.setBalance(account.getBalance().subtract(request.getAmount()));
        } else {
            account.setBalance(account.getBalance().add(request.getAmount()));
        }

        accountRepository.save(account);

        UpdateBalanceResponse response = new UpdateBalanceResponse();
        response.setAccountNumber(account.getAccountNumber());
        response.setNewBalance(account.getBalance());
        response.setStatus("SUCCESS");

        return response;
    }
}