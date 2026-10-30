import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { App } from './app';

describe('App', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);

  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    httpTesting.expectOne('/api/transactions').flush([]);

    expect(fixture.componentInstance).toBeTruthy();
    fixture.destroy();

  });

  it('should render the Transaction Hub dashboard title', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    httpTesting.expectOne('/api/transactions').flush([]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('h1')?.textContent?.trim()).toBe(
      'Transactions',
    );

    fixture.destroy();

  });

  it('should display an error when loading transactions fails', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    httpTesting.expectOne('/api/transactions').flush('Server unavailable', {
      status: 500,
      statusText: 'Internal Server Error',
    });

    fixture.detectChanges();

    expect(fixture.componentInstance.errorMessage).toBe(
      'Could not load transactions. Check that the backend is running.',
    );

    fixture.destroy();

  });

  // Reconciliation

  it('should run reconciliation and display all four result categories', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    httpTesting.expectOne('/api/transactions').flush([]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const reconciliationButton = Array.from(
      compiled.querySelectorAll('button'),
    ).find((button) =>
      button.textContent?.toLowerCase().includes('reconcil'),
    );

    expect(reconciliationButton).toBeTruthy();

    reconciliationButton!.click();
    fixture.detectChanges();

    const request = httpTesting.expectOne('/api/reconciliation');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});

    request.flush({
      matched: [
        {
          reference: 'txn_matched_001',
          amount: '123.45',
          currency: 'ZAR',
        },
      ],
      mismatched: [
        {
          reference: 'txn_mismatched_001',
          internal_amount: '200.00',
          external_amount: '180.00',
          internal_currency: 'ZAR',
          external_currency: 'ZAR',
        },
      ],
      missing_external: [
        {
          reference: 'txn_missing_external_001',
          amount: '95.00',
          currency: 'ZAR',
        },
      ],
      missing_internal: [
        {
          reference: 'txn_missing_internal_001',
          amount: '75.00',
          currency: 'EUR',
        },
      ],
    });

    fixture.detectChanges();

    expect(fixture.componentInstance.reconciliationResult).toBeTruthy();

    expect(compiled.textContent).toContain('txn_matched_001');
    expect(compiled.textContent).toContain('txn_mismatched_001');
    expect(compiled.textContent).toContain('txn_missing_external_001');
    expect(compiled.textContent).toContain('txn_missing_internal_001');

    expect(fixture.componentInstance.reconciling).toBe(false);
    expect(fixture.componentInstance.lastReconciledAt).toBeInstanceOf(Date);

    fixture.destroy();
  });

  it('should display an error when reconciliation fails', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    httpTesting.expectOne('/api/transactions').flush([]);
    fixture.detectChanges();

    fixture.componentInstance.runReconciliation();

    const request = httpTesting.expectOne('/api/reconciliation');

    request.flush('Reconciliation failed', {
      status: 500,
      statusText: 'Internal Server Error',
    });

    fixture.detectChanges();

    expect(fixture.componentInstance.reconciliationError).toBe(
      'Could not complete reconciliation. Please try again.',
    );
    expect(fixture.componentInstance.reconciling).toBe(false);

    fixture.destroy();
  });

  it('should prevent duplicate reconciliation requests while one is running', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    httpTesting.expectOne('/api/transactions').flush([]);

    fixture.componentInstance.runReconciliation();
    fixture.componentInstance.runReconciliation();

    const request = httpTesting.expectOne('/api/reconciliation');

    expect(request.request.method).toBe('POST');

    request.flush({
      matched: [],
      mismatched: [],
      missing_external: [],
      missing_internal: [],
    });

    httpTesting.expectNone('/api/reconciliation');

    fixture.destroy();
  });
});
