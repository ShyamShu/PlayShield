package com.payshield.transaction.Services;


import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.payshield.transaction.Entity.OutboxEvent;
import com.payshield.transaction.Entity.Transaction;
import com.payshield.transaction.Repository.OutboxEventRepository;

@Service
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventService(
            OutboxEventRepository outboxEventRepository) {

        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent saveTransactionSubmittedEvent(
            Transaction transaction) {

        OutboxEvent event = new OutboxEvent();

        event.setAggregateType("TRANSACTION");
        event.setAggregateId(transaction.getId());

        event.setEventType("TRANSACTION_SUBMITTED");
        event.setSchemaVersion(1);

        event.setPayload(buildPayload(transaction));

        return outboxEventRepository.saveAndFlush(event);
    }

    private Map<String, Object> buildPayload(
            Transaction transaction) {

        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("transactionId", transaction.getId());
        payload.put("customerRef", transaction.getCustomerRef());
        payload.put("merchantRef", transaction.getMerchantRef());
        payload.put("amount", transaction.getAmount());
        payload.put("currency", transaction.getCurrency());
        payload.put("transactionType", transaction.getTransactionType());
        payload.put("paymentStatus", transaction.getPaymentStatus());
        payload.put("channel", transaction.getChannel());
        payload.put("deviceRef", transaction.getDeviceRef());
        payload.put("occurredAt", transaction.getOccurredAt());
        payload.put("submittedBy", transaction.getSubmittedBy());

        return payload;
    }
}