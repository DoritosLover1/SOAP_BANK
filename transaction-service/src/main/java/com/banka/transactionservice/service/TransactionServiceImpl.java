package com.banka.transactionservice.service;

import com.banka.transactionservice.client.account.AccountServicePort;
import com.banka.transactionservice.client.account.AccountServicePortService;
import com.banka.transactionservice.client.account.GetAccountDetailsRequest;
import com.banka.transactionservice.client.account.GetAccountDetailsResponse;
import com.banka.transactionservice.client.account.UpdateBalanceRequest;

import jakarta.jws.WebService;

import java.math.BigDecimal;

@WebService(
        endpointInterface = "com.banka.transactionservice.service.TransactionService",
        targetNamespace = "http://banka.com/transactionservice"
)
public class TransactionServiceImpl implements TransactionService {

    @Override
    public TransferResult transferMoney(String fromAccount, String toAccount, double amount) {

        if (amount <= 0) {
            return new TransferResult("FAILED", null, "Transfer tutari sifirdan buyuk olmalidir");
        }

        AccountServicePortService service = new AccountServicePortService();
        AccountServicePort accountPort = service.getAccountServicePortSoap11();

        // 1. Gonderen hesabi sorgula
        GetAccountDetailsResponse fromAccountDetails;
        try {
            GetAccountDetailsRequest request = new GetAccountDetailsRequest();
            request.setAccountNumber(fromAccount);
            fromAccountDetails = accountPort.getAccountDetails(request);
        } catch (Exception e) {
            return new TransferResult("FAILED", null, "We cannot find sender account: " + fromAccount);
        }

        // 2. Alici hesabi sorgula
        GetAccountDetailsResponse toAccountDetails;
        try {
            GetAccountDetailsRequest toRequest = new GetAccountDetailsRequest();
            toRequest.setAccountNumber(toAccount);
            toAccountDetails = accountPort.getAccountDetails(toRequest);
        } catch (Exception e) {
            return new TransferResult("FAILED", null, "We cannot find receiver account: " + toAccount);
        }

        // 3. Para birimi kontrolu - iki hesap da ayni para biriminde olmali
        if (fromAccountDetails.getCurrency() != toAccountDetails.getCurrency()) {
            return new TransferResult("FAILED", null,
                    "Currency mismatch: sender uses " + fromAccountDetails.getCurrency()
                            + ", receiver uses " + toAccountDetails.getCurrency());
        }

        // 4. Bakiye yeterli mi kontrol et
        BigDecimal currentBalance = fromAccountDetails.getBalance();
        BigDecimal transferAmount = BigDecimal.valueOf(amount);

        if (currentBalance.compareTo(transferAmount) < 0) {
            return new TransferResult("FAILED", null,
                    "Current balance is not sufficient for this transaction: " + currentBalance + " " + fromAccountDetails.getCurrency());
        }

        // 5. Gonderenden dus (DEBIT)
        try {
            UpdateBalanceRequest debitRequest = new UpdateBalanceRequest();
            debitRequest.setAccountNumber(fromAccount);
            debitRequest.setAmount(transferAmount);
            debitRequest.setOperationType(com.banka.transactionservice.client.account.OperationType.DEBIT);
            accountPort.updateBalance(debitRequest);
        } catch (Exception e) {
            return new TransferResult("FAILED", null, "Failed to debit sender account: " + e.getMessage());
        }

        // 6. Aliciya ekle (CREDIT)
        try {
            UpdateBalanceRequest creditRequest = new UpdateBalanceRequest();
            creditRequest.setAccountNumber(toAccount);
            creditRequest.setAmount(transferAmount);
            creditRequest.setOperationType(com.banka.transactionservice.client.account.OperationType.CREDIT);
            accountPort.updateBalance(creditRequest);
        } catch (Exception e) {
            // KRITIK DURUM: gonderenden dusuldu ama aliciya eklenemedi
            // Gercek bir bankada burada "compensating transaction" (telafi islemi) gerekir
            return new TransferResult("FAILED", null,
                    "Critical: debited sender but failed to credit receiver. Manual intervention needed: " + e.getMessage());
        }

        String transactionId = "TXN-" + System.currentTimeMillis();
        System.out.println("Transfer tamamlandi: " + fromAccount + " -> " + toAccount
                + " tutar: " + transferAmount + " " + fromAccountDetails.getCurrency());

        return new TransferResult("SUCCESS", transactionId, "Transfer basariyla tamamlandi");
    }
}