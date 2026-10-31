
package com.payshield.transaction.Entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_transactions_customer_ref",
               columnList = "customer_ref"),
        @Index(name = "idx_transactions_merchant_ref",
               columnList = "merchant_ref"),
        @Index(name = "idx_transactions_occurred_at",
               columnList = "occurred_at"),
        @Index(name = "idx_transactions_payment_status",
               columnList = "payment_status")
    }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_ref", nullable = false, length = 64)
    private String customerRef;

    @Column(name = "merchant_ref", nullable = false, length = 64)
    private String merchantRef;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "bpchar(3)")
    private String currency;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "device_ref", length = 64)
    private String deviceRef;

    @Column(name = "description", length = 500)
    private String description;                                


    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "submitted_by", nullable = false, updatable = false)
    private UUID submittedBy;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false,
            length = 64, columnDefinition = "bpchar(64)")
    private String requestHash;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;


    public Transaction() {
}

   

    public UUID getId() {
       return id;
    }

   public String getCustomerRef() {
       return customerRef;
    }

    public String getMerchantRef() {
       return merchantRef;
    }

    public BigDecimal getAmount() {
       return amount;
    }

    public String getCurrency() {
       return currency;
    }

    public String getTransactionType() {
       return transactionType;
    }

    public String getPaymentStatus() {
       return paymentStatus;
    }

    public String getChannel() {
       return channel;
    }

    public String getDeviceRef() {
       return deviceRef;
    }

    public String getDescription() {
       return description;
    }

    public Instant getOccurredAt() {
       return occurredAt;
    }

    public Instant getCreatedAt() {
       return createdAt;
    }

    public UUID getSubmittedBy() {
       return submittedBy;
    }

    public void setId(UUID id) {
       this.id = id;
}

    public void setCustomerRef(String customerRef) {
       this.customerRef = customerRef;
    }

    public void setMerchantRef(String merchantRef) {
       this.merchantRef = merchantRef;
    }

    public void setAmount(BigDecimal amount) {
       this.amount = amount;
    }

    public void setCurrency(String currency) {
       this.currency = currency;
    }

    public void setTransactionType(String transactionType) {
       this.transactionType = transactionType;
    }

    public void setPaymentStatus(String paymentStatus) {
       this.paymentStatus = paymentStatus;
    }

    public void setChannel(String channel) {
       this.channel = channel;
    }

    public void setDeviceRef(String deviceRef) {
       this.deviceRef = deviceRef;
    }

    public void setDescription(String description) {
       this.description = description;
    }

    public void setCreatedAt(Instant createdAt) {
       this.createdAt = createdAt;
    }

    public void setSubmittedBy(UUID submittedBy) {
       this.submittedBy = submittedBy;
    }

    public void setIdempotencyKey(String idempotencyKey) {
       this.idempotencyKey = idempotencyKey;
    }

    public void setRequestHash(String requestHash) {
       this.requestHash = requestHash;
    }

    public void setOccurredAt(Instant occurredAt) {
       this.occurredAt = occurredAt;
    }

    public String getIdempotencyKey() {
       return idempotencyKey;
    }

    public String getRequestHash() {
       return requestHash;
    }
}
