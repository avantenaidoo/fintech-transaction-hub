// Create Angular API service for transaction reconcilation

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type CurrencyCode = 'ZAR' | 'USD' | 'EUR' | 'GBP';

export interface ReconciliationRecord {
    reference: string;
    amount: string;
    currency: CurrencyCode;
}

export interface ReconciliationMismatch {
    reference: string;
    internal_amount: string;
    external_amount: string;
    internal_currency: CurrencyCode;
    external_currency: CurrencyCode;
}

export interface ReconciliationResult {
    matched: ReconciliationRecord[];
    mismatched: ReconciliationMismatch[];
    missing_external: ReconciliationRecord[];
    missing_internal: ReconciliationRecord[];
}

@Injectable({ providedIn: 'root' })
export class ReconciliationService {
    private readonly http = inject(HttpClient);
    private readonly apiUrl = '/api/reconciliation';

    reconcile(): Observable<ReconciliationResult> {
        return this.http.post<ReconciliationResult>(this.apiUrl, {});
    }
}
