package com.banka.accountservice.endpoint;

import com.banka.accountservice.exception.AccountNotFoundException;
import com.banka.accountservice.generated.*;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import javax.wsdl.OperationType;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Endpoint
public class AccountEndpoint {

    private static final String NAMESPACE_URI = "http://banka.com/accountservice";

    // Hafızada tutulan basit hesap kaydı - kendi tanımladığımız iç sınıf
    private static class Account {
        String ownerName;
        BigDecimal balance;
        CurrencyType currency;

        Account(String ownerName, BigDecimal balance, CurrencyType currency) {
            this.ownerName = ownerName;
            this.balance = balance;
            this.currency = currency;
        }
    }

    // Hesap numarası -> Account eşlemesi tutan "veritabanı"
    private final Map<String, Account> accounts = new HashMap<>();

    // Endpoint metodları birazdan buraya gelecek

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getAccountDetailsRequest")
    @ResponsePayload
    public GetAccountDetailsResponse getAccountDetails(@RequestPayload GetAccountDetailsRequest request) {
           Account account = accounts.get(request.getAccountNumber());

           GetAccountDetailsResponse response = new GetAccountDetailsResponse();

           if (account == null) {
               throw new AccountNotFoundException(request.getAccountNumber());
           }

           response.setAccountNumber(request.getAccountNumber());
           response.setOwnerName(account.ownerName);
           response.setBalance(account.balance);
           response.setCurrency(account.currency);

           return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "createAccountRequest")
    @ResponsePayload
    public CreateAccountResponse createAccount(@RequestPayload CreateAccountRequest request) {
        String newAccountNumber = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Account newAccount = new Account(
                request.getOwnerName(),
                request.getInitialBalance(),
                request.getCurrency()
        );

        accounts.put(newAccountNumber, newAccount);

        CreateAccountResponse response = new CreateAccountResponse();
        response.setAccountNumber(newAccountNumber);
        response.setStatus("SUCCESS");

        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "updateBalanceRequest")
    @ResponsePayload
    public UpdateBalanceResponse updateBalance(@RequestPayload UpdateBalanceRequest request) {
        Account account = accounts.get(request.getAccountNumber());

        if (account == null) {
            throw new AccountNotFoundException(request.getAccountNumber());
        }

        if (request.getOperationType() == com.banka.accountservice.generated.OperationType.DEBIT) {
            account.balance = account.balance.subtract(request.getAmount());
        } else {
            account.balance = account.balance.add(request.getAmount());
        }

        UpdateBalanceResponse response = new UpdateBalanceResponse();
        response.setAccountNumber(request.getAccountNumber());
        response.setNewBalance(account.balance);
        response.setStatus("SUCCESS");

        return response;
    }
}