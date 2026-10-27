// Spring Data JPA generates database queries from the method names
package com.fintechhub.transactionapi.transaction;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}