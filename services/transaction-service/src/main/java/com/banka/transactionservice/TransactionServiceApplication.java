package com.banka.transactionservice;

import com.banka.transactionservice.service.TransactionServiceImpl;
import jakarta.xml.ws.Endpoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TransactionServiceApplication {

    @Value("${transaction.service.publish.url:http://localhost:8084/ws/transactions}")
    private String publishAddress;

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner publishSoapEndpoint(TransactionServiceImpl transactionServiceImpl) {
        return args -> {
            Endpoint.publish(publishAddress, transactionServiceImpl);
            System.out.println("Transaction SOAP servisi yayinda: " + publishAddress);
        };
    }

}