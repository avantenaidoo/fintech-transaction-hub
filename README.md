# Fintech Transaction Hub

A full-stack fintech transaction management and settlement reconciliation application built with Angular, Java Spring Boot, PostgreSQL, and Python.

## Overview# Fintech Transaction Hub

A full-stack fintech transaction management and settlement reconciliation application built with Angular, Java Spring Boot, PostgreSQL, and Python.

**[View Live Demo](https://fintech-transaction-hub-pi.vercel.app/)**

## Overview

Fintech Transaction Hub is a demonstration of a financial transaction processing system designed to manage internal transactions and reconcile them against external settlement records.

The application provides a central dashboard for creating and viewing transactions, validating financial data, preventing duplicate transaction submissions, and identifying discrepancies during settlement reconciliation.

## Key Features

- **Transaction Management:** Create and retrieve financial transactions.
- **Transaction Validation:** Validate transaction descriptions, amounts, and supported currencies.
- **Idempotency Protection:** Prevent duplicate transactions from being created through repeated requests.
- **Transaction Statuses:** Support pending, completed, and failed transaction statuses.
- **Settlement Reconciliation:** Compare internal PostgreSQL transaction records against external CSV settlement data.
- **Discrepancy Detection:** Identify matched, mismatched, missing external, and missing internal transactions.
- **REST API:** Integrate the Angular frontend with the Spring Boot backend.
- **Automated Testing:** Test application functionality, business logic, and reconciliation processing.

## Technology Stack

| Component | Technology |
|---|---|
| Frontend | Angular 22 |
| Styling | Tailwind CSS 4 |
| Backend | Java 27, Spring Boot 4 |
| Database | PostgreSQL |
| Reconciliation Engine | Python |
| Frontend Testing | Vitest |
| Backend Testing | JUnit 5, Mockito |
| Python Testing | Python unittest |

## Architecture

The application uses a modular full-stack architecture:

1. **Angular** provides the transaction management dashboard and user interface.
2. **Spring Boot** exposes REST APIs and handles transaction processing and business logic.
3. **PostgreSQL** stores internal transaction records.
4. **Python** performs settlement reconciliation against external CSV records.
5. Reconciliation results are returned to the Angular dashboard through the backend API.

### Deployment Architecture

| Component | Hosting Platform |
|---|---|
| Angular Frontend | Vercel |
| Spring Boot Backend and Python | Render Web Service |
| PostgreSQL Database | Render Managed PostgreSQL |

## Reconciliation Results

The reconciliation engine identifies four possible outcomes:

- **Matched:** Internal and external transaction records agree.
- **Mismatched:** A transaction exists in both records, but its amount or currency differs.
- **Missing External:** An internal transaction has no corresponding external settlement record.
- **Missing Internal:** An external settlement record has no corresponding internal transaction.

The current demonstration uses a bundled sample CSV settlement file.

## Project Scope

This project is a working fintech demonstration focused on transaction management, financial data validation, transaction integrity, and settlement reconciliation.

It does not process real payments, connect to banking institutions, or perform foreign exchange conversions.

The public demonstration uses a shared database, meaning transactions created by visitors are visible to other visitors. Only fictional demonstration data should be used.

## Testing

The project has 68 passing automated tests across the frontend, backend, and Python reconciliation module.

## Project Status

The full-stack demonstration is implemented and deployed.

- Angular frontend hosted on Vercel.
- Spring Boot REST API hosted on Render.
- PostgreSQL managed database hosted on Render.
- Python reconciliation engine integrated with the backend.
- CSV-based settlement reconciliation supporting all four discrepancy outcomes.
- 68 automated tests passing across the frontend, backend, and Python reconciliation module.

Fintech Transaction Hub is a demonstration of a financial transaction processing system designed to manage internal transactions and reconcile them against external settlement records.

The application provides a central dashboard for creating and viewing transactions, validating financial data, preventing duplicate transaction submissions, and identifying discrepancies during settlement reconciliation.

## Key Features

- **Transaction Management:** Create and retrieve financial transactions.
- **Transaction Validation:** Validate transaction descriptions, amounts, and supported currencies.
- **Idempotency Protection:** Prevent duplicate transactions from being created through repeated requests.
- **Transaction Statuses:** Support pending, completed, and failed transaction statuses.
- **Settlement Reconciliation:** Compare internal PostgreSQL transaction records against external CSV settlement data.
- **Discrepancy Detection:** Identify matched, mismatched, missing external, and missing internal transactions.
- **REST API:** Integrate the Angular frontend with the Spring Boot backend.
- **Automated Testing:** Test application functionality, business logic, and reconciliation processing.

## Technology Stack

| Component | Technology |
|---|---|
| Frontend | Angular 22 |
| Styling | Tailwind CSS 4 |
| Backend | Java 27, Spring Boot 4 |
| Database | PostgreSQL |
| Reconciliation Engine | Python |
| Frontend Testing | Vitest |
| Backend Testing | JUnit 5, Mockito |
| Python Testing | Python unittest |

## Architecture

The application uses a modular full-stack architecture:

1. Angular provides the transaction management dashboard and user interface.
2. Spring Boot exposes REST APIs and handles transaction processing and business logic.
3. PostgreSQL stores internal transaction records.
4. Python performs settlement reconciliation against external CSV records.
5. Reconciliation results are returned to the Angular dashboard through the backend API.

## Reconciliation Results

The reconciliation engine identifies four possible outcomes:

- **Matched:** Internal and external transaction records agree.
- **Mismatched:** A transaction exists in both records, but its amount or currency differs.
- **Missing External:** An internal transaction has no corresponding external settlement record.
- **Missing Internal:** An external settlement record has no corresponding internal transaction.

## Project Scope

This project is a working fintech demonstration focused on transaction management, financial data validation, transaction integrity, and settlement reconciliation.

It does not process real payments, connect to banking institutions, or perform foreign exchange conversions.

## Testing

The current implementation has 68 passing automated tests across the frontend, backend, and Python reconciliation module.

## Project Status

The basic working full-stack demonstration is implemented, with Angular, Spring Boot, PostgreSQL, and Python integrated and the four reconciliation outcomes verified end to end.