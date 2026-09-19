#!/usr/bin/env python3
"""Exportiert YOLO26n als Android-LiteRT-Modell (w8a32, GPU-fähig).

Benötigt Python 3.10–3.12 auf Linux x86_64 oder macOS:

    uv venv --python 3.12 .venv
    uv pip install --python .venv --index-url https://download.pytorch.org/whl/cpu torch torchvision
    uv pip install --python .venv "ultralytics-opencv-headless[export-litert]>=8.4.83"
    .venv/bin/python scripts/export_android.py
"""

from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "models"
MODEL_ID = "yolo26n"
QUANTIZE = "w8a32"
IMGSZ = 640


def main() -> None:
    from ultralytics import YOLO

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    weights = OUT_DIR / f"{MODEL_ID}.pt"
    model = YOLO(str(weights) if weights.exists() else f"{MODEL_ID}.pt")
    exported = model.export(
        format="litert",
        quantize=QUANTIZE,
        nms=False,
        end2end=False,
        imgsz=IMGSZ,
        batch=1,
    )
    print(f"Exportiert: {exported}")
    print("Android-Asset: ins Verzeichnis android/vision-yolo/src/main/assets/ kopieren.")


if __name__ == "__main__":
    main()
