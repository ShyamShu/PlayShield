
package com.payshield.transaction.Services;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payshield.transaction.Entity.OutboxEvent;

import jakarta.persistence.EntityManager;

@Service
public class OutboxClaimService {

    private final EntityManager entityManager;

    private static final Duration LEASE_DURATION =
            Duration.ofMinutes(3);

    public OutboxClaimService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public List<OutboxEvent> claimBatch(int batchSize) {

        UUID claimToken = UUID.randomUUID();

        List<OutboxEvent> events = entityManager.createNativeQuery(
                """
                SELECT *
                FROM outbox_events
                WHERE published_at IS NULL
                  AND next_attempt_at <= now()
                  AND (claimed_until IS NULL OR claimed_until < now())
                ORDER BY created_at ASC
                LIMIT :batchSize
                FOR UPDATE SKIP LOCKED
                """,
                OutboxEvent.class
        )
        .setParameter("batchSize", batchSize)
        .getResultList();

        Instant leaseExpiry = Instant.now().plus(LEASE_DURATION);

        for (OutboxEvent event : events) {
            event.setClaimToken(claimToken);
            event.setClaimedUntil(leaseExpiry);
        }

        entityManager.flush();

        return events;
    }
}
