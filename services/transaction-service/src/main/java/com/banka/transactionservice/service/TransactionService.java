package com.banka.transactionservice.service;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

import java.util.List;

@WebService
public interface TransactionService {

    @WebMethod
    TransferResult transferMoney(
            @WebParam(name = "fromAccount") String fromAccount,
            @WebParam(name = "toAccount") String toAccount,
            @WebParam(name = "amount") double amount
    );

    @WebMethod
    @WebResult(name = "transactionHistory")
    List<TransactionHistoryItem> getTransactionHistory(
            @WebParam(name = "accountNumber") String accountNumber
    );
}