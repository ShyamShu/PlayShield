package com.payshield.transaction.Controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payshield.transaction.Dto.RequestDto.CreateTransactionRequest;
import com.payshield.transaction.Dto.RequestDto.TransactionFilter;
import com.payshield.transaction.Dto.ResponseDto.TransactionCreateResult;
import com.payshield.transaction.Dto.ResponseDto.TransactionResponse;
import com.payshield.transaction.Services.TransactionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/transactions")
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(
            TransactionService transactionService) {

        this.transactionService = transactionService;
    }

    // =====================================================
    // CREATE TRANSACTION
    // =====================================================
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {

        UUID submittedBy = extractUserId(jwt);

        TransactionCreateResult result
                = transactionService.createTransaction(
                        request,
                        idempotencyKey,
                        submittedBy
                );

        HttpStatus status = result.created()
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity
                .status(status)
                .body(result.transaction());
    }

    // =====================================================
    // GET TRANSACTION BY ID
    // =====================================================
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @PathVariable UUID id) {

        TransactionResponse response
                = transactionService.getTransactionById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @ModelAttribute TransactionFilter filter,
            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        Page<TransactionResponse> response
                = transactionService.getTransactions(filter, pageable);

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // EXTRACT AUTHENTICATED USER ID
    // =====================================================
    private UUID extractUserId(Jwt jwt) {

        if (jwt == null || jwt.getSubject() == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authenticated user identity is missing"
            );
        }

        try {
            return UUID.fromString(jwt.getSubject());

        } catch (IllegalArgumentException ex) {

            throw new AuthenticationCredentialsNotFoundException(
                    "Invalid authenticated user identity"
            );
        }
    }
}
