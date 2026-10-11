# M7 Capture Provider Matrix

Stand: 2026-10-11

| Provider / Consumer | Status | Display | Cancellation / Timeout | Frame-Sharing | Grenze |
| --- | --- | --- | --- | --- | --- |
| Android Accessibility Screenshot | IMPLEMENTED | explizite Display-ID, Mismatch fail-closed | Request-Cancellation, Coroutine-Cancellation, Timeout | ja | API 30+, verbundener Service |
| MediaProjection | NOT_REGISTERED | - | - | - | keine Session und keine Hintergrund-Permission-Anfrage |
| Recorder | IMPLEMENTED | persistierte Display-/Crop-/Insets-Provenienz | Capture-Fehler bleibt Recorder-Diagnose | konsumiert Lease, persistiert Asset | A11y und Frame zeitlich assoziiert, nicht atomar |
| Workspace/Runtime Screenshot | IMPLEMENTED | aktueller expliziter DisplayContext | typisiertes Capture-Resultat | Process-Service | Dateiexport bleibt Consumer-Aufgabe |
| Floating Overlay Screenshot | IMPLEMENTED | Overlay-DisplayContext | typisiertes Capture-Resultat | Process-Service | Overlay-Hide/Restore bleibt Host-Aufgabe |
| Scene Inspector | IMPLEMENTED | liest persistierte Display-ID | nicht anwendbar | referenziert Frame | keine eigene Capture-Engine |
| OCR / ML Kit | GAP | Vertrag vorhanden | Vertrag vorhanden | Frame-ID/Lease vorgesehen | kein produktiver ML-Kit-Provider |
| OpenCV | GAP | Vertrag vorhanden | Vertrag vorhanden | Frame-ID/Lease vorgesehen | Canvas-Vergleich ist kein registrierter Live-Provider |
| YOLO | OUT_OF_SCOPE | Vision-Plugin separat | - | spaeter | keine M7-0-Neuentwicklung |

Providerwahl verwendet `ProviderSelectionPolicy`, `ProviderAvailability` und `ProviderId` aus
M5-8. Bildprozessoren erhalten einen Frame oder eine Frame-Referenz; sie duerfen keinen zweiten
Screenshot ausloesen, wenn derselbe Frame noch gueltig und geleast ist.
