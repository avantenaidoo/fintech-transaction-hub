// Rest controller connecting HTTP requests from Angular to existing TransactionService
// Controller with POST /api/transactions & GET /api/transaction/{id}
package com.fintechhub.transactionapi.transaction;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    // Removed controller dependency on TransactionRepository
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(
            // @valid triggers the validation rules in the request DTO
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        Transaction transaction = transactionService.createTransaction(
                request.description(),
                request.amount(),
                request.currency(),
                request.idempotencyKey()
        );

        // Return the response and not the JPA entity
        return TransactionResponse.from(transaction);
    }

    // Return existing response DTO and idempotency remains private

    @GetMapping
    public List<TransactionResponse> getTransactions() {
        return transactionService.getRecentTransactions()
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TransactionResponse getTransaction(@PathVariable Long id) {
        Transaction transaction = transactionService.getTransactionById(id);

        return TransactionResponse.from(transaction);
    }
}