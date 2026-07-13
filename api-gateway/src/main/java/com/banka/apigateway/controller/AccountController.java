package com.banka.apigateway.controller;

import com.banka.apigateway.client.account.*;
import com.banka.apigateway.dto.AccountResponse;
import com.banka.apigateway.dto.CreateAccountRequest;
import jakarta.xml.ws.BindingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.net.URL;

@RestController
@RequestMapping("api/accounts")
public class AccountController {

    // Build-time'da wsimport da bu adresi kullanıyordu (pom.xml'de); runtime'da ise
    // aşağıdaki iki property cluster içinde "account-service" servis adına çevrilecek.
    @Value("${account.service.wsdl-url:http://localhost:8081/ws/accounts.wsdl}")
    private String wsdlUrl;

    @Value("${account.service.endpoint-url:http://localhost:8081/ws/accounts}")
    private String endpointUrl;

    private AccountServicePort getAccountPort() {
        try {
            AccountServicePortService service = new AccountServicePortService(new URL(wsdlUrl));
            AccountServicePort port = service.getAccountServicePortSoap11();
            // WSDL içine gömülü adres ne olursa olsun, gerçek çağrı adresini burada zorluyoruz
            ((BindingProvider) port).getRequestContext()
                    .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
            return port;
        } catch (MalformedURLException e) {
            throw new RuntimeException("account-service WSDL adresi geçersiz: " + wsdlUrl, e);
        }
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<?> getAccount(@PathVariable String accountNumber) {
        try {
            GetAccountDetailsRequest request = new GetAccountDetailsRequest();
            request.setAccountNumber(accountNumber);

            GetAccountDetailsResponse soapResponse = getAccountPort().getAccountDetails(request);

            AccountResponse response = new AccountResponse(
                    soapResponse.getAccountNumber(),
                    soapResponse.getBalance(),
                    soapResponse.getCurrency().value()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Account was not found: " + accountNumber);
        }
    }

    @PostMapping
    public ResponseEntity<?> createAccount(@RequestBody CreateAccountRequest request) {
        try {
            com.banka.apigateway.client.account.CreateAccountRequest soapRequest =
                    new com.banka.apigateway.client.account.CreateAccountRequest();

            soapRequest.setOwnerName(request.getOwnerName());
            soapRequest.setInitialBalance(request.getInitialBalance());
            soapRequest.setCurrency(CurrencyType.valueOf(request.getCurrency()));

            CreateAccountResponse soapResponse = getAccountPort().createAccount(soapRequest);

            return ResponseEntity.status(HttpStatus.CREATED).body(soapResponse.getAccountNumber());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Gecersiz para birimi: " + request.getCurrency());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Hesap olusturulamadi: " + e.getMessage());
        }
    }
}