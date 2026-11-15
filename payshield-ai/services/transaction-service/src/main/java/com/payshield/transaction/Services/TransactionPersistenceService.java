package com.payshield.transaction.Services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.payshield.transaction.Entity.OutboxEvent;
import com.payshield.transaction.Entity.Transaction;
import com.payshield.transaction.Repository.TransactionRepository;

@Service
public class TransactionPersistenceService {

    private final TransactionRepository transactionRepository;
    private final OutboxEventService outboxEventService;

    public TransactionPersistenceService(
            TransactionRepository transactionRepository,
            OutboxEventService outboxEventService) {

        this.transactionRepository = transactionRepository;
        this.outboxEventService = outboxEventService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction save(Transaction transaction) {

        // 1. Save the transaction first.
        Transaction savedTransaction =
                transactionRepository.saveAndFlush(transaction);

        // 2. Create and save the corresponding outbox event.
        OutboxEvent event =
                outboxEventService.saveTransactionSubmittedEvent(
                        savedTransaction
                );

        // 3. Return the saved transaction.
        return savedTransaction;
    }
}