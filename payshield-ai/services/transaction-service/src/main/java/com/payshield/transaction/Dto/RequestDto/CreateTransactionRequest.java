package com.payshield.transaction.Dto.RequestDto;



import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public class CreateTransactionRequest {

    @NotBlank(message = "Customer reference is required")
    @Size(max = 64, message = "Customer reference cannot exceed 64 characters")
    private String customerRef;

    @NotBlank(message = "Merchant reference is required")
    @Size(max = 64, message = "Merchant reference cannot exceed 64 characters")
    private String merchantRef;

    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
    @Digits(integer = 15, fraction = 4,
            message = "Amount must support up to 15 integer and 4 decimal digits")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$",
            message = "Currency must be a three-letter uppercase code")
    private String currency;

    @NotBlank(message = "Transaction type is required")
    @Size(max = 20)
    private String transactionType;

    @NotBlank(message = "Channel is required")
    @Size(max = 20)
    private String channel;

    @Size(max = 64)
    private String deviceRef;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Transaction occurrence timestamp is required")
    private Instant occurredAt;


    public CreateTransactionRequest() {
    }

    public String getCustomerRef() {
        return customerRef;
    }

    public void setCustomerRef(String customerRef) {
        this.customerRef = customerRef;
    }

    public String getMerchantRef() {
        return merchantRef;
    }

    public void setMerchantRef(String merchantRef) {
        this.merchantRef = merchantRef;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getDeviceRef() {
        return deviceRef;
    }

    public void setDeviceRef(String deviceRef) {
        this.deviceRef = deviceRef;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}

