package com.banka.transactionservice.service;

import com.banka.transactionservice.client.account.AccountServicePort;
import com.banka.transactionservice.client.account.AccountServicePortService;
import com.banka.transactionservice.client.account.GetAccountDetailsRequest;
import com.banka.transactionservice.client.account.GetAccountDetailsResponse;
import com.banka.transactionservice.client.account.UpdateBalanceRequest;
import com.banka.transactionservice.entity.TransactionRecord;
import com.banka.transactionservice.repository.TransactionRepository;

import jakarta.jws.WebService;
import jakarta.xml.ws.BindingProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@WebService(
        endpointInterface = "com.banka.transactionservice.service.TransactionService",
        targetNamespace = "http://banka.com/transactionservice"
)
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Value("${account.service.wsdl-url:http://localhost:8081/ws/accounts.wsdl}")
    private String accountServiceWsdlUrl;

    @Value("${account.service.endpoint-url:http://localhost:8081/ws/accounts}")
    private String accountServiceEndpointUrl;

    @Autowired
    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    private AccountServicePort getAccountPort() {
        Thread.currentThread().setContextClassLoader(AccountServicePortService.class.getClassLoader());

        try {
            AccountServicePortService service = new AccountServicePortService(new URL(accountServiceWsdlUrl));
            AccountServicePort port = service.getAccountServicePortSoap11();
            ((BindingProvider) port).getRequestContext()
                    .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, accountServiceEndpointUrl);
            return port;
        } catch (MalformedURLException e) {
            throw new RuntimeException("account-service WSDL adresi geçersiz: " + accountServiceWsdlUrl, e);
        }
    }

    @Override
    public TransferResult transferMoney(String fromAccount, String toAccount, double amount) {

        if (amount <= 0) {
            return saveAndReturn("FAILED", null, fromAccount, toAccount, amount,
                    "Transfer tutari sifirdan buyuk olmalidir");
        }

        AccountServicePort accountPort = getAccountPort();

        GetAccountDetailsResponse fromAccountDetails;
        try {
            GetAccountDetailsRequest request = new GetAccountDetailsRequest();
            request.setAccountNumber(fromAccount);
            fromAccountDetails = accountPort.getAccountDetails(request);
        } catch (Exception e) {
            return saveAndReturn("FAILED", null, fromAccount, toAccount, amount,
                    "We cannot find sender account: " + fromAccount);
        }

        GetAccountDetailsResponse toAccountDetails;
        try {
            GetAccountDetailsRequest toRequest = new GetAccountDetailsRequest();
            toRequest.setAccountNumber(toAccount);
            toAccountDetails = accountPort.getAccountDetails(toRequest);
        } catch (Exception e) {
            return saveAndReturn("FAILED", null, fromAccount, toAccount, amount,
                    "We cannot find receiver account: " + toAccount);
        }

        if (fromAccountDetails.getCurrency() != toAccountDetails.getCurrency()) {
            return saveAndReturn("FAILED", null, fromAccount, toAccount, amount,
                    "Currency mismatch: sender uses " + fromAccountDetails.getCurrency()
                            + ", receiver uses " + toAccountDetails.getCurrency());
        }

        BigDecimal currentBalance = fromAccountDetails.getBalance();
        BigDecimal transferAmount = BigDecimal.valueOf(amount);

        if (currentBalance.compareTo(transferAmount) < 0) {
            return saveAndReturn("FAILED", null, fromAccount, toAccount, amount,
                    "Current balance is not sufficient for this transaction: " + currentBalance + " " + fromAccountDetails.getCurrency());
        }

        try {
            UpdateBalanceRequest debitRequest = new UpdateBalanceRequest();
            debitRequest.setAccountNumber(fromAccount);
            debitRequest.setAmount(transferAmount);
            debitRequest.setOperationType(com.banka.transactionservice.client.account.OperationType.DEBIT);
            accountPort.updateBalance(debitRequest);
        } catch (Exception e) {
            return saveAndReturn("FAILED", null, fromAccount, toAccount, amount,
                    "Failed to debit sender account: " + e.getMessage());
        }

        try {
            UpdateBalanceRequest creditRequest = new UpdateBalanceRequest();
            creditRequest.setAccountNumber(toAccount);
            creditRequest.setAmount(transferAmount);
            creditRequest.setOperationType(com.banka.transactionservice.client.account.OperationType.CREDIT);
            accountPort.updateBalance(creditRequest);
        } catch (Exception e) {
            return saveAndReturn("FAILED", null, fromAccount, toAccount, amount,
                    "Critical: debited sender but failed to credit receiver. Manual intervention needed: " + e.getMessage());
        }

        String transactionId = "TXN-" + System.currentTimeMillis();
        return saveAndReturn("SUCCESS", transactionId, fromAccount, toAccount, amount,
                "Transfer basariyla tamamlandi");
    }

    @Override
    public List<TransactionHistoryItem> getTransactionHistory(String accountNumber) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        List<TransactionRecord> records = transactionRepository
                .findByFromAccountOrToAccountOrderByCreatedAtDesc(accountNumber, accountNumber);

        return records.stream()
                .map(r -> new TransactionHistoryItem(
                        r.getTransactionId() != null ? r.getTransactionId() : "N/A",
                        r.getFromAccount(),
                        r.getToAccount(),
                        r.getAmount(),
                        r.getStatus(),
                        r.getMessage(),
                        r.getCreatedAt().format(formatter)
                ))
                .collect(Collectors.toList());
    }

    private TransferResult saveAndReturn(String status, String transactionId,
                                          String fromAccount, String toAccount,
                                          double amount, String message) {
        String id = transactionId != null ? transactionId : "TXN-" + System.currentTimeMillis() + "-FAILED";

        TransactionRecord record = new TransactionRecord(
                id, fromAccount, toAccount, BigDecimal.valueOf(amount), status, message
        );
        transactionRepository.save(record);

        return new TransferResult(status, transactionId, message);
    }
}