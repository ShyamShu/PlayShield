package com.payshield.transaction.Repository;


import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payshield.transaction.Entity.OutboxEvent;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {
}