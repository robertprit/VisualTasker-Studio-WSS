# Vision / Perception Pipeline — WSS Reference

> **Status:** Adapted Reference
>
> **Origin:** `visualtasker-studio/docs/architecture/migration/VISION_PIPELINE_BOUNDARY.md`

## Grundidee

Das alte Studio hatte Wahrnehmung, Canvas, Dataset und UI teilweise vermischt. Der alte Boundary-Entwurf trennte deshalb bereits Provider, Processor, Artifact, Observation/Detection, Canvas, Overlay, Tool/Perspective und Persistence.

Diese Trennung passt direkt zur heutigen WSS-/Worldview-Architektur und wird hier weitergeführt.

## Zielmodell

```text
Provider
  -> Raw Input / Capture
  -> Processor
  -> Observation / Detection
  -> Evidence Fusion / Entity Resolution
  -> WorldEntity / Scene / Worldview

Artifacts / Resources laufen parallel über Persistence.
Canvas und Overlays projizieren diese Daten, besitzen sie aber nicht.
```

## Provider

Provider beschaffen Rohinput oder externe Beobachtungen, z. B.:

- Screenshot Capture
- Accessibility Scan
- zukünftige Detector- oder Remote-Provider

Provider sollen keine Panel-State-, Rendering- oder Dataset-Authority besitzen.

## Processor

Processor transformieren Input in normalisierte Ergebnisse, z. B.:

- OCR
- OpenCV Template Matching
- Region OCR
- YOLO/Object Detection
- Color Sampling

Processor geben strukturierte Ergebnisse zurück und besitzen weder Canvas noch UI-Lifecycle.

## Artifact / Resource

Persistente oder referenzierbare Objekte, z. B.:

- Screenshot
- Template
- Marker
- Dataset Asset

Sie sind nicht dasselbe wie eine Observation.

## Observation / Detection

Eine Observation ist ein provider- oder processor-erzeugtes Wahrnehmungsergebnis. Sie enthält idealerweise stabile Source-/Provider-Metadaten und Provenance.

Eine Detection ist eine spezialisierte Observation. Sie ist nicht automatisch ein `WorldEntity`.

## Evidence Fusion / Entity Resolution

Neue WSS-Regel gegenüber dem alten Dokument:

`Observation -> Resolution / Fusion -> WorldEntity`

Mehrere Observations können gemeinsam ein Entity stützen. Konflikte oder mehrere plausible Kandidaten bleiben als `Ambiguity` sichtbar.

## Canvas

Der gemeinsame Screenshot-/Frame-Canvas darf besitzen:

- Zoom
- Pan
- Auswahl
- sichtbare Layer
- Koordinatentransformation
- Hit Testing gegen normalisierte Layerdaten

Er darf nicht besitzen:

- OCR- oder Detector-Logik
- Screenshot Capture
- Dataset-Persistence
- WorldEntity-Authority

## Overlay

Overlays projizieren Observations, Entities, Marker, Regions und weitere Layer. Sie dürfen keine versteckte Detection- oder Resolve-Logik enthalten.

## Tool / Perspective

Inspector, Template Editor, Marker, Recorder, Dataset View und ähnliche Tools arbeiten über gemeinsame Contracts. Tool-State bleibt lokal/transient.

## Hauptrisiken aus dem alten Studio

Die alte Dokumentation identifizierte bereits:

- Screenshot Ownership war über mehrere Module verteilt;
- Canvas erledigte zu viel;
- OCR war nicht normalisiert;
- Accessibility `ElementNode` wurde teilweise als Container für andere Detection-Typen missbraucht;
- Dataset-Pfade konnten zwischen Capture, Anzeige und Persistence driften;
- Marker, Bounds und Regions hatten mehrere konkurrierende Formen.

Diese Risiken sollen im WSS nicht erneut als Panel-spezifische Modelle entstehen.
