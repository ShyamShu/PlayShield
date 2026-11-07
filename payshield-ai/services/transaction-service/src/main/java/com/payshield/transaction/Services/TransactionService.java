package com.payshield.transaction.Services;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Currency;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payshield.transaction.Dto.RequestDto.CreateTransactionRequest;
import com.payshield.transaction.Dto.RequestDto.TransactionFilter;
import com.payshield.transaction.Dto.ResponseDto.TransactionCreateResult;
import com.payshield.transaction.Dto.ResponseDto.TransactionResponse;
import com.payshield.transaction.Entity.Transaction;
import com.payshield.transaction.Exceptions.DuplicateTransactionException;
import com.payshield.transaction.Exceptions.InvalidTransactionException;
import com.payshield.transaction.Exceptions.TransactionNotFoundException;
import com.payshield.transaction.Properties.TransactionProperties;
import com.payshield.transaction.Repository.TransactionRepository;
import com.payshield.transaction.Specifications.TransactionSpecifications;

@Service
public class TransactionService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "customerRef",
            "merchantRef",
            "amount",
            "currency",
            "transactionType",
            "paymentStatus",
            "channel",
            "createdAt",
            "occurredAt"
    );

    private final TransactionRepository transactionRepository;
    private final TransactionPersistenceService persistenceService;
    private final TransactionProperties properties;

    public TransactionService(
            TransactionRepository transactionRepository,
            TransactionPersistenceService persistenceService,
            TransactionProperties properties
    ) {
        this.transactionRepository = transactionRepository;
        this.persistenceService = persistenceService;
        this.properties = properties;
    }

    // =========================================================
    // 1. CREATE TRANSACTION RECORD
    // =========================================================

    public TransactionCreateResult createTransaction(
            CreateTransactionRequest request,
            String idempotencyKey,
            UUID submittedBy
    ) {

        // Step 1: Validate required inputs.

        if (request == null) {
            throw new InvalidTransactionException(
                    "Transaction request cannot be null"
            );
        }

        if (submittedBy == null) {
            throw new InvalidTransactionException(
                    "Authenticated admin is required"
            );
        }

        // Step 2: Normalize the idempotency key.

        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);

        // Step 3: Normalize incoming values.

        CreateTransactionRequest normalizedRequest =
                normalizeRequest(request);

        // Step 4: Validate configuration-dependent business rules.

        validateBusinessRules(normalizedRequest);

        // Step 5: Generate request hash on the server.

        String requestHash = generateRequestHash(normalizedRequest);

        // Step 6: Check existing transaction for this admin and key.

        var existingTransaction =
                transactionRepository.findBySubmittedByAndIdempotencyKey(
                        submittedBy,
                        normalizedKey
                );

        if (existingTransaction.isPresent()) {
            return resolveExisting(
                    existingTransaction.get(),
                    requestHash
            );
        }

        // Step 7: Create the transaction entity.

        Transaction transaction = new Transaction();

        transaction.setCustomerRef(normalizedRequest.customerRef());
        transaction.setMerchantRef(normalizedRequest.merchantRef());
        transaction.setAmount(normalizedRequest.amount());
        transaction.setCurrency(normalizedRequest.currency());

        transaction.setTransactionType(
                normalizedRequest.transactionType().name()
        );

        // Status is supplied by the admin.
        // PayShield does not authorize or decline the payment itself.

        transaction.setPaymentStatus(
                normalizedRequest.paymentStatus().name()
        );

        transaction.setChannel(
                normalizedRequest.channel().name()
        );

        transaction.setDeviceRef(normalizedRequest.deviceRef());
        transaction.setDescription(normalizedRequest.description());

        // Backend-controlled fields.

        transaction.setSubmittedBy(submittedBy);
        transaction.setIdempotencyKey(normalizedKey);
        transaction.setRequestHash(requestHash);
        transaction.setOccurredAt(normalizedRequest.occurredAt());

        // Step 8: Save transaction.
        // Persistence service has its own transaction boundary.

        try {

            Transaction savedTransaction =
                    persistenceService.save(transaction);

            return new TransactionCreateResult(
                    TransactionResponse.fromEntity(savedTransaction),
                    true
            );

        } catch (DataIntegrityViolationException ex) {

            /*
             * A concurrent request may have inserted the same
             * submitter/key combination after our initial lookup.
             *
             * The separate persistence transaction has rolled back
             * before execution reaches this catch block.
             */

            var concurrentTransaction =
                    transactionRepository.findBySubmittedByAndIdempotencyKey(
                            submittedBy,
                            normalizedKey
                    );

            if (concurrentTransaction.isEmpty()) {
                // Not an idempotency conflict.
                // Let the global exception handler process it.
                throw ex;
            }

            return resolveExisting(
                    concurrentTransaction.get(),
                    requestHash
            );
        }
    }

    // =========================================================
    // 2. RESOLVE EXISTING IDEMPOTENCY RECORD
    // =========================================================

    private TransactionCreateResult resolveExisting(
            Transaction existing,
            String requestHash
    ) {

        boolean sameRequest = MessageDigest.isEqual(
                existing.getRequestHash().getBytes(StandardCharsets.UTF_8),
                requestHash.getBytes(StandardCharsets.UTF_8)
        );

        if (!sameRequest) {
            throw new DuplicateTransactionException(
                    "Idempotency key already used for another transaction"
            );
        }

        // Same admin + same key + same payload = replay.

        return new TransactionCreateResult(
                TransactionResponse.fromEntity(existing),
                false
        );
    }

    // =========================================================
    // 3. NORMALIZE REQUEST
    // =========================================================

    private CreateTransactionRequest normalizeRequest(
            CreateTransactionRequest request
    ) {

        return new CreateTransactionRequest(
                trimRequired(request.customerRef()),
                trimRequired(request.merchantRef()),
                request.amount(),
                trimRequired(request.currency()),
                request.transactionType(),
                request.paymentStatus(),
                request.channel(),
                trimOptional(request.deviceRef()),
                trimOptional(request.description()),
                request.occurredAt()
        );
    }

    private String normalizeIdempotencyKey(String key) {

        if (key == null || key.isBlank()) {
            throw new InvalidTransactionException(
                    "Idempotency key is required"
            );
        }

        String normalized = key.trim();

        if (normalized.length() > 100) {
            throw new InvalidTransactionException(
                    "Idempotency key cannot exceed 100 characters"
            );
        }

        return normalized;
    }

    private String trimRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String trimOptional(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    // =========================================================
    // 4. CONFIGURATION-DEPENDENT VALIDATION
    // =========================================================

    private void validateBusinessRules(
            CreateTransactionRequest request
    ) {

        BigDecimal amount = request.amount();

        // Maximum amount from application.yml.

        if (amount.compareTo(properties.maxAmount()) > 0) {
            throw new InvalidTransactionException(
                    "Transaction amount exceeds the maximum allowed amount"
            );
        }

        // Database column supports NUMERIC(19,4).

        if (amount.stripTrailingZeros().scale() > 4) {
            throw new InvalidTransactionException(
                    "Amount cannot have more than 4 decimal places"
            );
        }

        // Validate actual ISO currency code.

        try {
            Currency.getInstance(request.currency());

        } catch (IllegalArgumentException ex) {
            throw new InvalidTransactionException(
                    "Unsupported ISO currency code"
            );
        }

        // Validate occurrence timestamp.

        Instant now = Instant.now();
        Instant occurredAt = request.occurredAt();

        if (occurredAt.isAfter(
                now.plus(properties.maxFutureSkew())
        )) {
            throw new InvalidTransactionException(
                    "Transaction occurrence time is too far in the future"
            );
        }

        if (occurredAt.isBefore(
                now.minus(properties.maxAge())
        )) {
            throw new InvalidTransactionException(
                    "Transaction occurrence time exceeds the maximum allowed age"
            );
        }
    }

    // =========================================================
    // 5. GENERATE REQUEST HASH
    // =========================================================

    private String generateRequestHash(
            CreateTransactionRequest request
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            updateHash(digest, request.customerRef());
            updateHash(digest, request.merchantRef());

            updateHash(
                    digest,
                    request.amount()
                            .stripTrailingZeros()
                            .toPlainString()
            );

            updateHash(digest, request.currency());

            updateHash(
                    digest,
                    request.transactionType().name()
            );

            // Include status because the admin submits it.

            updateHash(
                    digest,
                    request.paymentStatus().name()
            );

            updateHash(
                    digest,
                    request.channel().name()
            );

            updateHash(digest, request.deviceRef());
            updateHash(digest, request.description());

            updateHash(
                    digest,
                    request.occurredAt().toString()
            );

            return HexFormat.of().formatHex(digest.digest());

        } catch (NoSuchAlgorithmException ex) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    ex
            );
        }
    }

    private void updateHash(
            MessageDigest digest,
            String value
    ) {

        if (value == null) {

            digest.update(
                    ByteBuffer.allocate(4)
                            .putInt(-1)
                            .array()
            );

            return;
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        // Length-prefix each value to avoid delimiter collisions.

        digest.update(
                ByteBuffer.allocate(4)
                        .putInt(bytes.length)
                        .array()
        );

        digest.update(bytes);
    }

    // =========================================================
    // 6. GET TRANSACTION BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID id) {

        if (id == null) {
            throw new InvalidTransactionException(
                    "Transaction ID is required"
            );
        }

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() ->
                        new TransactionNotFoundException(
                                "Transaction not found"
                        )
                );

        return TransactionResponse.fromEntity(transaction);
    }

    // =========================================================
    // 7. GET ALL TRANSACTIONS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAllTransactions(
            Pageable pageable
    ) {

        return getTransactions(null, pageable);
    }

    // =========================================================
    // 11. COMMON FILTER METHOD
    // =========================================================

    @Transactional(readOnly = true)
        public Page<TransactionResponse> getTransactions(
                TransactionFilter filter,
                Pageable pageable) {

        validatePageable(pageable);

        TransactionFilter normalizedFilter = normalizeFilter(filter);

        return transactionRepository.findAll(
                TransactionSpecifications.withFilters(normalizedFilter),
                pageable
        ).map(TransactionResponse::fromEntity);
        }
    // =========================================================
    // 12. PAGINATION VALIDATION
    // =========================================================

    private void validatePageable(Pageable pageable) {

        if (pageable == null || pageable.isUnpaged()) {
            throw new InvalidTransactionException(
                    "Valid pagination information is required"
            );
        }

        if (pageable.getPageSize() > properties.maxPageSize()) {
            throw new InvalidTransactionException(
                    "Page size cannot exceed "
                            + properties.maxPageSize()
            );
        }

        pageable.getSort().forEach(order -> {

            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {

                throw new InvalidTransactionException(
                        "Sorting by '"
                                + order.getProperty()
                                + "' is not supported"
                );
            }
        });
    }

    private TransactionFilter normalizeFilter(TransactionFilter filter) {

    if (filter == null) {
        return null;
    }

    String customerRef = filter.customerRef() == null
            ? null : filter.customerRef().trim();

    String merchantRef = filter.merchantRef() == null
            ? null : filter.merchantRef().trim();

    String currency = filter.currency() == null
            ? null : filter.currency().trim();

    if (customerRef != null && customerRef.length() > 64) {
        throw new InvalidTransactionException(
                "Customer reference cannot exceed 64 characters"
        );
    }

    if (merchantRef != null && merchantRef.length() > 64) {
        throw new InvalidTransactionException(
                "Merchant reference cannot exceed 64 characters"
        );
    }

    if (currency != null && !currency.isBlank()
            && !currency.matches("^[A-Z]{3}$")) {
        throw new InvalidTransactionException(
                "Currency must contain three uppercase letters"
        );
    }

    if (filter.fromDate() != null
            && filter.toDate() != null
            && filter.fromDate().isAfter(filter.toDate())) {

        throw new InvalidTransactionException(
                "From date cannot be after to date"
        );
    }

    return new TransactionFilter(
            customerRef,
            merchantRef,
            filter.paymentStatus(),
            filter.transactionType(),
            filter.channel(),
            currency,
            filter.fromDate(),
            filter.toDate()
    );
}
}