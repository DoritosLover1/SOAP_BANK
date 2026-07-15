package com.banka.accountservice.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class AccountEntity implements Serializable {

    @Id
    private String accountNumber;

    private String ownerName;

    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    public enum Currency {
        TRY, USD, EUR
    }

    public AccountEntity() {}

    public AccountEntity(String accountNumber, String ownerName, BigDecimal balance, Currency currency) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = balance;
        this.currency = currency;
    }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
}