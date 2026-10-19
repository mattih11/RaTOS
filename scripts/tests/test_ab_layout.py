import copy
import importlib.util
import json
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


spec = importlib.util.spec_from_file_location(
    "ab_layout", Path(__file__).resolve().parents[1] / "check-musicrat-ab-layout.py"
)
layout = importlib.util.module_from_spec(spec)
spec.loader.exec_module(layout)


class LayoutTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.image = Path(self.directory.name) / "test.wic"
        partitions = []
        start = 2048
        for index, (name, mib) in enumerate(zip(
            ["efi", "BOOT0", "BOOT1", "systema", "systemb", "var"],
            [16, 128, 128, 2048, 2048, 512],
        )):
            size = mib * 2048
            partitions.append(
                {"name": name, "start": start, "size": size, "uuid": str(index)}
            )
            start += size
        self.table = {
            "label": "gpt", "unit": "sectors", "sectorsize": 512,
            "partitions": partitions,
        }
        with self.image.open("wb") as stream:
            stream.truncate(start * 512)
            stream.seek(partitions[3]["start"] * 512)
            stream.write(b"hsqs")

    def check(self, table=None):
        output = subprocess.CompletedProcess(
            [], 0, stdout=json.dumps({"partitiontable": table or self.table})
        )
        with patch.object(layout.subprocess, "run", return_value=output):
            layout.check_layout(self.image)

    def test_accepts_initial_ab_layout(self):
        self.check()

    def test_rejects_single_root(self):
        self.table["partitions"] = self.table["partitions"][:3]
        with self.assertRaises(ValueError):
            self.check()

    def test_rejects_wrong_capacity_or_duplicate_uuid(self):
        for field, value in [("size", 4096), ("uuid", "0")]:
            table = copy.deepcopy(self.table)
            table["partitions"][4][field] = value
            with self.subTest(field=field), self.assertRaises(ValueError):
                self.check(table)

    def test_rejects_missing_initial_root(self):
        with self.image.open("r+b") as stream:
            stream.seek(self.table["partitions"][3]["start"] * 512)
            stream.write(b"\0\0\0\0")
        with self.assertRaises(ValueError):
            self.check()

    def test_rejects_dirty_inactive_header(self):
        with self.image.open("r+b") as stream:
            stream.seek(self.table["partitions"][4]["start"] * 512)
            stream.write(b"old-root")
        with self.assertRaises(ValueError):
            self.check()


if __name__ == "__main__":
    unittest.main()
