package com.payshield.transaction.Services;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payshield.transaction.Repository.OutboxEventRepository;

@Service
public class OutboxDeliveryStateService {

    private final OutboxEventRepository repository;

    public OutboxDeliveryStateService(
            OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean markPublished(UUID eventId, UUID claimToken) {

        return repository.markPublished(
                eventId,
                claimToken,
                Instant.now()
        ) == 1;
    }

    @Transactional
    public boolean recordFailure(
            UUID eventId,
            UUID claimToken,
            Instant nextAttemptAt,
            String lastError) {

        return repository.recordFailure(
                eventId,
                claimToken,
                nextAttemptAt,
                lastError
        ) == 1;
    }
}