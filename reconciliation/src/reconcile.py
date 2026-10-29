# Reconciliation logic implementation
# Matched, Mismatched, Missing external, Missing internal and Validation
# Python Decimal - best use for financial comparisons

from decimal import Decimal, InvalidOperation
from typing import Any, Iterable, Mapping


TransactionRecord = Mapping[str, Any]
SUPPORTED_CURRENCIES = {"ZAR", "USD", "EUR", "GBP"}


def reconcile_transactions(
    internal_transactions: Iterable[TransactionRecord],
    external_transactions: Iterable[TransactionRecord],
) -> dict[str, list[dict[str, Any]]]:
    """Compare internal transactions with an external settlement record set.

    Each transaction must contain:
    - reference: a unique transaction reference
    - amount: a valid decimal amount
    - currency: a currency code such as ZAR or USD

    Amounts are compared using Decimal to avoid floating-point inaccuracies.
    """
    internal_by_reference = _index_transactions(internal_transactions)
    external_by_reference = _index_transactions(external_transactions)

    internal_references = set(internal_by_reference)
    external_references = set(external_by_reference)

    matched = []
    mismatched = []

    for reference in sorted(internal_references & external_references):
        internal = internal_by_reference[reference]
        external = external_by_reference[reference]

        internal_amount = Decimal(str(internal["amount"]))
        external_amount = Decimal(str(external["amount"]))

        if (
            internal_amount == external_amount
            and internal["currency"] == external["currency"]
        ):
            matched.append(
                {
                    "reference": reference,
                    "amount": str(internal_amount),
                    "currency": internal["currency"],
                }
            )
        else:
            mismatched.append(
                {
                    "reference": reference,
                    "internal_amount": str(internal_amount),
                    "external_amount": str(external_amount),
                    "internal_currency": internal["currency"],
                    "external_currency": external["currency"],
                }
            )

    missing_external = [
        internal_by_reference[reference]
        for reference in sorted(internal_references - external_references)
    ]

    missing_internal = [
        external_by_reference[reference]
        for reference in sorted(external_references - internal_references)
    ]

    return {
        "matched": matched,
        "mismatched": mismatched,
        "missing_external": missing_external,
        "missing_internal": missing_internal,
    }


def _index_transactions(
    transactions: Iterable[TransactionRecord],
) -> dict[str, TransactionRecord]:
    """Index transactions and reject invalid or duplicate references."""
    indexed = {}

    for transaction in transactions:
        reference = transaction.get("reference")

        if not isinstance(reference, str) or not reference.strip():
            raise ValueError("Every transaction must have a non-empty reference.")

        if reference in indexed:
            raise ValueError(f"Duplicate transaction reference: {reference}")

        if "amount" not in transaction or "currency" not in transaction:
            raise ValueError(
                f"Transaction {reference} must include amount and currency."
            )

        try:
            amount = Decimal(str(transaction["amount"]))
        except (InvalidOperation, ValueError):
            raise ValueError(
                f"Transaction {reference} has an invalid amount."
            ) from None

        if not amount.is_finite():
            raise ValueError(
                f"Transaction {reference} must have a finite amount."
            )

        # Ensure that 0, negative numbers, NaN and infinity cannot pass validation
        if amount <= 0:
            raise ValueError(
                f"Transaction {reference} amount must be greater than zero."
            )

        # Requires exact currency supported code
        currency = transaction["currency"]

        if not isinstance(currency, str) or currency not in SUPPORTED_CURRENCIES:
            raise ValueError(
                f"Transaction {reference} has an unsupported currency: {currency!r}."
            )

        indexed[reference] = {
            **transaction,
            "amount": amount,
            "currency": currency,
        }

    return indexed
