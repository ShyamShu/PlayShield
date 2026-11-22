
package com.payshield.transaction.Exceptions;

public class TransactionNotFoundException
        extends RuntimeException {

    public TransactionNotFoundException(String message) {
        super(message);
    }
}
