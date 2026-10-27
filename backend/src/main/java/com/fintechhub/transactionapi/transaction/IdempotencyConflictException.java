// This will allow the REST API to return HTTP 409 conflict when a client reuses an idempotency key with different transaction details
package com.fintechhub.transactionapi.transaction;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}