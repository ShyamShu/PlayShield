
package com.payshield.transaction.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.payshield.transaction.Entity.Transaction;

public interface TransactionRepository
        extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByCustomerRef(
            String customerRef, Pageable pageable);

    Page<Transaction> findByMerchantRef(
            String merchantRef, Pageable pageable);

    Page<Transaction> findByPaymentStatus(
            String paymentStatus, Pageable pageable);

    Optional<Transaction> findByIdempotencyKey(
            String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
