package com.payshield.transaction.Dto.EventDto;

public record TransactionSubmittedPayload(
        String transactionId,
        String customerRef,
        String merchantRef,
        String amount,
        String currency,
        String transactionType,
        String paymentStatus,
        String channel,
        String deviceRef,
        String description,
        String occurredAt,
        String correlationId
) {}