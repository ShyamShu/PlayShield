package com.payshield.transaction.Dto.EventDto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OutboxEventMessage(
        UUID eventId,
        String eventType,
        Integer schemaVersion,
        String aggregateType,
        UUID aggregateId,
        String correlationId,
        Instant createdAt,
        Map<String, Object> payload
) {
}