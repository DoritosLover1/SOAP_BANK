package com.banka.apigateway.controller;

import com.banka.apigateway.client.transaction.*;
import com.banka.apigateway.dto.TransferRequest;
import com.banka.apigateway.dto.TransferResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfer")
public class TransferController {

    private TransactionService getTransactionPort() {
        TransactionServiceImplService service = new TransactionServiceImplService();
        return service.getTransactionServiceImplPort();
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
}