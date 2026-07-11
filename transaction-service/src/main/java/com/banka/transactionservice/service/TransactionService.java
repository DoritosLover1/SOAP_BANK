package com.banka.transactionservice.service;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

@WebService(targetNamespace = "http://banka.com/transactionservice")
public interface TransactionService {

    @WebMethod(operationName = "transferMoney")
    @WebResult(name = "transferResult")
    TransferResult transferMoney(
            @WebParam(name = "fromAccount") String fromAccount,
            @WebParam(name = "toAccount") String toAccount,
            @WebParam(name = "amount") double amount
    );
}