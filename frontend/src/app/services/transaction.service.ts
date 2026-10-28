// Create the Angular API service - check CORS errors

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type CurrencyCode = 'ZAR' | 'USD' | 'EUR' | 'GBP';
export type TransactionStatus = 'PENDING' | 'COMPLETED' | 'FAILED';

export interface Transaction {
    id: number;
    reference: string;
    description: string;
    amount: number;
    currency: CurrencyCode;
    status: TransactionStatus;
    createdAt: string;
    updatedAt: string;
}

export interface CreateTransactionRequest {
    description: string;
    amount: number;
    currency: CurrencyCode;
    idempotencyKey: string;
}

@Injectable({ providedIn: 'root' })
export class TransactionService {
    private readonly http = inject(HttpClient);
    private readonly apiUrl = '/api/transactions';

    getTransactions(): Observable<Transaction[]> {
        return this.http.get<Transaction[]>(this.apiUrl);
    }

    createTransaction(
        request: CreateTransactionRequest,
    ): Observable<Transaction> {
        return this.http.post<Transaction>(this.apiUrl, request);
    }
}