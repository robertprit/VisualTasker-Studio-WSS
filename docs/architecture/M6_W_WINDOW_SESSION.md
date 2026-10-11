# M6-W Unified WindowSession And Floating Workspace Architecture

Stand: 2026-10-11

## Status

M6-W fuehrt die vorhandenen Compose-Panels und nativen Android-Overlays unter einem gemeinsamen
Darstellungs- und Lifecycle-Vertrag zusammen. Die bestehende Panel-Engine, ihre Material-3-
Darstellung, Drag/Resize/Snap-Logik und `WorkspaceSessionStore` bleiben produktiv. Der neue Vertrag
ersetzt keinen Renderer und besitzt weder `WorkflowGraph` noch `ExecutionSession`.

Architektur:

`WindowSessionRegistry -> WindowSession -> WindowHost -> WindowSurface -> Panel`

Produktive Hosts:

- `StudioWindowHost`: Adapter fuer die vorhandenen Compose-Panels in `WorkspaceScreen`.
- `OverlayWindowHost`: Adapter fuer die vorhandenen `WindowManager`-Views in
  `StudioOverlayService`.
- `WindowHostType.DISPLAY`: reservierter Typ fuer einen spaeteren, separat freizugebenden Host.
  M6-W erzeugt keinen `VirtualDisplay`.

## Ownership Und Identitaet

- `WindowSessionId`, Panel-ID, Host-ID, Surface-ID und Display-ID sind getrennte Typen bzw.
  getrennte Felder.
- Neue Session-IDs werden unabhaengig von der Panel-ID erzeugt und persistent gespeichert.
- Die Registry besitzt den neutralen Sessionzustand. Android-`View`, Compose-State,
  `WindowManager.LayoutParams` und andere Window-Handles bleiben beim jeweiligen Host.
- `WorkflowGraph` bleibt kanonische semantische Wahrheit. Fensteroperationen erzeugen keine
  WorkspaceOperation und keine Workflow-Revision.
- `ExecutionSessionManager` und `RuntimeProcessHost` bleiben alleinige Runtime-Owner. Fenster duerfen
  deren Zustand beobachten, aber starten oder besitzen keinen zweiten Runner.

## Registry-Vertrag

`WindowSessionRegistry` bietet deterministische Operationen fuer Create, Attach, Detach, Show,
Hide, Move, Resize, BringToFront, Dock, Undock, Close, Focus und InteractionMode.

Identische Wiederholungen sind No-ops. Konflikte wie doppeltes Attach an einen anderen Host,
Host-Typ-Mismatch, fehlendes Display, ungueltige Bounds oder ein abgelehnter Host-Callback liefern
eine typisierte `WindowDiagnostic`. Ein Reattach mit derselben logischen Host-ID ersetzt den
Hostadapter, nicht die Session. Ein spaetes Dispose des alten Activity-/Service-Adapters kann den
neuen Adapter nicht aus der Registry entfernen.

## Bestehende Engine Bleibt Produktiv

### Studio

`WorkspaceScreen` projiziert jeden vorhandenen `PanelState` als lokale Window-Pixel in eine
`WindowSession`. Position, Groesse, Z-Order, Sichtbarkeit und Fokus kommen weiterhin aus dem
bestehenden Panelcontroller.
`DarkPanel` meldet zusaetzlich Drag-/Resize-Phasen, damit die Registry den fluechtigen
`WindowInteractionMode` kennt. Rails, Bottom Sheets, Toolbars und panelinterner Zustand werden nicht
dupliziert.

`WorkspaceSessionStore` bleibt fuer den vollstaendigen Compose-Panelzustand zustaendig. Der neue
Sessionstore ergaenzt nur hostuebergreifende, typisierte Window-Metadaten. Beim Studio-Reconcile ist
der bestehende `PanelState` die produktive Geometriequelle.

### Overlay

`StudioOverlayService` behaelt seine View-Erzeugung, Drag-/Resize-Handler und den bisherigen
`OverlayStateStore` als Legacy-/Migrationsfallback. Nach erfolgreichem `WindowManager.addView`
meldet der Service Attach und Geometrie an `OverlayWindowHost`. Vor dem Attach werden
`Settings.canDrawOverlays`, gespeicherte Display-ID und `WindowManager`-Fehler geprueft.

Bei Serviceende werden Views kontrolliert entfernt und Sessions detacht. Die gewuenschte
Sichtbarkeit bleibt bei einem Service-Neustart erhalten; explizites Schliessen setzt sie auf
`HIDDEN`. Fullscreen-LiveMarker sind ebenfalls registriert. Transientes Ausblenden waehrend einer
Screenshotaufnahme aendert nicht die persistente Sichtbarkeit.

## Display Und Koordinaten

`DisplayContext` enthaelt stabile Display-ID, physische Breite und Hoehe in Pixeln, Density,
Rotation, Insets und daraus abgeleitete nutzbare Display-Bounds.

Jeder Punkt und jede Bounds tragen einen expliziten `WindowCoordinateSpace`:

- `COMPOSE_DP`
- `LOCAL_WINDOW_PX`
- `DISPLAY_PX`
- `SCREENSHOT_PX`

Konvertierungen zwischen dp und lokalen Pixeln, lokalen und Display-Pixeln sowie Display- und
Screenshot-Pixeln sind explizite Funktionen. Display-Bounds werden gegen Insets geklemmt. Eine
persistierte Session eines nicht vorhandenen Displays wird diagnostiziert und nicht still auf das
Default-Display verschoben.

M6-W inventarisiert den aktuellen physischen Displaykontext. DisplayManager-Generationen,
Cutout-Geometrie, VirtualDisplay-Erzeugung und Nicht-Default-Display-Gesten bleiben M7-V bzw.
spaeteren Display-Slices vorbehalten.

## Fokus Und Touch

- Pro Registry ist hoechstens eine Session aktiv auf `ACTIVE`, `KEYBOARD` oder `TOUCH` fokussiert.
- Hide setzt Fokus auf `INACTIVE`.
- Drag und Resize sind fluechtige InteractionModes und werden nie als laufende Geste restauriert.
- `PASS_THROUGH`, `INTERACTIVE` und `MODAL` sind explizite Vertragswerte.
- Native Overlays bleiben mit den vorhandenen Android-Flags nicht keyboard-fokussierbar und
  blockieren dadurch keine Systemnavigation. Touch-interaktive Marker bleiben explizit interaktiv.
- Die eigene IME wurde nicht veraendert.

## Persistenz Und Recovery

`AndroidWindowSessionStore` persistiert Schema 1 mit WindowSession-ID, Panel-ID, Host-Typ,
Displaykontext, Position, Groesse, Docking, Z-Order, Sichtbarkeit und dauerhaftem InteractionMode.

Host-Handles, Fokus und laufende Drag-/Resize-Zustaende werden nicht persistiert. Beim Laden sind
Sessions detacht und inaktiv. Studio-Activity-Recreation und Overlay-Service-Neustart reattachen an
dieselbe logische Session. Alte Overlay-Platzierungen werden nur verwendet, wenn noch keine
WindowSession existiert.

## Tests

Pure JVM-Tests pruefen mehr als die geforderten 24 Zustaende, darunter alle Registry-Operationen,
Idempotenz, Diagnostics, Fokus, Pass-through, Z-Order, Docking, Activity-Recreation,
Overlay-Reattach, Persistenz, Display-Erhalt, Rotation, Insets und Koordinatenkonvertierung.

Android-Instrumentation prueft physische Displaydaten, prozessweite Registry-Eindeutigkeit und die
Manifest-Deklaration der Overlay-Berechtigung ohne den Berechtigungszustand zu veraendern.
Die Debug-App wurde auf dem angeschlossenen SM-S918B installiert und erfolgreich gestartet. Alle
drei WindowSession-Instrumentationstests liefen dort erfolgreich durch.

## Abnahme

| AC | Status | Nachweis / Grenze |
| --- | --- | --- |
| AC-01 Bestehende Panel-Infrastruktur | PASS | `PanelState`, `DarkPanel`, `WorkspaceSessionStore` bleiben produktiv. |
| AC-02 Gemeinsamer WindowSession-Vertrag | PASS | `WindowSessionContracts.kt`. |
| AC-03 Zentrale Registry | PASS | `WindowSessionRegistry.kt` und Pure-State-Tests. |
| AC-04 StudioWindowHost | PASS | Adapter und Verdrahtung in `WorkspaceScreen`. |
| AC-05 OverlayWindowHost | PASS | Adapter und Verdrahtung in `StudioOverlayService`. |
| AC-06 DisplayContext | PASS | Typisierte Display-ID, Metriken, Rotation und Insets. |
| AC-07 Koordinatenkonvertierung | PASS | dp/local/display/screenshot Tests. |
| AC-08 Fokus und Touch-Routing | PASS | Exklusiver Fokus, InteractionModes und bestehende Android-Flags. |
| AC-09 Persistenz/Recovery | PASS | Schema-1-Codec, Activity-/Service-Reattach Tests. |
| AC-10 Keine Leaks/Duplikate | PASS | Hostersetzung, idempotentes Attach und kontrolliertes Remove. |
| AC-11 Runtime unabhaengig | PASS | Keine Runtime-/Workflow-Abhaengigkeit im Window-Paket; Regressionstest. |
| AC-12 Tests und Geraetetest | PARTIAL | Build, Installation, Start und drei Instrumentationstests auf SM-S918B bestanden; reale Gesten und Prozess-Kill benoetigen separaten freigegebenen Ablauf. |

## Offene Grenzen

1. Reale Drag-/Resize-/Dock-/Rail-/Overlay-Abnahme auf dem Zielgeraet ist noch auszufuehren.
2. Der visuelle M5-H-RailTrace-Provenienznachweis und kontrollierte Prozess-Kill bleiben offen.
3. Exakte Cutout-/WindowInsets-Ermittlung pro Surface kann spaeter mit dem jeweiligen Activity-
   Window erweitert werden; M6-W verwendet physische Metriken plus Systembar-Insetwerte.
4. `DISPLAY` ist nur ein Hosttyp. VirtualDisplay, MediaProjection und Remote-Sessions sind nicht
   implementiert.
