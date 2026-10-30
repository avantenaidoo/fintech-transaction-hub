// Unit test for transaction.service.ts


import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
    HttpTestingController,
    provideHttpClientTesting,
} from '@angular/common/http/testing';

import {
    CreateTransactionRequest,
    Transaction,
    TransactionService,
} from './transaction.service';

describe('TransactionService', () => {
    let service: TransactionService;
    let httpTestingController: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                TransactionService,
                provideHttpClient(),
                provideHttpClientTesting(),
            ],
        });

        service = TestBed.inject(TransactionService);
        httpTestingController = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpTestingController.verify();
    });

    const mockTransaction: Transaction = {
        id: 1,
        reference: 'txn_test_001',
        description: 'Test transaction',
        amount: 100.00,
        currency: 'ZAR',
        status: 'PENDING',
        createdAt: '2026-10-30T10:00:00Z',
        updatedAt: '2026-10-30T10:00:00Z',
    };

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should send a GET request to retrieve transactions', () => {
        service.getTransactions().subscribe();

        const request = httpTestingController.expectOne('/api/transactions');

        expect(request.request.method).toBe('GET');

        request.flush([mockTransaction]);
    });

    it('should return the retrieved transactions', () => {
        let actualTransactions: Transaction[] | undefined;

        service.getTransactions().subscribe((transactions) => {
            actualTransactions = transactions;
        });

        const request = httpTestingController.expectOne('/api/transactions');

        request.flush([mockTransaction]);

        expect(actualTransactions).toEqual([mockTransaction]);
        expect(actualTransactions?.length).toBe(1);
    });

    it('should send a POST request with the transaction details', () => {
        const requestPayload: CreateTransactionRequest = {
            description: 'Test transaction',
            amount: 100.00,
            currency: 'ZAR',
            idempotencyKey: 'test-key-001',
        };

        service.createTransaction(requestPayload).subscribe();

        const request = httpTestingController.expectOne('/api/transactions');

        expect(request.request.method).toBe('POST');
        expect(request.request.body).toEqual(requestPayload);

        request.flush(mockTransaction);
    });

    it('should return the created transaction', () => {
        const requestPayload: CreateTransactionRequest = {
            description: 'Test transaction',
            amount: 100.00,
            currency: 'ZAR',
            idempotencyKey: 'test-key-001',
        };

        let actualTransaction: Transaction | undefined;

        service.createTransaction(requestPayload).subscribe((transaction) => {
            actualTransaction = transaction;
        });

        const request = httpTestingController.expectOne('/api/transactions');

        request.flush(mockTransaction);

        expect(actualTransaction).toEqual(mockTransaction);
        expect(actualTransaction?.reference).toBe('txn_test_001');
    });

    it('should propagate HTTP errors when retrieving transactions', () => {
        let receivedError: unknown;

        service.getTransactions().subscribe({
            error: (error) => {
                receivedError = error;
            },
        });

        const request = httpTestingController.expectOne('/api/transactions');

        request.flush(
            { message: 'Failed to retrieve transactions' },
            {
                status: 500,
                statusText: 'Internal Server Error',
            },
        );

        expect(receivedError).toBeTruthy();
        expect(receivedError).toMatchObject({
            status: 500,
        });
    });
});
