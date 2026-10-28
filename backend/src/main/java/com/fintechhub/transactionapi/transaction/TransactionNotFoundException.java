// The Exception when someone requests a transaction ID that does not exist

package com.fintechhub.transactionapi.transaction;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(Long id) {
        super("Transaction not found with ID: " + id);
    }
}