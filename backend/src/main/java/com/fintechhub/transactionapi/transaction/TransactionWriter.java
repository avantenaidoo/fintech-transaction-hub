// Duplicate idempotency key can be detected at the save boundary
package com.fintechhub.transactionapi.transaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionWriter {

    private final TransactionRepository transactionRepository;

    public TransactionWriter(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction save(Transaction transaction) {
        return transactionRepository.saveAndFlush(transaction);
    }
}