#!/usr/bin/env python3
"""Reject single-root or incompatible disks before offering them for A/B installation."""

import argparse
import json
import struct
import subprocess
from pathlib import Path


def check_layout(image: Path) -> None:
    result = subprocess.run(
        ["sfdisk", "--json", str(image)], check=True, capture_output=True, text=True
    )
    table = json.loads(result.stdout)["partitiontable"]
    if table["label"] != "gpt" or table["unit"] != "sectors":
        raise ValueError("expected a GPT disk with sector-based offsets")
    partitions = table["partitions"]
    names = ["efi", "BOOT0", "BOOT1", "systema", "systemb", "var"]
    if [part.get("name") for part in partitions] != names:
        raise ValueError(f"expected exactly these partitions in order: {names}")

    sector_size = table["sectorsize"]
    expected_sizes = [16, 128, 128, 2048, 2048, 512]
    end = 0
    uuids = set()
    for part, size_mib in zip(partitions, expected_sizes):
        if part["size"] * sector_size != size_mib * 1024 * 1024:
            raise ValueError(f"{part['name']}: expected {size_mib} MiB")
        start = part["start"] * sector_size
        if start < end:
            raise ValueError(f"{part['name']}: overlapping partition")
        end = start + part["size"] * sector_size
        if not part.get("uuid") or part["uuid"] in uuids:
            raise ValueError("partition UUIDs must be present and distinct")
        uuids.add(part["uuid"])
    if end > image.stat().st_size:
        raise ValueError("partition extends beyond the disk image")

    with image.open("rb") as stream:
        stream.seek(partitions[3]["start"] * sector_size)
        magic, = struct.unpack("<I", stream.read(4))
        if magic != 0x73717368:
            raise ValueError("systema does not contain the initial SquashFS root")
        stream.seek(partitions[4]["start"] * sector_size)
        if any(stream.read(5 * 1024 * 1024)):
            raise ValueError("the initial systemb header must be empty")
    print(f"A/B layout verified: {image} (two 2 GiB roots, dual boot slots, persistent /var)")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("image", type=Path, help="uncompressed initial MusicRaT A/B .wic")
    args = parser.parse_args()
    try:
        check_layout(args.image)
    except subprocess.CalledProcessError as error:
        parser.exit(1, f"A/B partition inspection failed: {error.stderr}\n")
    except (OSError, ValueError, KeyError) as error:
        parser.exit(1, f"A/B layout validation failed: {error}\n")


if __name__ == "__main__":
    main()
