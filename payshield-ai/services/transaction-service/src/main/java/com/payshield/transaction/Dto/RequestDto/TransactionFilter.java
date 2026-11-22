package com.payshield.transaction.Dto.RequestDto;

import java.time.Instant;

import com.payshield.transaction.Enums.PaymentStatus;
import com.payshield.transaction.Enums.TransactionChannel;
import com.payshield.transaction.Enums.TransactionType;

public record TransactionFilter(

        String customerRef,

        String merchantRef,

        PaymentStatus paymentStatus,

        TransactionType transactionType,

        TransactionChannel channel,

        String currency,

        Instant fromDate,

        Instant toDate

) {
}