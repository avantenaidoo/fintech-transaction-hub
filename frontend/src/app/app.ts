
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnInit, inject, ElementRef, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  CreateTransactionRequest,
  CurrencyCode,
  Transaction,
  TransactionService,
} from './services/transaction.service';

import {
  ReconciliationResult,
  ReconciliationService,
} from './services/reconciliation.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, DatePipe, DecimalPipe],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  private readonly transactionService = inject(TransactionService);
  private readonly reconciliationService = inject(ReconciliationService);
  private readonly cdr = inject(ChangeDetectorRef);

  transactions: Transaction[] = [];
  loading = true;
  submitting = false;
  showForm = false;

  @ViewChild('transactionForm')
  transactionForm?: ElementRef<HTMLElement>;
  errorMessage = '';
  successMessage = '';

  reconciliationResult: ReconciliationResult | null = null;
  reconciling = false;
  reconciliationError = '';
  lastReconciledAt: Date | null = null;

  description = '';
  amount: number | null = null;
  currency: CurrencyCode = 'ZAR';

  readonly currencies: CurrencyCode[] = ['ZAR', 'USD', 'EUR', 'GBP'];

  ngOnInit(): void {
    this.loadTransactions();
  }

  openTransactionForm(): void {
    this.showForm = true;

    setTimeout(() => {
      this.transactionForm?.nativeElement.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      });
    });
  }

  toggleTransactionForm(): void {
    if (this.showForm) {
      this.showForm = false;
      this.errorMessage = '';
      this.successMessage = '';
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    this.openTransactionForm();
  }

  loadTransactions(): void {
    this.loading = true;
    this.errorMessage = '';

    this.transactionService.getTransactions().subscribe({
      next: (transactions) => {
        this.transactions = transactions;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.getErrorMessage(
          error,
          'Could not load transactions. Check that the backend is running.',
        );
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  createTransaction(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const description = this.description.trim();

    if (!description || description.length > 255) {
      this.errorMessage = 'Enter a description of up to 255 characters.';
      return;
    }

    if (
      this.amount === null ||
      !Number.isFinite(this.amount) ||
      this.amount <= 0 ||
      this.amount > 999999999999999.9999 ||
      Number(this.amount.toFixed(4)) !== this.amount
    ) {
      this.errorMessage =
        'Enter a positive amount with no more than 4 decimal places.';
      return;
    }

    const request: CreateTransactionRequest = {
      description,
      amount: this.amount,
      currency: this.currency,
      idempotencyKey: crypto.randomUUID(),
    };

    this.submitting = true;

    this.transactionService.createTransaction(request).subscribe({
      next: (transaction) => {
        this.transactions = [
          transaction,
          ...this.transactions.filter((item) => item.id !== transaction.id),
        ].slice(0, 100);

        this.successMessage = `Transaction ${transaction.reference} created successfully.`;
        this.description = '';
        this.amount = null;
        this.currency = 'ZAR';
        this.submitting = false;
        this.showForm = false;
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.getErrorMessage(
          error,
          'Could not create the transaction. Please try again.',
        );
        this.submitting = false;
        this.cdr.markForCheck();
      },
    });
  }

  runReconciliation(): void {
    if (this.reconciling) {
      return;
    }

    this.reconciling = true;
    this.reconciliationError = '';

    this.reconciliationService.reconcile().subscribe({
      next: (result) => {
        this.reconciliationResult = result;
        this.lastReconciledAt = new Date();
        this.reconciling = false;
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.reconciliationError = this.getErrorMessage(
          error,
          'Could not complete reconciliation. Please try again.',
        );
        this.reconciling = false;
        this.cdr.markForCheck();
      },
    });
  }

  get totalTransactions(): number {
    return this.transactions.length;
  }

  get pendingTransactions(): number {
    return this.transactions.filter((item) => item.status === 'PENDING').length;
  }

  get completedTransactions(): number {
    return this.transactions.filter((item) => item.status === 'COMPLETED').length;
  }

  trackTransaction(_index: number, transaction: Transaction): number {
    return transaction.id;
  }

  private getErrorMessage(
    error: HttpErrorResponse,
    fallback: string,
  ): string {
    if (error.status === 0) {
      return 'Cannot reach the API. Make sure Spring Boot is running on port 8080.';
    }

    if (error.error && typeof error.error.message === 'string') {
      return error.error.message;
    }

    return fallback;
  }
}
