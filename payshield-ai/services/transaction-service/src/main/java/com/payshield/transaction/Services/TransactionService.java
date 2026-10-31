
package com.payshield.transaction.Services;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payshield.transaction.Dto.RequestDto.CreateTransactionRequest;
import com.payshield.transaction.Dto.ResponseDto.TransactionResponse;
import com.payshield.transaction.Entity.Transaction;
import com.payshield.transaction.Exceptions.DuplicateTransactionException;
import com.payshield.transaction.Exceptions.InvalidTransactionException;
import com.payshield.transaction.Exceptions.TransactionNotFoundException;
import com.payshield.transaction.Repository.TransactionRepository;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(
            TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // =====================================================
    // CREATE TRANSACTION
    // =====================================================

    @Transactional
    public TransactionResponse createTransaction(
            CreateTransactionRequest request,
            String idempotencyKey,
            UUID submittedBy) {

        // 1. Validate request
        if (request == null) {
            throw new InvalidTransactionException(
                    "Transaction request cannot be null");
        }

        validateRequest(request);

        // 2. Validate idempotency key
        if (isBlank(idempotencyKey) ||
                idempotencyKey.length() > 100) {

            throw new InvalidTransactionException(
                    "Valid idempotency key is required");
        }

        // 3. Validate authenticated user
        if (submittedBy == null) {
            throw new InvalidTransactionException(
                    "Authenticated transaction submitter is required");
        }

        // 4. Generate request hash on the server
        String requestHash = generateRequestHash(request);

        // 5. Check existing transaction
        Optional<Transaction> existingTransaction =
                transactionRepository.findByIdempotencyKey(
                        idempotencyKey);

        if (existingTransaction.isPresent()) {

            Transaction existing = existingTransaction.get();

            // Same key + same request = return existing transaction
            if (MessageDigest.isEqual(
                    existing.getRequestHash()
                            .getBytes(StandardCharsets.UTF_8),
                    requestHash.getBytes(StandardCharsets.UTF_8))) {

                return TransactionResponse.fromEntity(existing);
            }

            // Same key + different request = conflict
            throw new DuplicateTransactionException(
                    "Idempotency key already used for another transaction");
        }

        // 6. Create transaction entity
        Transaction transaction = new Transaction();

        transaction.setCustomerRef(request.getCustomerRef().trim());
        transaction.setMerchantRef(request.getMerchantRef().trim());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setPaymentStatus("PENDING");
        transaction.setChannel(request.getChannel());
        transaction.setDeviceRef(request.getDeviceRef());
        transaction.setDescription(request.getDescription());

        // Backend-controlled fields
        transaction.setSubmittedBy(submittedBy);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setRequestHash(requestHash);
        transaction.setOccurredAt(request.getOccurredAt());

        // 7. Save transaction
        Transaction savedTransaction =
                transactionRepository.saveAndFlush(transaction);

        // 8. Return response DTO
        return TransactionResponse.fromEntity(savedTransaction);
    }


    // =====================================================
    // VALIDATE REQUEST
    // =====================================================

    private void validateRequest(CreateTransactionRequest request) {

        if (isBlank(request.getCustomerRef()) ||
                request.getCustomerRef().length() > 64) {

            throw new InvalidTransactionException(
                    "Invalid customer reference");
        }

        if (isBlank(request.getMerchantRef()) ||
                request.getMerchantRef().length() > 64) {

            throw new InvalidTransactionException(
                    "Invalid merchant reference");
        }

        BigDecimal amount = request.getAmount();

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new InvalidTransactionException(
                    "Transaction amount must be greater than zero");
        }

        BigDecimal normalizedAmount = amount.stripTrailingZeros();

        if (normalizedAmount.scale() > 4 ||
                normalizedAmount.precision()
                        - normalizedAmount.scale() > 15) {

            throw new InvalidTransactionException(
                    "Transaction amount exceeds supported precision");
        }

        if (request.getCurrency() == null ||
                !request.getCurrency().matches("[A-Z]{3}")) {

            throw new InvalidTransactionException(
                    "Currency must be a three-letter uppercase code");
        }

        if (isBlank(request.getTransactionType()) ||
                request.getTransactionType().length() > 20) {

            throw new InvalidTransactionException(
                    "Invalid transaction type");
        }

        if (isBlank(request.getChannel()) ||
                request.getChannel().length() > 20) {

            throw new InvalidTransactionException(
                    "Invalid transaction channel");
        }

        if (request.getOccurredAt() == null) {
            throw new InvalidTransactionException(
                    "Transaction occurrence timestamp is required");
        }

        if (request.getDeviceRef() != null &&
                request.getDeviceRef().length() > 64) {

            throw new InvalidTransactionException(
                    "Device reference cannot exceed 64 characters");
        }

        if (request.getDescription() != null &&
                request.getDescription().length() > 500) {

            throw new InvalidTransactionException(
                    "Description cannot exceed 500 characters");
        }
    }


    // =====================================================
    // GENERATE REQUEST HASH
    // =====================================================

    private String generateRequestHash(
            CreateTransactionRequest request) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            updateHash(digest, request.getCustomerRef().trim());
            updateHash(digest, request.getMerchantRef().trim());

            updateHash(
                    digest,
                    request.getAmount()
                            .stripTrailingZeros()
                            .toPlainString());

            updateHash(digest, request.getCurrency());
            updateHash(digest, request.getTransactionType());
            updateHash(digest, request.getChannel());
            updateHash(digest, request.getDeviceRef());
            updateHash(digest, request.getDescription());
            updateHash(digest, request.getOccurredAt().toString());

            return HexFormat.of().formatHex(digest.digest());

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable", ex);
        }
    }


    private void updateHash(MessageDigest digest, String value) {

        if (value == null) {
            digest.update(ByteBuffer.allocate(4).putInt(-1).array());
            return;
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        // Prefix each value with its length to avoid delimiter collisions
        digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
        digest.update(bytes);
    }


    // =====================================================
    // GET TRANSACTION BY ID
    // =====================================================

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID id) {

        if (id == null) {
            throw new InvalidTransactionException(
                    "Transaction ID cannot be null");
        }

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() ->
                        new TransactionNotFoundException(
                                "Transaction not found"));

        return TransactionResponse.fromEntity(transaction);
    }


    // =====================================================
    // GET ALL TRANSACTIONS
    // =====================================================

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAllTransactions(
            Pageable pageable) {

        validatePageable(pageable);

        return transactionRepository.findAll(pageable)
                .map(TransactionResponse::fromEntity);
    }


    // =====================================================
    // GET TRANSACTIONS BY CUSTOMER
    // =====================================================

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByCustomer(
            String customerRef,
            Pageable pageable) {

        if (isBlank(customerRef)) {
            throw new InvalidTransactionException(
                    "Customer reference cannot be empty");
        }

        if (customerRef.length() > 64) {
            throw new InvalidTransactionException(
                    "Customer reference cannot exceed 64 characters");
        }

        validatePageable(pageable);

        return transactionRepository.findByCustomerRef(
                customerRef.trim(), pageable)
                .map(TransactionResponse::fromEntity);
    }


    // =====================================================
    // GET TRANSACTIONS BY MERCHANT
    // =====================================================

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByMerchant(
            String merchantRef,
            Pageable pageable) {

        if (isBlank(merchantRef)) {
            throw new InvalidTransactionException(
                    "Merchant reference cannot be empty");
        }

        if (merchantRef.length() > 64) {
            throw new InvalidTransactionException(
                    "Merchant reference cannot exceed 64 characters");
        }

        validatePageable(pageable);

        return transactionRepository.findByMerchantRef(
                merchantRef.trim(), pageable)
                .map(TransactionResponse::fromEntity);
    }


    // =====================================================
    // GET TRANSACTIONS BY PAYMENT STATUS
    // =====================================================

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByStatus(
            String paymentStatus,
            Pageable pageable) {

        if (isBlank(paymentStatus) ||
                paymentStatus.length() > 20) {

            throw new InvalidTransactionException(
                    "Invalid payment status");
        }

        validatePageable(pageable);

        return transactionRepository.findByPaymentStatus(
                paymentStatus, pageable)
                .map(TransactionResponse::fromEntity);
    }


    // =====================================================
    // COMMON VALIDATION
    // =====================================================

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validatePageable(Pageable pageable) {

        if (pageable == null) {
            throw new InvalidTransactionException(
                    "Pagination information is required");
        }
    }
}
