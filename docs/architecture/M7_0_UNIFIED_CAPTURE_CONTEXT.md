# M7-0 Unified Capture Context

Stand: 2026-10-11

## Ziel Und Ergebnis

M7-0 fuehrt einen gemeinsamen, displaybewussten Capture-Vertrag ein. Es entsteht keine zweite
Screenshot-Engine: Der produktive Provider adaptiert
`VisualTaskerAccessibilityService.takeScreenshot`. Recorder, Workspace-Screenshot, Runtime-
Screenshot und Floating Overlay verwenden diesen Adapter ueber denselben `CaptureFrameService`.

Kette:

`DisplayContext -> CaptureRequest -> CaptureFrameProvider -> CaptureFrame -> Observation -> Consumer`

`WorkflowGraph` bleibt davon unabhaengig. Ein Frame oder eine Observation mutiert weder den
Workspace noch erzeugt sie eine Workflow-Entity.

## Bestandsaufnahme

| Alter Weg | Vor M7-0 | M7-0 |
| --- | --- | --- |
| Accessibility | `takeScreenshotTo` schrieb Default-Display direkt in eine Datei | bestehende API nimmt explizite Display-ID an; Adapter liefert typisiertes Resultat |
| Recorder | eigener temporaerer Screenshot-/Bitmap-Bounds-Pfad | liest denselben `CaptureFrame` und persistiert additive Provenienz |
| Workspace/Runtime | direkter Service-Aufruf | gemeinsamer Process-Service, anschliessend expliziter Export |
| Floating Overlay | direkter Service-Aufruf nach Overlay-Hide | gemeinsamer Process-Service; Hide/Restore bleibt beim Overlay |
| Canvas/Inspector | gespeicherte Dateien und Recorder-Frames | Recorder-Frame zeigt Capture-ID, Display, Quelle und Koordinatenraum |
| OCR | Katalog/Observation-Daten, kein Live-ML-Kit-Provider | Consumer-Vertrag kann gemeinsamen Frame referenzieren; Live-Provider bleibt M7-3 |
| OpenCV | lokaler Canvas-Vergleich, kein registrierter Provider | Consumer-Vertrag kann gemeinsamen Frame referenzieren; Live-Provider bleibt M7-4 |
| MediaProjection | nicht vorhanden | weiterhin nicht vorhanden; keine implizite Permission-Anfrage |

## Vertraege

`CaptureContracts.kt` definiert `CaptureRequest`, `CaptureFrame`, `CaptureFrameId`,
`CaptureFrameProvider`, `CaptureCapability`, `CaptureResult`, `CaptureFailure`, `CaptureMetadata`
und eine bilddatenfreie `CaptureDataReference`.

Ein `CaptureFrame` enthaelt nur Identitaet, Metadaten und Datenreferenz. Die PNG-Bytes liegen im
begrenzten `CaptureFrameStore` und sind nur ueber eine `CaptureFrameLease` lesbar. Damit gelangen
weder `Bitmap` noch Bildbytes in `WorkflowGraph`, Observation oder ExecutionTrace.

`ObservationContracts.kt` trennt:

- Frame: konkrete Aufnahme.
- AccessibilitySnapshot: zeitlich zugeordnete UI-Struktur, ausdruecklich nicht atomar.
- Observation: abgeleitetes Ergebnis mit Producer, Status, Zeit, Display und Koordinatenraum.
- ElementMatch: erkanntes Ziel.
- WorkflowEntity: bleibt ausserhalb dieses Pakets kanonische Workflow-Semantik.

## Provider Resolution

`CaptureProviderRegistry` verwendet die M5-8-Typen `ProviderSelectionPolicy`, `ProviderId` und
`ProviderAvailability`. Explizite Auswahl, Ausschluss, Praeferenz, Prioritaet und stabile ID-
Sortierung sind deterministisch. Nicht verfuegbar, Permission fehlt, Display nicht unterstuetzt,
Mismatch, Cancellation, Timeout, Providerverlust, ungueltiger Frame und Speicherlimit sind
unterschiedliche `CaptureFailureKind`-Werte.

Der Accessibility-Provider prueft API-Level, verbundenen Service und angefordertes Display. Der
tatsaechlich verwendete `DisplayContext` kommt in das Resultat. Ein Mismatch wird verworfen, nicht
auf Display 0 umgeleitet.

## Frame Lifecycle

- Der Service publiziert genau einen Frame in einen begrenzten In-Memory-Store.
- Mehrere Consumer verwenden `retain()` und lesen dieselbe Frame-ID.
- Jede Lease wird explizit geschlossen.
- Freigegebene Frames koennen entfernt oder bei Speicherdruck evicted werden.
- Bytes werden defensiv kopiert; Android-HardwareBuffer und temporaere Datei werden im Provider
  immer geschlossen beziehungsweise geloescht.
- Coroutine-Cancellation wird nicht verschluckt. Explizite Request-Cancellation und Timeout werden
  typisiert diagnostiziert.
- Der Recorder persistiert weiter content-addressed PNG-Assets; der In-Memory-Store ersetzt keine
  Langzeitpersistenz.

## Display Und Koordinaten

`CaptureCoordinateMapper` bildet explizit ab:

`WindowLocal <-> DisplayPx <-> ScreenshotPx <-> RegionLocal`

Der Mapper beruecksichtigt Capture-Crop, Rotation und abweichende Frame-Aufloesung. Density und
Insets bleiben im `DisplayContext`; dp und px werden nicht implizit vermischt. Recorder-Schema 5
speichert nullable Display-, Insets-, Crop- und Capture-Source-Metadaten. Alte Schema-4-Frames
werden durch Migration 4->5 mit `null`-Provenienz erhalten und bleiben lesbar.

Der Accessibility-Provider erzeugt ausschliesslich Vollbild-Frames. Eine direkte Crop-Anfrage wird
mit `CAPABILITY_UNSUPPORTED` abgelehnt, statt Vollbilddaten mit falschen Crop-Metadaten zu
veroeffentlichen. Regionale Ansichten werden aus einem geteilten Vollbild-Frame und dem
`CaptureCoordinateMapper` abgeleitet.

## Consumer

- Recorder: produktiv auf `CaptureFrameService` migriert; A11y-Snapshot referenziert den
  Recorder-Frame und dessen Capture-Provenienz.
- Scene Inspector: zeigt Capture-ID, Display, Quelle, Koordinatenraum, Bounds und getrennte
  Frame-/Snapshot-Zeitpunkte.
- Canvas-/Vision-Observation: kann Frame-ID, Display und Koordinatenraum tragen.
- Runtime: `CaptureTraceInfo` transportiert nur Provenienz und Abmessungen, keine Bilddaten.
- OCR/OpenCV: gemeinsamer Observation-/Frame-Vertrag vorhanden; produktive Engines fehlen im
  aktuellen Repository und bleiben eigene M7-3-/M7-4-Slices.

## Abnahme

| AC | Status | Nachweis / Grenze |
| --- | --- | --- |
| AC-01 Capture-Wege inventarisiert | PASS | Tabelle und Quellensuche; MediaProjection ist nicht vorhanden. |
| AC-02 Einheitlicher CaptureFrame-Vertrag | PASS | `capture/CaptureContracts.kt`. |
| AC-03 Expliziter Display-Kontext | PASS | Request, Metadata, Accessibility-Provider und Mismatch-Test. |
| AC-04 M5-8 Provider-Integration | PASS | vorhandene Policy-/Availability-/Provider-ID-Typen werden wiederverwendet. |
| AC-05 Frame-Lifecycle | PASS | Lease, Refcount, Speicherbudget, Cleanup-, Sharing- und Timeout-Tests. |
| AC-06 Observation Contract | PASS | getrennte Frame-, Snapshot-, Observation- und Match-Typen. |
| AC-07 Recorder integriert | PASS | `AndroidRecorderCaptureSource`, Room Schema 5 und Legacy-Migration. |
| AC-08 Inspector integriert | PASS | Recording-Scene-Tree zeigt gemeinsame Frame-Provenienz. |
| AC-09 OCR/OpenCV integriert | GAP | gemeinsame Input-/Observation-Referenz vorhanden; Live-Provider existieren noch nicht. |
| AC-10 Koordinatenabbildung | PASS | Rotation-, Crop-, Scaling-, Insets-, Density- und Region-Tests. |
| AC-11 Trace/Workspace-Integritaet | PASS | bilddatenfreier `CaptureTraceInfo`; keine Workspace-Abhaengigkeit im Capture-Kern. |
| AC-12 Regressionen/Geraet | PARTIAL | JVM-/Build-Gates und nicht-erfassende Instrumentation; echter Capture/OCR/OCV-Lauf benoetigt freigegebene Testoberflaeche. |

## Verifikation

- 24 getrennte M7-Pruefungen: 23 Capture-/Observation-Vertragstests plus Room-Migration 4->5.
- App: 855 JVM-Tests ohne Fehler; `assembleDebug` erfolgreich.
- BlockEditor und FlowEditor: jeweiliger vollstaendiger `test`-Task erfolgreich.
- Three-Editor-Conformance: drei erzwungene, cache-unabhaengige Laeufe erfolgreich.
- SM-S918B: sechs ausgewaehlte Room-/Window-Instrumentationstests erfolgreich.
- SM-S918B: Debug-APK installiert, `MainActivity` kalt gestartet, Prozess aktiv, keine Fatal Exception.
- Nicht ausgefuehrt: produktive Bildschirmaufnahme, Inspector-Navigation, OCR und OpenCV. Diese
  Schritte erfordern eine separat freigegebene Testoberflaeche und produktive M7-3-/M7-4-Provider.

## Offene Grenzen

1. ML Kit OCR und OpenCV sind keine produktiven Provider und werden hier nicht vorgetaeuscht.
2. MediaProjection und VirtualDisplay bleiben ausserhalb von M7-0.
3. Ein echter Screenshot auf dem Zielgeraet sowie visuelle Inspector-/Koordinatenpruefung benoetigen
   eine ausdruecklich freigegebene Testoberflaeche.
4. Gesture-Requests tragen noch nicht durchgaengig denselben Nicht-Default-Display-Vertrag; das ist
   kein Capture-Fallback und bleibt separat.
