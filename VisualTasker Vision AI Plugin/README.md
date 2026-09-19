# YOLO26n für Android

## Lizenz

Dieses Provider-Projekt wird wegen der enthaltenen Ultralytics-YOLO-Modelle
unter AGPL-3.0 geführt. Der Apache-2.0-lizenzierte WSS-Host bindet nur
`vision-contracts` ein; Modelle und Inferenzruntime bleiben in diesem separat
installierbaren Provider. Eine Enterprise-Lizenz kann diese Grenze für eine
anders lizenzierte Distribution ersetzen.

YOLO26n ist als offizielles LiteRT-Modell `w8a32` vorbereitet: dynamisches INT8 (INT8-Gewichte, FP32-Aktivierungen), Eingabe **NCHW** `[1, 3, 640, 640]`, Ausgabe `[1, 84, 8400]` ohne eingebautes NMS. Dieses Format kompiliert auf dem LiteRT-GPU-Delegate und braucht keine Kalibrierdaten.

## Inhalt

| Pfad | Zweck |
| --- | --- |
| `android/vision-yolo/src/main/assets/yolo26n_w8a32.tflite` | Android-Modell (Detect, COCO, 640 px) |
| `android/vision-yolo/src/main/assets/coco_labels.txt` | 80 COCO-Klassen |
| `android/` | Android-Studio-App mit Live-Kamera |
| `scripts/export_android.py` | Eigenen Export (Custom-Weights) |

## App bauen

1. Android Studio öffnen und den Ordner `android/` importieren.
2. Gerät oder Emulator mit **API 26+** wählen (arm64 bevorzugt; GPU nur auf echtem Gerät sinnvoll).
3. App starten und Kamerazugriff erlauben.

Debug-APK nach erfolgreichem Build:

`android/app/build/outputs/apk/debug/app-debug.apk`

Die App bezieht `yolo26n_w8a32.tflite` aus dem Modul `vision-yolo`, versucht zuerst **GPU**, fällt sonst auf **CPU/XNNPACK** zurück und zeichnet die Boxen über die Kamera.

Abhängigkeit: `com.google.ai.edge.litert:litert:2.1.5`.

## In eigenes Projekt einbinden

```kotlin
implementation("com.google.ai.edge.litert:litert:2.1.5")
```

Modell nach `android/vision-yolo/src/main/assets/` legen. Wichtige Punkte:

- Eingabe ist **NCHW** (planar RGB), Werte in `[0, 1]`.
- Tensor-Namen: Eingabe `args_0`, Ausgabe `output_0`.
- Letterbox auf 640×640 (Padding 114), danach Host-NMS (`conf=0.25`, `IoU=0.45`).
- FP16 nicht extra exportieren: der GPU-Delegate rechnet FP32-Graphen intern in FP16.

## Eigenes Modell exportieren

Nur auf Linux x86_64 oder macOS, Python 3.10–3.12, `ultralytics>=8.4.83`:

```bash
uv venv --python 3.12 .venv
uv pip install --python .venv --index-url https://download.pytorch.org/whl/cpu torch torchvision
uv pip install --python .venv "ultralytics-opencv-headless[export-litert]>=8.4.83"
.venv/bin/python scripts/export_android.py
```

Oder direkt:

```python
from ultralytics import YOLO
YOLO("yolo26n.pt").export(format="litert", quantize="w8a32", imgsz=640, nms=False, end2end=False)
```

Nicht `format="tflite"` mit `half=True` verwenden: dieser Pfad ist veraltet und scheitert oft am GPU-Delegate (`GATHER_ND`, INT64).

## Referenzwerte (offizielle w8a32-Assets)

Auf aktuellen Flaggschiffen liegt Detect-YOLO26n bei etwa **12–16 ms GPU** bzw. **35–50 ms CPU** inklusive Vor-/Nachverarbeitung, abhängig vom SoC.
