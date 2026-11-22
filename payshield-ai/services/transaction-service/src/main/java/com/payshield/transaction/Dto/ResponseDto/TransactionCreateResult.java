package com.payshield.transaction.Dto.ResponseDto;

public record TransactionCreateResult(
        TransactionResponse transaction,
        boolean created
) {
}