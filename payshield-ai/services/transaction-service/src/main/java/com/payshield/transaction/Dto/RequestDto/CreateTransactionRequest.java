package com.payshield.transaction.Dto.RequestDto;

import java.math.BigDecimal;
import java.time.Instant;

import com.payshield.transaction.Enums.PaymentStatus;
import com.payshield.transaction.Enums.TransactionChannel;
import com.payshield.transaction.Enums.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTransactionRequest(

        @NotBlank(message = "Customer reference is required")
        @Size(max = 64, message = "Customer reference cannot exceed 64 characters")
        String customerRef,

        @NotBlank(message = "Merchant reference is required")
        @Size(max = 64, message = "Merchant reference cannot exceed 64 characters")
        String merchantRef,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
        @Digits(
                integer = 15,
                fraction = 4,
                message = "Amount supports up to 15 integer digits and 4 decimal places"
        )
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(
                regexp = "^[A-Z]{3}$",
                message = "Currency must be a three-letter uppercase code"
        )
        String currency,

        @NotNull(message = "Transaction type is required")
        TransactionType transactionType,

        @NotNull(message = "Payment status is required")
        PaymentStatus paymentStatus,

        @NotNull(message = "Transaction channel is required")
        TransactionChannel channel,

        @Size(max = 64, message = "Device reference cannot exceed 64 characters")
        String deviceRef,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @NotNull(message = "Transaction occurrence timestamp is required")
        Instant occurredAt

) {
}