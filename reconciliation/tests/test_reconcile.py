# Adding automated tests


import unittest

from reconciliation.src.reconcile import reconcile_transactions


class TestReconcileTransactions(unittest.TestCase):
    def setUp(self):
        self.internal = [
            {"reference": "txn-001", "amount": "100.50", "currency": "ZAR"},
            {"reference": "txn-002", "amount": "250.00", "currency": "ZAR"},
            {"reference": "txn-003", "amount": "75.00", "currency": "USD"},
        ]

        self.external = [
            {"reference": "txn-001", "amount": "100.50", "currency": "ZAR"},
            {"reference": "txn-002", "amount": "255.00", "currency": "ZAR"},
            {"reference": "txn-004", "amount": "30.00", "currency": "EUR"},
        ]

    def test_matches_equal_amount_and_currency(self):
        result = reconcile_transactions(
            self.internal,
            self.external,
        )

        self.assertEqual(
            result["matched"],
            [
                {
                    "reference": "txn-001",
                    "amount": "100.50",
                    "currency": "ZAR",
                }
            ],
        )

    def test_detects_amount_mismatch(self):
        result = reconcile_transactions(
            self.internal,
            self.external,
        )

        self.assertEqual(
            result["mismatched"],
            [
                {
                    "reference": "txn-002",
                    "internal_amount": "250.00",
                    "external_amount": "255.00",
                    "internal_currency": "ZAR",
                    "external_currency": "ZAR",
                }
            ],
        )

    def test_detects_currency_mismatch(self):
        internal = [
            {"reference": "txn-001", "amount": "100.00", "currency": "ZAR"}
        ]
        external = [
            {"reference": "txn-001", "amount": "100.00", "currency": "USD"}
        ]

        result = reconcile_transactions(internal, external)

        self.assertEqual(len(result["mismatched"]), 1)
        self.assertEqual(result["mismatched"][0]["internal_currency"], "ZAR")
        self.assertEqual(result["mismatched"][0]["external_currency"], "USD")

    def test_detects_transactions_missing_from_external(self):
        result = reconcile_transactions(
            self.internal,
            self.external,
        )

        self.assertEqual(
            [item["reference"] for item in result["missing_external"]],
            ["txn-003"],
        )

    def test_detects_transactions_missing_from_internal(self):
        result = reconcile_transactions(
            self.internal,
            self.external,
        )

        self.assertEqual(
            [item["reference"] for item in result["missing_internal"]],
            ["txn-004"],
        )

    def test_compares_decimal_amounts_without_float_rounding(self):
        internal = [
            {"reference": "txn-001", "amount": "0.30", "currency": "ZAR"}
        ]
        external = [
            {"reference": "txn-001", "amount": "0.30", "currency": "ZAR"}
        ]

        result = reconcile_transactions(internal, external)

        self.assertEqual(len(result["matched"]), 1)
        self.assertEqual(result["mismatched"], [])

    def test_rejects_duplicate_references(self):
        duplicate_transactions = [
            {"reference": "txn-001", "amount": "10.00", "currency": "ZAR"},
            {"reference": "txn-001", "amount": "20.00", "currency": "ZAR"},
        ]

        with self.assertRaisesRegex(ValueError, "Duplicate transaction reference"):
            reconcile_transactions(duplicate_transactions, [])

    def test_rejects_missing_amount_or_currency(self):
        invalid_transactions = [
            {"reference": "txn-001", "amount": "10.00"}
        ]

        with self.assertRaisesRegex(ValueError, "include amount and currency"):
            reconcile_transactions(invalid_transactions, [])

    def test_rejects_invalid_amount(self):
        invalid_transactions = [
            {"reference": "txn-001", "amount": "not-a-number", "currency": "ZAR"}
        ]

        with self.assertRaisesRegex(ValueError, "invalid amount"):
            reconcile_transactions(invalid_transactions, [])

    def test_rejects_non_finite_amount(self):
        invalid_transactions = [
            {"reference": "txn-001", "amount": "NaN", "currency": "ZAR"}
        ]

        with self.assertRaisesRegex(ValueError, "finite amount"):
            reconcile_transactions(invalid_transactions, [])

    def test_rejects_empty_reference(self):
        invalid_transactions = [
            {"reference": " ", "amount": "10.00", "currency": "ZAR"}
        ]

        with self.assertRaisesRegex(ValueError, "non-empty reference"):
            reconcile_transactions(invalid_transactions, [])


if __name__ == "__main__":
    unittest.main()
