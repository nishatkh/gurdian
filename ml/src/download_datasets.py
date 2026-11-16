"""Download and verify the official UCI HAR Smartphone dataset."""

from __future__ import annotations

import hashlib
import json
import urllib.request
import zipfile
from pathlib import Path

UCI_HAR_URL = (
    "https://archive.ics.uci.edu/static/public/240/"
    "human+activity+recognition+using+smartphones.zip"
)


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def download_uci_har(base_dir: Path = Path("ml/raw/uci_har")) -> Path:
    base_dir.mkdir(parents=True, exist_ok=True)
    outer_zip = base_dir / "uci_har_bundle.zip"
    dataset_root = base_dir / "UCI HAR Dataset"

    if (dataset_root / "train" / "X_train.txt").exists():
        print(f"Dataset already present at {dataset_root}")
        return dataset_root

    print(f"Downloading UCI HAR dataset from {UCI_HAR_URL}...")
    req = urllib.request.Request(
        UCI_HAR_URL,
        headers={"User-Agent": "GuardianAI-ML-Pipeline/1.0"},
    )
    with urllib.request.urlopen(req, timeout=120) as resp, outer_zip.open("wb") as out:
        out.write(resp.read())

    digest = sha256_file(outer_zip)
    print(f"Downloaded {outer_zip.stat().st_size} bytes (sha256={digest})")

    with zipfile.ZipFile(outer_zip, "r") as zf:
        zf.extractall(base_dir)

    inner_zip = base_dir / "UCI HAR Dataset.zip"
    if inner_zip.exists():
        with zipfile.ZipFile(inner_zip, "r") as izf:
            izf.extractall(base_dir)

    if not (dataset_root / "train" / "X_train.txt").exists():
        raise RuntimeError(f"Expected UCI HAR files not found under {dataset_root}")

    manifest = {
        "source_url": UCI_HAR_URL,
        "archive_sha256": digest,
        "archive_bytes": outer_zip.stat().st_size,
        "extracted_path": str(dataset_root),
    }
    (base_dir / "download_manifest.json").write_text(json.dumps(manifest, indent=2))
    print(f"Verified extraction at {dataset_root}")
    return dataset_root


if __name__ == "__main__":
    download_uci_har()
