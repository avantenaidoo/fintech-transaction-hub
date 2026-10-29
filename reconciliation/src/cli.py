# Python CLI

"""Command-line interface for transaction settlement reconciliation."""

import argparse
import csv
import json
import sys
from pathlib import Path
from typing import Any

from reconciliation.src.reconcile import reconcile_transactions


REQUIRED_CSV_COLUMNS = {"reference", "amount", "currency"}


def load_settlement_csv(file_path: Path) -> list[dict[str, str]]:
    """Load external settlement transactions from a CSV file."""
    try:
        with file_path.open("r", encoding="utf-8-sig", newline="") as file:
            reader = csv.DictReader(file)

            if not reader.fieldnames:
                raise ValueError("Settlement CSV is empty or has no header.")

            original_headers = reader.fieldnames
            normalized_headers = [header.strip() for header in original_headers]

            if len(normalized_headers) != len(set(normalized_headers)):
                raise ValueError("Settlement CSV contains duplicate column names.")

            missing_columns = REQUIRED_CSV_COLUMNS - set(normalized_headers)
            if missing_columns:
                missing = ", ".join(sorted(missing_columns))
                raise ValueError(
                    f"Settlement CSV is missing required columns: {missing}."
                )

            reader.fieldnames = normalized_headers
            transactions = []

            for row in reader:
                if not row or all(
                    value is None or not value.strip()
                    for value in row.values()
                ):
                    continue

                if None in row:
                    raise ValueError(
                        "Settlement CSV contains a row with too many columns."
                    )

                transaction = {
                    key: value.strip() if value is not None else ""
                    for key, value in row.items()
                }
                transactions.append(transaction)

            return transactions

    except FileNotFoundError:
        raise ValueError(
            f"Settlement CSV file not found: {file_path}"
        ) from None
    except UnicodeDecodeError:
        raise ValueError("Settlement CSV must use UTF-8 encoding.") from None


def run_reconciliation(
    internal_transactions: list[dict[str, Any]],
    settlement_file: Path,
) -> dict[str, list[dict[str, Any]]]:
    """Reconcile internal JSON records against an external settlement CSV."""
    external_transactions = load_settlement_csv(settlement_file)
    return reconcile_transactions(internal_transactions, external_transactions)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Reconcile internal transactions against a settlement CSV."
    )
    parser.add_argument(
        "--settlement-file",
        required=True,
        type=Path,
        help="Path to the external settlement CSV file.",
    )
    args = parser.parse_args()

    try:
        internal_transactions = json.load(sys.stdin)

        if not isinstance(internal_transactions, list):
            raise ValueError(
                "Standard input must contain a JSON array of transactions."
            )

        if not all(isinstance(item, dict) for item in internal_transactions):
            raise ValueError(
                "Every internal transaction must be a JSON object."
            )

        results = run_reconciliation(
            internal_transactions,
            args.settlement_file,
        )

        json.dump(results, sys.stdout, default=str)
        sys.stdout.write("\n")
        return 0

    except (ValueError, OSError) as exc:
        print(f"Reconciliation error: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
