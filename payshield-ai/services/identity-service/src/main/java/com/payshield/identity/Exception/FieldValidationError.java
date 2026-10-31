package com.payshield.identity.Exception;

public record FieldValidationError(
        String field,
        String message
) {
}
