# 06 – Persistenz und Integrationen

Ziel: pruefen, dass Zustand kontrolliert gespeichert, wiederhergestellt und ueber Integrationsgrenzen transportiert wird, ohne konkurrierende Wahrheiten oder stille Datenmigrationen zu erzeugen.

# Persistenz

## PERSIST-001 – Workspace Session
Status: `NOT_TESTED` · P0 · T2

1. mehrere Panels oeffnen, verschieben, minimieren.
2. App beenden und neu starten.

Erwartet: Session wird ohne Crash und ohne unerklaerliche Panelduplikate wiederhergestellt.

## PERSIST-002 – Block View
Status: `NOT_TESTED` · P1 · T2

1. Blockpositionen, Zoom und Auswahl setzen.
2. speichern/neustarten.

Erwartet: View-State darf wiederhergestellt werden; Workflow-Semantik bleibt kanonisch und davon getrennt.

## PERSIST-003 – Flow View
Status: `NOT_TESTED` · P1 · T2

1. Nodepositionen, Zoom/Fokus/Collapse-Zustaende setzen.
2. speichern/neustarten.

Erwartet: Flow-View rekonstruiert Darstellung ohne eigene Graph-Wahrheit.

## PERSIST-004 – RailTrace letzte Position
Status: `NOT_TESTED` · P1 · T2

1. Session laden und Seek/Step-Position setzen.
2. speichern/neustarten.

Erwartet: letzte relevante Rail-Position wird plausibel wiederhergestellt und verweist weiterhin auf gueltige Daten.

## PERSIST-005 – Recording Session
Status: `NOT_TESTED` · P0 · T2

1. Recording erzeugen und speichern.
2. App neu starten.
3. Session erneut laden.

Erwartet: Event-Reihenfolge, Timestamps/relative Zeit und Referenzen bleiben konsistent.

## PERSIST-006 – Ressourcen Save/Reload
Status: `NOT_TESTED` · P1 · T2

1. aktuell unterstuetzte Marker/Screenshot/Template-Ressource speichern.
2. App neu starten.
3. Resource erneut laden/projizieren.

Erwartet: stabile Resource-Identitaet; kein stilles Ersetzen durch UI-State.

## PERSIST-007 – Alte/inkompatible Daten
Status: `NOT_TESTED` · P0 · T2

1. falls Testfixture vorhanden: alte oder unvollstaendige gespeicherte Daten laden.

Erwartet: kontrolliertes Fallback/Migration/Diagnose; kein Crash und keine lautlose falsche Interpretation.

# Drag/Drop als Integrationspfad

## INT-004 – TextEditor Drop Effect
Status: `NOT_TESTED` · P1 · T2

1. eine aktuell als Drag-Payload unterstuetzte Ressource/Repraesentation in den TextEditor ziehen.

Erwartet: Gesture wird in einen definierten semantischen Drop-Effekt uebersetzt; kein direkter unvalidierter Domain-Mutationspfad.

## INT-005 – Editor Representation Drop
Status: `NOT_TESTED` · P1 · T2

1. eine unterstuetzte Command-/Resource-Repraesentation in einen Editor ziehen.

Erwartet: resultierende Darstellung entspricht dem Payload; Transport und Effect bleiben getrennte Konzepte.

# VT2VT

## INT-001 – VT2VT Loopback
Status: `NOT_TESTED` · P1 · T2

1. Loopback-/lokalen Testpfad starten, soweit UI vorhanden.
2. bekannte Nachricht senden.

Erwartet: Nachricht kommt exakt einmal logisch an; keine Endlosschleife/duplizierte Wiederverarbeitung.

## INT-002 – VT2VT Codec Roundtrip sichtbar
Status: `NOT_TESTED` · P1 · T2

1. unterstuetzten Runtime-/Trace-Payload senden und empfangen.

Erwartet: IDs, Typ und Payload-Semantik bleiben ueber Encode/Decode erhalten.

## INT-003 – VT2VT LAN Observer
Status: `NOT_TESTED` · P2 · T3

1. zwei Geraete verbinden, sobald aktueller LAN-Transport testbar ist.
2. Observer-Modus verwenden.

Erwartet: Observer kann zugelassene Daten sehen, aber keine nicht autorisierte kanonische Mutation ausloesen.

# Provider/Plugin-Grenzen

## INT-006 – Provider-/Plugin-Gate
Status: `NOT_TESTED` · P0 · T2

1. Capability anfordern, deren Provider fehlt/deaktiviert ist.
2. denselben Capability-Pfad mit vorhandenem Provider testen, falls moeglich.

Erwartet: Resolver/Gate meldet Verfuegbarkeit explizit; UI erfindet keinen Erfolg. Provider-spezifische Daten werden in VTS-Contracts normalisiert.

## Spaetere Integrationen

Tasker, Termux, Shizuku, scrcpy, CustomChromeTab, OCR/OCV/YOLO und weitere Provider sind laut Roadmap Adapter-/Plugin-Kandidaten bzw. noch nicht vollstaendig releasefest. Sobald ein konkreter Contract implementiert ist, bekommt jeder Provider mindestens diese gemeinsame Testgruppe:

```text
INSTALL/AVAILABILITY
CAPABILITY DISCOVERY
VALID INPUT
INVALID INPUT
SUCCESS OUTCOME
ERROR OUTCOME
TIMEOUT/CANCEL
PROVENANCE
SAVE/RESTORE falls relevant
NO DIRECT CANONICAL MUTATION
```
