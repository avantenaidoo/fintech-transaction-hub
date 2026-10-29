# Python tests CLI

import csv
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from reconciliation.src.cli import (
    load_settlement_csv,
    run_reconciliation,
)


PROJECT_ROOT = Path(__file__).resolve().parents[2]


class TestReconciliationCli(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp_dir.cleanup)
        self.csv_path = Path(self.temp_dir.name) / "settlement.csv"

        self.internal = [
            {"reference": "txn-001", "amount": "100.50", "currency": "ZAR"},
            {"reference": "txn-002", "amount": "250.00", "currency": "ZAR"},
            {"reference": "txn-003", "amount": "75.00", "currency": "USD"},
        ]

        with self.csv_path.open("w", encoding="utf-8", newline="") as file:
            writer = csv.writer(file)
            writer.writerow(["reference", "amount", "currency"])
            writer.writerow(["txn-001", "100.50", "ZAR"])
            writer.writerow(["txn-002", "255.00", "ZAR"])
            writer.writerow(["txn-004", "30.00", "EUR"])

    def test_loads_settlement_csv(self):
        records = load_settlement_csv(self.csv_path)
        self.assertEqual(len(records), 3)
        self.assertEqual(records[0]["reference"], "txn-001")

    def test_reconciles_internal_and_external_records(self):
        results = run_reconciliation(self.internal, self.csv_path)
        self.assertEqual(len(results["matched"]), 1)
        self.assertEqual(len(results["mismatched"]), 1)
        self.assertEqual(len(results["missing_external"]), 1)
        self.assertEqual(len(results["missing_internal"]), 1)

    def test_rejects_missing_csv(self):
        with self.assertRaisesRegex(ValueError, "file not found"):
            load_settlement_csv(
                Path(self.temp_dir.name) / "does-not-exist.csv"
            )

    def test_rejects_missing_csv_columns(self):
        bad_csv = Path(self.temp_dir.name) / "invalid.csv"
        bad_csv.write_text(
            "reference,amount\ntxn-001,10.00\n",
            encoding="utf-8",
        )

        with self.assertRaisesRegex(ValueError, "missing required columns"):
            load_settlement_csv(bad_csv)

    def test_cli_emits_json_and_success_exit_code(self):
        process = subprocess.run(
            [
                sys.executable,
                "-m",
                "reconciliation.src.cli",
                "--settlement-file",
                str(self.csv_path),
            ],
            input=json.dumps(self.internal),
            text=True,
            capture_output=True,
            cwd=PROJECT_ROOT,
            check=False,
        )

        self.assertEqual(process.returncode, 0, process.stderr)
        results = json.loads(process.stdout)
        self.assertEqual(len(results["matched"]), 1)
        self.assertEqual(len(results["mismatched"]), 1)

    def test_cli_reports_invalid_json(self):
        process = subprocess.run(
            [
                sys.executable,
                "-m",
                "reconciliation.src.cli",
                "--settlement-file",
                str(self.csv_path),
            ],
            input="{invalid json",
            text=True,
            capture_output=True,
            cwd=PROJECT_ROOT,
            check=False,
        )

        self.assertEqual(process.returncode, 1)
        self.assertIn("Reconciliation error:", process.stderr)
        self.assertEqual(process.stdout, "")


if __name__ == "__main__":
    unittest.main()
