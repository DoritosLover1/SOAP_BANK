package com.banka.transactionservice;

import com.banka.transactionservice.service.TransactionServiceImpl;
import jakarta.xml.ws.Endpoint;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner publishSoapEndpoint() {
        return args -> {
            String address = "http://localhost:8082/ws/transactions";
            Endpoint.publish(address, new TransactionServiceImpl());
            System.out.println("Transaction SOAP servisi yayinda: " + address);
        };
    }

}