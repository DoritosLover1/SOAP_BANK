package com.banka.apigateway.controller;

import com.banka.apigateway.client.transaction.*;
import com.banka.apigateway.dto.TransactionHistoryResponse;
import com.banka.apigateway.dto.TransferRequest;
import com.banka.apigateway.dto.TransferResponse;
import jakarta.xml.ws.BindingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transfer")
public class TransferController {

    @Value("${transaction.service.wsdl-url:http://localhost:8084/ws/transactions?wsdl}")
    private String wsdlUrl;

    @Value("${transaction.service.endpoint-url:http://localhost:8084/ws/transactions}")
    private String endpointUrl;

    private TransactionService getTransactionPort() {
        try {
            TransactionServiceImplService service = new TransactionServiceImplService(new URL(wsdlUrl));
            TransactionService port = service.getTransactionServiceImplPort();
            ((BindingProvider) port).getRequestContext()
                    .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
            return port;
        } catch (MalformedURLException e) {
            throw new RuntimeException("transaction-service WSDL adresi geçersiz: " + wsdlUrl, e);
        }
    }

    @PostMapping
    public ResponseEntity<?> transfer(@RequestBody TransferRequest request) {
        try {
            TransferResult soapResult = getTransactionPort().transferMoney(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount()
            );

            TransferResponse response = new TransferResponse(
                    soapResult.getStatus(),
                    soapResult.getTransactionId(),
                    soapResult.getMessage()
            );

            if ("SUCCESS".equals(soapResult.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Transfer islenemedi: " + e.getMessage());
        }
    }

    @GetMapping("/history/{accountNumber}")
    public ResponseEntity<?> getHistory(@PathVariable String accountNumber) {
        try {
            List<TransactionHistoryItem> soapHistory =
                    getTransactionPort().getTransactionHistory(accountNumber);

            List<TransactionHistoryResponse> response = soapHistory.stream()
                    .map(item -> new TransactionHistoryResponse(
                            item.getTransactionId(),
                            item.getFromAccount(),
                            item.getToAccount(),
                            item.getAmount(),
                            item.getStatus(),
                            item.getMessage(),
                            item.getCreatedAt()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Islem gecmisi alinamadi: " + e.getMessage());
        }
    }
}