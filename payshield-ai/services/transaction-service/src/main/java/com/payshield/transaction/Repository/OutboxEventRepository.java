package com.payshield.transaction.Repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.payshield.transaction.Entity.OutboxEvent;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    @Modifying
    @Query("""
        UPDATE OutboxEvent e
        SET e.publishedAt = :publishedAt,
            e.claimToken = null,
            e.claimedUntil = null,
            e.lastError = null
        WHERE e.id = :eventId
          AND e.claimToken = :claimToken
          AND e.publishedAt IS NULL
        """)
    int markPublished(
            @Param("eventId") UUID eventId,
            @Param("claimToken") UUID claimToken,
            @Param("publishedAt") Instant publishedAt
    );

    @Modifying
    @Query("""
        UPDATE OutboxEvent e
        SET e.attempts = e.attempts + 1,
            e.nextAttemptAt = :nextAttemptAt,
            e.lastError = :lastError,
            e.claimToken = null,
            e.claimedUntil = null
        WHERE e.id = :eventId
          AND e.claimToken = :claimToken
          AND e.publishedAt IS NULL
        """)
    int recordFailure(
            @Param("eventId") UUID eventId,
            @Param("claimToken") UUID claimToken,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("lastError") String lastError
    );
}