package com.banka.transactionservice.repository;

import com.banka.transactionservice.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, String> {

    List<TransactionRecord> findByFromAccountOrToAccountOrderByCreatedAtDesc(String fromAccount, String toAccount);
}