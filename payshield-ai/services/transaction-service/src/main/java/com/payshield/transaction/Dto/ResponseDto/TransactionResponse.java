package com.payshield.transaction.Dto.ResponseDto;



import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.payshield.transaction.Entity.Transaction;

public class TransactionResponse {

    private UUID id;

    private String customerRef;

    private String merchantRef;

    private BigDecimal amount;

    private String currency;

    private String transactionType;

    private String paymentStatus;

    private String channel;

    private String deviceRef;

    private String description;

    private Instant createdAt;

    private UUID submittedBy;

    private Instant occurredAt;


    public TransactionResponse() {
    }

    // Convert Entity into Response DTO
    public static TransactionResponse fromEntity(Transaction transaction) {

        TransactionResponse response = new TransactionResponse();

        response.setId(transaction.getId());
        response.setCustomerRef(transaction.getCustomerRef());
        response.setMerchantRef(transaction.getMerchantRef());
        response.setAmount(transaction.getAmount());
        response.setCurrency(transaction.getCurrency());
        response.setTransactionType(transaction.getTransactionType());
        response.setPaymentStatus(transaction.getPaymentStatus());
        response.setChannel(transaction.getChannel());
        response.setDeviceRef(transaction.getDeviceRef());
        response.setDescription(transaction.getDescription());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setSubmittedBy(transaction.getSubmittedBy());
        response.setOccurredAt(transaction.getOccurredAt());

        return response;
    }


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(UUID submittedBy) {
        this.submittedBy = submittedBy;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}

