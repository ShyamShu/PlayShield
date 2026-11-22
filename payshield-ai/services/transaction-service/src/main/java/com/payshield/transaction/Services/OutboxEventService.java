package com.payshield.transaction.Services;


import java.math.RoundingMode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payshield.transaction.Dto.EventDto.TransactionSubmittedPayload;
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

    public OutboxEvent saveTransactionSubmittedEvent(
        Transaction transaction,
        String correlationId
    ) {
        TransactionSubmittedPayload payload =
                buildPayload(transaction, correlationId);

        OutboxEvent event = new OutboxEvent();

        event.setId(UUID.randomUUID());
        event.setAggregateType("TRANSACTION");
        event.setAggregateId(transaction.getId());
        event.setEventType("TRANSACTION_SUBMITTED");
        event.setSchemaVersion(1);

        event.setCorrelationId(correlationId);
        event.setPartitionKey(transaction.getCustomerRef());
        event.setPayload(payload);

        event.setAttempts(0);

        return outboxEventRepository.save(event);
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
    @Transactional
    public void markPublished(UUID eventId) {

        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Outbox event not found: " + eventId
                        ));

        event.setPublishedAt(Instant.now());

        outboxEventRepository.save(event);
    }

    @Transactional
    public void recordFailure(UUID eventId) {

        outboxEventRepository.findById(eventId)
                .ifPresent(event -> {
                    event.setAttempts(event.getAttempts() + 1);
                    outboxEventRepository.save(event);
                });
    }

    private TransactionSubmittedPayload buildPayload(
        Transaction transaction,
        String correlationId
    ) {
        int fractionDigits = Currency.getInstance(
                transaction.getCurrency()
        ).getDefaultFractionDigits();

        String amount = transaction.getAmount()
                .setScale(fractionDigits, RoundingMode.UNNECESSARY)
                .toPlainString();

        String occurredAt = DateTimeFormatter.ISO_INSTANT.format(
                transaction.getOccurredAt()
        );

        return new TransactionSubmittedPayload(
                transaction.getId().toString(),
                transaction.getCustomerRef(),
                transaction.getMerchantRef(),
                amount,
                transaction.getCurrency(),
                transaction.getTransactionType(),
                transaction.getPaymentStatus(),
                transaction.getChannel(),
                transaction.getDeviceRef(),
                transaction.getDescription(),
                occurredAt,
                correlationId
        );
    }
}