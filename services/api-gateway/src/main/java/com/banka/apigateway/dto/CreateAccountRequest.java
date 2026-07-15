package com.banka.apigateway.dto;

import java.math.BigDecimal;

public class CreateAccountRequest {
    private String ownerName;
    private BigDecimal initialBalance;
    private String currency;

    public CreateAccountRequest() {}

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public BigDecimal getInitialBalance() { return initialBalance; }
    public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}