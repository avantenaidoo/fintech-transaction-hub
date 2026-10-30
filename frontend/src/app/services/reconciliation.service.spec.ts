// Unit test for reconciliation.service.ts

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
    HttpTestingController,
    provideHttpClientTesting,
} from '@angular/common/http/testing';

import {
    ReconciliationResult,
    ReconciliationService,
} from './reconciliation.service';

describe('ReconciliationService', () => {
    let service: ReconciliationService;
    let httpTestingController: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                ReconciliationService,
                provideHttpClient(),
                provideHttpClientTesting(),
            ],
        });

        service = TestBed.inject(ReconciliationService);
        httpTestingController = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpTestingController.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should send a POST request to the reconciliation endpoint', () => {
        service.reconcile().subscribe();

        const request = httpTestingController.expectOne('/api/reconciliation');

        expect(request.request.method).toBe('POST');
        expect(request.request.body).toEqual({});

        request.flush({
            matched: [],
            mismatched: [],
            missing_external: [],
            missing_internal: [],
        });
    });

    it('should return all four reconciliation result categories', () => {
        const mockResult: ReconciliationResult = {
            matched: [
                {
                    reference: 'txn_001',
                    amount: '123.45',
                    currency: 'ZAR',
                },
            ],
            mismatched: [
                {
                    reference: 'txn_002',
                    internal_amount: '500.00',
                    external_amount: '450.00',
                    internal_currency: 'ZAR',
                    external_currency: 'ZAR',
                },
            ],
            missing_external: [
                {
                    reference: 'txn_003',
                    amount: '95.00',
                    currency: 'ZAR',
                },
            ],
            missing_internal: [
                {
                    reference: 'txn_external_001',
                    amount: '75.00',
                    currency: 'EUR',
                },
            ],
        };

        let actualResult: ReconciliationResult | undefined;

        service.reconcile().subscribe((result) => {
            actualResult = result;
        });

        const request = httpTestingController.expectOne('/api/reconciliation');

        request.flush(mockResult);

        expect(actualResult).toEqual(mockResult);
        expect(actualResult?.matched.length).toBe(1);
        expect(actualResult?.mismatched.length).toBe(1);
        expect(actualResult?.missing_external.length).toBe(1);
        expect(actualResult?.missing_internal.length).toBe(1);
    });

    it('should handle an empty reconciliation result', () => {
        const emptyResult: ReconciliationResult = {
            matched: [],
            mismatched: [],
            missing_external: [],
            missing_internal: [],
        };

        let actualResult: ReconciliationResult | undefined;

        service.reconcile().subscribe((result) => {
            actualResult = result;
        });

        const request = httpTestingController.expectOne('/api/reconciliation');

        request.flush(emptyResult);

        expect(actualResult).toEqual(emptyResult);
    });

    it('should propagate HTTP errors', () => {
        let receivedError: unknown;

        service.reconcile().subscribe({
            error: (error) => {
                receivedError = error;
            },
        });

        const request = httpTestingController.expectOne('/api/reconciliation');

        request.flush(
            { message: 'Reconciliation failed' },
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
