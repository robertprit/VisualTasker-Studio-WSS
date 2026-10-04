# BlockEditor Drag / Render Performance Audit

Stand: 2026-10-04
Scope: P0 Diagnose, keine Produktionscode-Aenderung, kein M2-2

## 1. Executive Summary

Der kanonische `WorkspaceDocument` wird waehrend eines aktiven Drag nicht pro
Pointer-Move mutiert. Der Drag besitzt bereits getrennten transienten Zustand und
verschiebt die gezogenen Bloecke im Canvas per Draw-Translation.

Die Traegheit entsteht trotzdem, weil jeder Move einen neuen `DragRenderState`
publiziert. Dieser State wird in `BlockEditorHost` breit gelesen und invalidiert
den kompletten Editor-/Canvas-Pfad. Dabei werden insbesondere:

1. alle Workspace-Bloecke ohne Viewport-Culling erneut gezeichnet,
2. pro Block mehrfach globale Graphabfragen ausgefuehrt,
3. die vollstaendige EMScript-/IR-Projektion ueber `codePreview` erneut angefordert,
4. Snap-Kandidaten zwar raeumlich vorgefiltert, danach aber mit wiederholten
   globalen `WorkspaceGraph`-Traversierungen validiert,
5. beim Drop viele unabhaengige Placement-Animationen gestartet.

Auf dem SM-S918B mit dem geladenen LARGE-Workspace (181 Bloecke) betrug der
Median waehrend eines einfachen aktiven Drag 200 ms und bei einem komplexen
Container-Drag 250 ms. Alle gemessenen aktiven Drag-Frames waren janky. Der
P95 lag bei 300 beziehungsweise 350 ms. Das Problem ist damit nicht nur
subjektiv, sondern deutlich oberhalb der Budgets von 16,67 ms bei 60 Hz und
8,33 ms bei 120 Hz.

## 2. Testgeraet und Build

| Merkmal | Wert |
|---|---|
| Geraet | Samsung SM-S918B |
| Android | 16 / API 36 |
| App | `com.visualtasker.wss`, Version 1.0 (1) |
| Root-Revision | `3666887` |
| BlockEditor-Revision | `f051e77` |
| Flowchart-Revision | `008b136` |
| Display | 1080 x 2316, adaptiv 10-120 Hz |
| Beim Idle-Sample gemeldete Render-Rate | 24 Hz |
| Aktiver Workspace | Command-Catalog-Breadth, 181 Bloecke |

Werkzeuge:

- Android `dumpsys gfxinfo ... reset` und Frame-Statistik
- reproduzierbare `motionevent`-Sequenzen mit verifiziertem Pointer-Log
- temporaere, nach der Messung entfernte JVM-Probes fuer Fixture-Groesse,
  Layout und EMScript-Generator
- statischer Call-/State-/Draw-Audit

Die ADB-Move-Sequenzen liefern weniger und grober getaktete Pointer-Events als
ein Finger. Deshalb sind die gemessenen Framezeiten belastbar, nicht aber eine
Behauptung ueber die exakte Eventrate eines Menschen. Compose-Recomposition-,
Measure- und GC-Zaehler wurden nicht mit ausreichender Zuverlaessigkeit
erfasst und werden nicht geschaetzt.

## 3. Reale Drag-Pipeline

### Drag Start

```text
WorkspacePointerGestures.awaitEachGesture
  -> BlockEditorController.onLongPressDragStart
  -> blockTouchZoneAt
  -> beginBlockDrag
  -> DragOperations.beginDrag
  -> LayoutEngine.build(document)                  pre-lift
  -> DragLayoutPreview.layoutDocument/snapDocument
  -> LayoutEngine.build(layoutDoc)                 static, je nach PullMode
  -> LayoutEngine.build(layoutDoc)                 drag layout
  -> dragRender = DragRenderState(...)
```

Ein Single-Drag baut zwei Layouts, ein Stack-Drag bis zu drei. Das ist
Drag-Start-Kosten, nicht Move-Kosten.

### Drag Move

```text
Pointer MOVE
  -> BlockEditorController.onPointerMove
  -> TransientEditorState neu anlegen
  -> DragOperations.updateDrag
  -> SnapEngine.findSnapCandidate
       -> Anchor-SpatialIndex.queryPoint
       -> WorkspaceGraph-/Type-Kompatibilitaetspruefungen
  -> DragRuntimeState.update
  -> dragRender = render.copy(...)
  -> BlockEditorHost recomposes
       -> controller.codePreview
       -> EmscriptGenerator -> IrGenerator -> gesamtes Dokument
       -> BlockEditorScaffold
       -> EditorCanvasLayer
            -> Grid
            -> alle statischen Bloecke
            -> alle Inline-Reporter-Overlays
            -> gezogener Block/Subtree
            -> Snap-Visualisierung
```

Der kanonische Dokumentzustand und `layoutCache` bleiben dabei stabil. Die
gezogene Position wird als Draw-Offset angewendet. Trotzdem wird der gesamte
Canvas neu gezeichnet.

### Drag End / Settle

```text
Pointer UP
  -> BlockEditorController.onPointerUp
  -> WorkspaceGraph detach/parent/chain checks
  -> DragOperations.endDrag
  -> Root-Position sichern und clampen
  -> dragRender = null
  -> applyPersistentDocumentChange (wenn geaendert)
       -> History record
       -> document = newDocument
       -> LayoutEngine.build(newDocument)
       -> WorkspaceSerializer.serialize(newDocument)
       -> debounced Validation + EMScript generation
  -> Placement Animatable pro geaendertem Block
```

## 4. State Mutation Map

| State | Owner/Typ | Drag Start | Jeder Move | Drop | Ausgeloeste Arbeit |
|---|---|---:|---:|---:|---|
| `WorkspaceDocument` | Controller, Compose State | nein | nein | bei Aenderung | Layout, Serialisierung, Derived Outputs |
| `layoutCache` | Controller, Compose State | nein | nein | ja | Canvas/Editor-Recomposition |
| `dragRender` | Controller, Compose State | ja | **ja, `copy`** | null | Host + kompletter Canvas-Pfad |
| `DragRuntimeState` | Drag-Presentation | ja | **ja** | beendet | Offset/Snap-Praesentation |
| `selectedBlockIds` | Controller, Compose State | gegebenenfalls | nein | gegebenenfalls | Selection/Inspector |
| `snapCandidate` | Bestandteil `dragRender` | null | **ja** | verbraucht | Snap-Markierung und Drop-Semantik |
| `viewport` | Controller, Compose State | nein | nur Auto-Scroll/Pan | nein | Canvas-Transformation |
| Placement-`Animatable`s | Canvas, StateMap | nein | nein | **je Zielblock** | wiederholte Canvas-Invalidierung |

Antwort auf die kritische Frage: Der kanonische Workspace wird beim Move nicht
veraendert. Zu breit ist der transiente Presentation-State und dessen Leser.

## 5. Recomposition Findings

- `BlockEditorHost` liest `controller.dragRender` und `controller.codePreview`
  im selben Composable-Aufruf.
- `codePreview` ist kein gecachter Snapshot. Der Getter ruft
  `generateDraft(false)` auf; dieser projiziert das gesamte Dokument ueber den
  Workspace-Codegenerator.
- Auch `selectedBlockInfo()` und Palette-Ableitungen werden im breiten Host-Pfad
  erneut angefordert.
- `dragRender` enthaelt pro Move eine neue Objektidentitaet.
- Exakte Recomposition-Anzahl: **UNKNOWN**. Code und Device-Verhalten belegen
  die breite Invalidierungsgrenze, aber kein verlaesslicher Compose-Zaehler wurde
  erhoben.

Host-Probe fuer den vollstaendigen Generator:

| Fixture | Bloecke | Median | P95 |
|---|---:|---:|---:|
| SMALL | 15 | 0,333 ms | 0,540 ms |
| MEDIUM | 42 | 0,763 ms | 1,424 ms |
| LARGE | 181 | 3,126 ms | 5,925 ms |
| Complex Medium | 151 | 1,444 ms | 3,218 ms |
| Complex Large | 278 | 3,862 ms | 4,882 ms |

Diese Zeiten sind Host-JVM-Werte und keine Android-Framezeiten. Sie zeigen die
Skalierung und dass Codegenerierung in einem Presentation-only Move falsch
platziert ist.

## 6. Measure / Layout Findings

- Aktiver Drag verwendet Canvas-Translation. Es wird kein `LayoutEngine.build`
  pro Move aufgerufen.
- `LayoutEngine.build` laeuft zwei- bis dreimal beim Drag Start und einmal beim
  Drop beziehungsweise auch bei einem unveraenderten Drop.
- Der Layoutaufbau erzeugt Flat-Listen, Hit-Primitives, Anchor-Indizes,
  `MeasuredLayoutTree` und `PlacedLayoutTree` komplett neu.
- Mehrere interne `.find`- und `WorkspaceGraph`-Abfragen wachsen mit der
  Dokumentgroesse.
- Compose-Measure-/Layout-Passzahl waehrend Drag: **UNKNOWN**. Der fachliche
  LayoutEngine ist waehrend Move nicht aktiv; daher ist globaler semantischer
  Relayout kein Hauptverursacher aktiver Drag-Frames.

Lineare Layout-Probe:

| Bloecke | Median | P95 |
|---:|---:|---:|
| 10 | 0,620 ms | 1,127 ms |
| 50 | 1,209 ms | 2,734 ms |
| 100 | 2,071 ms | 3,645 ms |
| 200 | 6,821 ms | 9,428 ms |

## 7. Shape / Geometry Findings

- Legacy-Blockpfade sind in `BlockPathCache` nach Definition, Groesse und
  Branch-Dividern gecacht.
- Trotzdem erzeugt `resolveBlockVisualPath` fuer jeden gezeichneten Block einen
  `BlockVisualPathRequest`, kopiert `branchDividerYs` und erstellt eine tiefe
  `presentationSnapshot()`-Kopie der relevanten Definitionslisten.
- Der WSS-Provider fragt danach `VisualAssetPresentationRuntime.pathFor` ab.
- Auf dem Testgeraet existierte kein Visual-Asset-Katalog. Deshalb trat dort
  keine EMA-Datei-/Decode-/Morph-Kosten auf. Der Provider-Lookup und die
  Request-/Snapshot-Allokationen blieben bestehen.
- Bei gebundenen EMA-Assets wuerde aktuell pro Draw Datei lesen, dekodieren,
  morphen, fitten und einen neuen `Path` erzeugen. Das ist ein latentes, aber in
  dieser Messung nicht aktives Hochrisiko.
- Blockgroesse, Branch-Topologie und lokale Connector-Offsets bleiben beim Move
  fachlich unveraendert und sind cachebar.

## 8. HitTest Findings

- Pointer-Down nutzt vorhandene Hit-Primitives und einen `SpatialIndex`.
- Ein aktiver Move fuehrt nicht erneut den allgemeinen Block-Hit-Test aus.
- Der Index verwendet grobe 128-Pixel-Zellen und Bounds-Rejection.
- `SpatialIndex.query` allokiert pro Query dennoch ein neues `mutableSetOf`.

Bewertung: Allgemeines HitTesting ist fuer ACTIVE DRAG nicht signifikant.

## 9. Snap Findings

- Snap wird bei jedem Move berechnet.
- Anchor-Kandidaten werden zuerst lokal ueber `anchorIndex.queryPoint` gesucht;
  es gibt also keinen ungefilterten globalen Anchor-Scan.
- Fuer jeden lokalen Kandidaten folgen jedoch potentiell teure globale Checks:
  `findConnection`, `descendants`, `slotContaining`, `previousChain`, belegter
  Partner und Value-Type-Inferenz.
- `WorkspaceGraph.findConnection` scannt alle Bloecke und deren Connections.
- `isDescendantOf` baut den Descendant-Set rekursiv neu auf.
- Kandidatenzahl, Kompatibilitaetspruefungen und Geometriechecks pro Frame:
  **UNKNOWN**, da P0 keine permanente Instrumentation hinterlaesst.

Der raeumliche Index begrenzt die Geometriekandidaten gut; die semantische
Kandidatenvalidierung skaliert noch mit dem gesamten Graphen.

## 10. Connection Findings

- Bestehende Connection-Anchor-Geometrie kommt waehrend Drag aus dem stabilen
  `LayoutCache` und wird nicht komplett neu berechnet.
- Der Block-Canvas zeichnet keine globalen Kanten neu; er zeichnet Bloecke,
  Slots und den aktiven Snap-Indikator.
- Semantische Connection-Aufloesung findet bei Snap-Pruefung und Drop statt.
- Globale Connection-Suche ist damit ein Snap-/Graphproblem, nicht primaer ein
  Connector-Drawproblem.

## 11. Animation Findings

- `EditorCanvasLayer` haelt ein `Animatable<Offset>` pro sichtbarem Block.
- Bei einer neuen `documentVersion` wird eine Target-Map fuer alle sichtbaren
  Bloecke erstellt.
- Fuer jeden veraenderten Zielwert startet eine eigene 170-ms-Tween-Coroutine.
- Alle Animationen schreiben in State, den derselbe gemeinsame Canvas liest.
- Die Animation wird nicht pro Drag-Move neu gestartet, verstaerkt aber den
  Drop-/Reflow-Jank und kann viele komplette Canvas-Redraws erzeugen.

## 12. Allocation Findings

Signifikante kurzlebige Objekte im Move-/Draw-Pfad:

- `TransientEditorState`, `DragSession.copy`, `DragRenderState.copy`
- `BlockEditorValidationEvent` pro Move, obwohl der WSS-Host keinen eigenen
  Validation-Event-Consumer registriert
- bewegte Anchor-Liste und SnapCandidate
- `mutableSetOf` pro Spatial-Index-Query
- sortierte/gefilterte Blocklisten in mehreren Canvas-Paessen
- `BlockVisualPathRequest`, Definitions-Snapshots und Listen-Kopien pro Block
- Branch-/Inline-Reporter-Ableitungen und lineare `.find`-Suchen pro Block
- Textmessungsanfragen und temporare Draw-Objekte

Ein GC-Anteil an den langen Frames wurde nicht belastbar gemessen: **UNKNOWN**.
Die Allokationsdichte ist dennoch als mitwirkend einzustufen.

## 13. Draw Findings

- Jeder `dragRender`-Wechsel invalidiert den gemeinsamen Canvas.
- Der Dot-Grid-Hintergrund wird erneut gezeichnet.
- `visibleBlocks` wird vollstaendig sortiert und durchlaufen.
- Es gibt kein Viewport-Culling; offscreen Bloecke werden ebenfalls verarbeitet
  und gezeichnet.
- `movesWithDrag` ruft fuer viele Bloecke
  `WorkspaceGraph.isAttachedToDraggedAncestor` auf. Diese Funktion folgt
  Reporter-Eltern ueber wiederholte globale Connection-Suchen.
- Container-Visuals filtern globale Branch-/Slot-Listen pro Block.
- Inline-Reporter werden in einem zweiten globalen Pass verarbeitet.
- Gezogene Bloecke werden in einem dritten Filter-/Sortier-/Draw-Pass
  verarbeitet.
- Android meldete bei jedem gemessenen Drag-Frame langsame Draw Commands.

Das ist der staerkste belegte aktive Hotspot.

## 14. Small / Medium / Large Comparison

| Groesse | Fixture | Bloecke | Layout Median/P95 | Generator Median/P95 | Device Drag |
|---|---|---:|---:|---:|---|
| SMALL | Sample | 15 | 10er Probe: 0,620/1,127 ms | 0,333/0,540 ms | nicht separat gemessen |
| MEDIUM | Basic | 42 | 50er Probe: 1,209/2,734 ms | 0,763/1,424 ms | nicht separat gemessen |
| LARGE | Catalog Breadth | 181 | 200er Probe: 6,821/9,428 ms | 3,126/5,925 ms | gemessen |

Die JVM-Probes sind reproduzierbare Skalierungsindikatoren, keine
Android-Frame-Benchmarks. Fuer SMALL und MEDIUM wurden keine belastbaren
Device-Framewerte erzwungen, weil ein Fixture-Wechsel den aktuellen
Benutzer-Workspace veraendert haette und P0 keine neue Benchmark-Oberflaeche
einfuehren soll.

## 15. Simple / Complex Subtree Comparison auf LARGE

| Phase | Typ | Frames | Janky | Median | P95 |
|---|---|---:|---:|---:|---:|
| Idle, 5 s | keine Interaktion | 11 | 5 / 45,45 % | 34 ms | 57 ms |
| Active Drag | einfacher Statement-Block | 9 | 9 / 100 % | 200 ms | 300 ms |
| Drop/Settle | einfacher Statement-Block | 8 | 7 / 87,50 % | 150 ms | 250 ms |
| Active Drag | Repeat/komplexer Subtree | 7 | 7 / 100 % | 250 ms | 350 ms |
| Drop/Settle | Repeat/komplexer Subtree | 7 | 7 / 100 % | 129 ms | 300 ms |

Der komplexe Subtree erhoeht aktive Drag-Kosten, die Grundlast ist aber bereits
beim einfachen Block extrem. Die Kosten skalieren daher primaer mit
Workspace-Groesse und zusaetzlich mit Subtree-Komplexitaet.

Der Idle-Wert ist nur ein Kontrollsample: Eine wirklich ruhende Compose-UI
rendert kaum Frames. Die elf Frames zeigen verbleibende UI-Aktivitaet, sind aber
keine kontinuierliche Idle-FPS-Messung.

## 16. Hypothesis Matrix H1-H10

| Hypothese | Bewertung | Evidenz |
|---|---|---|
| H1 Drag veraendert zu viel zentralen State | **CONTRIBUTING** | Kanonisches Dokument bleibt stabil, aber `dragRender` ist breit gelesener Compose-State und wird pro Move ersetzt. |
| H2 Unnoetig breite Recomposition | **CONFIRMED** | Host liest Drag, CodePreview, Inspector-/Palette-Ableitungen gemeinsam; kompletter Canvas wird invalidiert. Exakte Count-Zahl UNKNOWN. |
| H3 Unnoetig Measure/Layout | **NOT SIGNIFICANT fuer ACTIVE DRAG**, **CONTRIBUTING bei Start/Settle** | Kein LayoutEngine-Build pro Move; zwei bis drei Builds bei Start, einer bei Drop. Compose-Passzahl UNKNOWN. |
| H4 Shape/Path zu haeufig | **CONTRIBUTING** | Legacy-Pfad gecacht, aber Request/Definition-Kopien pro Draw. EMA-I/O auf Testgeraet nicht aktiv; bei Bindings Hochrisiko. |
| H5 Globales HitTesting | **NOT SIGNIFICANT** | HitIndex nur bei Pointer-Start; kein allgemeiner Hit-Test pro Move. |
| H6 Snap Candidate Search | **CONTRIBUTING** | Lokaler Anchor-Index, danach wiederholte globale Graph-/Typpruefungen pro Kandidat und Move. |
| H7 Globale Connection-Aktualisierung | **NOT SIGNIFICANT fuer Draw** | Anchor-Geometrie stabil; keine globalen Kanten im Block-Canvas. Globale Suche wirkt ueber Snap mit. |
| H8 Animation verstaerkt Jank | **CONTRIBUTING** | Pro geaendertem Block eigene Animatable-Coroutine, alle invalidieren denselben Canvas beim Settle. |
| H9 Allocation/GC | **CONTRIBUTING, GC INCONCLUSIVE** | Zahlreiche Listen/Kopien/Events/Snap- und Shape-Objekte pro Move/Draw; kein belastbarer GC-Trace. |
| H10 Canvas/Draw ist Hauptproblem | **CONFIRMED** | Kein Culling, mehrere Vollpaesse, globale Graphabfragen pro Block; Slow Draw Commands auf allen Drag-Frames. |

## 17. Top Hotspots

### #1 Vollstaendiger Canvas-Redraw mit globalen Graphabfragen

- **Evidence:** Alle sichtbaren und offscreen Bloecke werden in mehreren
  Paessen verarbeitet; `movesWithDrag` fuehrt pro Block Graphsuchen aus;
  Android meldet langsame Draw Commands in jedem Drag-Frame.
- **Trigger:** jede neue `dragRender`-Instanz.
- **Scaling:** Workspace-Groesse und Reporter-/Container-Tiefe.
- **Impact:** sehr hoch; einzige Quelle mit direkter 100-%-Jank-Korrelation.
- **Safe boundary:** vorberechneter Render-Frame-Index und Viewport-Culling ohne
  Aenderung von Workflow-, Layout- oder Snap-Semantik.

### #2 Eager EMScript-/IR-Generierung im Host-Recomposition-Pfad

- **Evidence:** `codePreview` ruft `generateDraft` auf; LARGE P95 5,925 ms auf
  dem Host, obwohl ein Drag keine Semantik aendert.
- **Trigger:** jede tatsaechliche Host-Recomposition.
- **Scaling:** gesamte Dokumentgroesse und Ausdruckskomplexitaet.
- **Impact:** mittel bis hoch, vollstaendig vermeidbar.
- **Safe boundary:** Draft nur an Dokumentversion/persistente Mutation binden.

### #3 Snap-Semantik ueber globale WorkspaceGraph-Traversierungen

- **Evidence:** lokaler Anchor-Query, danach `findConnection`, `descendants`,
  `slotContaining` und Typinferenz pro Kandidat.
- **Trigger:** jeder Move in der Naehe von Anchors.
- **Scaling:** Workspace-Groesse, Graph-Tiefe und lokale Kandidatenzahl.
- **Impact:** mittel, in dichten Docking-Bereichen hoeher.
- **Safe boundary:** immutable Connection-/Parent-/Descendant-Indizes aus dem
  stabilen Dokument fuer die Drag-Dauer.

### #4 Mehrfacher Voll-Layoutaufbau bei Start und Drop

- **Evidence:** zwei bis drei Builds beim Start; 200er P95 9,428 ms auf Host;
  erneuter Build und Vollserialisierung beim Drop.
- **Trigger:** Long Press Start und Pointer Up.
- **Scaling:** gesamtes Dokument und Containerstruktur.
- **Impact:** mittel fuer Ansprechverzoegerung und Settle.
- **Safe boundary:** vorhandenes committed Layout wiederverwenden und nur ein
  benoetigtes Drag-Preview-Layout erzeugen.

### #5 Animatable-Fan-out und per-Block Draw-Allokationen

- **Evidence:** eine Coroutine/Animatable pro veraendertem Block; Shape-Requests,
  Definitionskopien, Filter-/Sortierlisten und Textmessungen im Draw-Pfad.
- **Trigger:** Drop/Reflow und jeder Canvas-Redraw.
- **Scaling:** Zahl geaenderter/gerenderter Bloecke.
- **Impact:** mittel, vor allem beim Settle.
- **Safe boundary:** koordinierter Animationsfortschritt und cachebare lokale
  Render-Geometrie.

## 18. Recommended Optimization Slices

### P1 - Derived Output vom Drag-Recomposition-Pfad trennen

- **Ziel:** Kein IR-/EMScript-Generatoraufruf waehrend eines semantikfreien Drag.
- **Minimale Aenderung:** `codePreview` als bei Dokumentaenderung aktualisierten
  Snapshot halten; Host liest keine berechnende Getter-Funktion.
- **Erwarteter Effekt:** LARGE spart pro tatsaechlicher Host-Recomposition die
  gemessenen circa 3,1 ms Median / 5,9 ms P95 Hostarbeit.
- **Acceptance:** Generator-Invocation-Count waehrend 100 Drag-Moves = 0;
  nach Drop exakt einmal beziehungsweise vorhandene Debounce-Semantik;
  Roundtrip-/Draft-Tests unveraendert gruen.
- **Risiko:** niedrig.

### P2 - Render-Frame-Index und Viewport-Culling

- **Ziel:** Draw-Kosten an tatsaechlich sichtbare plus gezogene Bloecke binden.
- **Minimale Aenderung:** Aus stabilem Layout einmal pro Dokumentversion
  vorberechnen: z-sortierte Bloecke, Inline-Reporter je Owner,
  Container-Visuals je Block, Drag-Closure/Parent-Index. Im Frame nur
  Viewport-Intersection und Translation.
- **Acceptance:** keine `WorkspaceGraph.isAttachedToDraggedAncestor`-Aufrufe im
  Draw-Pass; gezeichnete Blockzahl <= sichtbare Bloecke + Drag-Closure;
  LARGE Drag P95 muss gegen diese Baseline messbar sinken.
- **Risiko:** mittel; Culling muss grosse Container/Subtree-Bounds beachten.

### P3 - Drag-lokaler Snap-/Connection-Index

- **Ziel:** Nach raeumlichem Query keine globalen Connection-Scans pro Kandidat.
- **Minimale Aenderung:** beim Drag Start immutable Maps fuer Connection-Owner,
  Partner, Parent, Descendant und ValueType aus dem stabilen Dokument erstellen.
- **Acceptance:** `WorkspaceGraph.findConnection`-Count pro Move = 0 im
  Snap-Pfad; gleiche Snap-Ergebnisse fuer bestehende Interaktionstests.
- **Risiko:** mittel; Index darf nur fuer unveraendertes Drag-Dokument gelten.

### P4 - Settle-Animation koordinieren

- **Ziel:** ein Animations-Takt statt N unabhaengiger State-Produzenten.
- **Minimale Aenderung:** betroffene Blockpositionen in einer gemeinsamen
  Placement-Transition auswerten; unbetroffene Bloecke bleiben statisch.
- **Acceptance:** Animations-State-Producer O(1), keine Animation fuer
  unveraenderte Bloecke, Settle P95 unter der aktuellen 250-300-ms-Baseline.
- **Risiko:** mittel.

### P5 - Visual-Path-Provider cachebar machen

- **Ziel:** keine Definitionskopien und kein EMA-I/O/Decode im Draw-Pass.
- **Minimale Aenderung:** Presentation-Snapshot und Asset-Geometrie nach
  Asset-Revision, Definition-ID, Zielgroesse und Branch-Geometrie cachen.
- **Acceptance:** Datei-/Decode-/Morph-Count waehrend Drag = 0; Path-Cache-Hit
  fuer unveraenderte Bloecke; visuelle Golden-/Shape-Tests gruen.
- **Risiko:** niedrig bis mittel; Cache-Invalidierung bei Asset-Revision.

## 19. LayoutTree-Zukunft und Cache-Grenzen

Strukturell cachebar:

- gemessene Blockgroesse und lokale Blockgeometrie
- Branch-/Statement-Regionen relativ zum Block
- lokale Connector-Offsets und Hit-Primitives
- Definition-Presentation-Snapshot und Shape-Topologie
- Connection-Owner-/Parent-/Descendant-Indizes
- z-sortierte Renderliste und Inline-Reporter-Gruppierung

Wirklich positionsabhaengig waehrend Drag:

- Root-Translation des gezogenen Blocks/Subtrees
- virtuelle Anchor-Positionen der Drag-Quellen
- Distanz zum lokalen Snap-Ziel
- Auto-Scroll-abhaengige Viewport-Transformation

Das passt zur geplanten Kette `Semantic Structure -> LayoutTree ->
LayoutEnvelope -> Geometry -> Render`. P0 fuehrt diese Architektur nicht ein.

## 20. Risiken und offene Messluecken

- SMALL/MEDIUM haben keine separaten Android-Framebaselines; die vorhandenen
  Nutzerdaten wurden nicht fuer Benchmark-Fixtures ueberschrieben.
- Recomposition-, Compose-Measure- und Compose-Layout-Counts sind UNKNOWN.
- Kandidaten-/Graphquery-Invocation-Counts pro Move sind aus dem Codepfad
  belegt, aber nicht als permanente Runtime-Counter erhoben.
- GC-Zeit und Allocation Bytes pro Frame sind UNKNOWN.
- Das Testgeraet hatte keine gebundenen EMA-Assets; deren teurer Dateipfad ist
  statisch belegt, aber nicht Teil der gemessenen Framezeiten.
- Die ADB-Sequenz misst wenige, kontrollierte Moves. Ein Finger erzeugt mehr
  Events und kann die Queue zusaetzlich belasten.

## 21. Raw / Supporting Metrics

```text
IDLE (5 s):
frames=11, janky=5 (45.45%), p50=34 ms, p95=57 ms

LARGE simple active drag:
frames=9, janky=9 (100%), p50=200 ms, p95=300 ms
missedVsync=7, slowUiThread=9, slowDrawCommands=9

LARGE simple settle:
frames=8, janky=7 (87.50%), p50=150 ms, p95=250 ms
missedVsync=6, slowUiThread=7, slowDrawCommands=7

LARGE complex active drag:
frames=7, janky=7 (100%), p50=250 ms, p95=350 ms
missedVsync=6, slowUiThread=7, slowDrawCommands=7

LARGE complex settle:
frames=7, janky=7 (100%), p50=129 ms, p95=300 ms
missedVsync=6, slowUiThread=7, slowDrawCommands=7
```

Ein erster kombinierter Start/Drag/Drop-Lauf enthielt einen 3,5-s-Ausreisser.
Er wurde nicht als Baseline verwendet, weil die Phasen und ADB-Scheduling dort
nicht getrennt waren.

## 22. Audit Result

Die wichtigste Grenze ist nicht Workflow-Semantik gegen Drag, denn diese
Trennung existiert bereits. Die aktuelle Verluststelle liegt zwischen
transientem Drag-State und Darstellung: Ein kleiner Positionswechsel loest
breite Hostarbeit und einen globalen, graphintensiven Canvas-Redraw aus.

P1 soll deshalb klein und beweisbar beginnen: semantikfreie Codeprojektion aus
dem Drag-Pfad entfernen. P2 adressiert danach den groessten gemessenen Anteil,
ohne Workflow-, Connection- oder Layoutsemantik zu veraendern.
