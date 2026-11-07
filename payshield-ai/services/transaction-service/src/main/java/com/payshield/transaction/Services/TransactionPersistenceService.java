package com.payshield.transaction.Services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.payshield.transaction.Entity.Transaction;
import com.payshield.transaction.Repository.TransactionRepository;

@Service
public class TransactionPersistenceService {

    private final TransactionRepository transactionRepository;

    public TransactionPersistenceService(
            TransactionRepository transactionRepository
    ) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction save(Transaction transaction) {
        return transactionRepository.saveAndFlush(transaction);
    }
}