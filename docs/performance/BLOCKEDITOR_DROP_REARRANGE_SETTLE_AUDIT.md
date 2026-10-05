# BlockEditor Drop / Rearrange / Settle Performance Audit

Stand: 2026-10-05
Scope: P2 Diagnose, keine Optimierung, kein M2-2

## 1. Executive Summary

Der wahrnehmbare Engpass liegt nach `Pointer Up`. Ein lokaler Drop erzeugt nur
einen persistenten Commit und einen fachlichen Layout-Aufruf, dieser
`LayoutEngine.build` vermisst aber immer das vollstaendige Dokument. Im
LARGE-Workspace wurden bei einer Aenderung an zwei beziehungsweise drei
Bloecken alle 181 Bloecke vermessen, 98 beziehungsweise 143 Zielpositionen
geaendert und ebenso viele unabhaengige Placement-Animationen gestartet.

Der synchrone Pointer-Up-Pfad blockierte im voll instrumentierten Lauf 65,534
ms. Darin lagen 14,519 ms Layout, 9,479 ms Serialisierung und 30,050 ms
Host-Callback. Die Zielanimation wurde erst 2.253,814 ms nach Pointer Up vom
Canvas beobachtet und lief danach 240,101 ms. Ein zweiter RECONNECT-Lauf
benoetigte insgesamt 4.380,141 ms bis Stable. Die grosse Luecke vor dem
Animationsstart ist durch die aktuellen Messgrenzen nicht auf eine einzelne
Funktion reduzierbar; sie liegt nach Controller-Commit und vor der
`LaunchedEffect`-Beobachtung in Host-/Compose-Publikation, Recomposition und
erstem Rendern.

Es gibt keine Layout-Kaskade pro Animationsframe, keine Retarget-Schleife und
keine Projektion pro Frame. P1 bleibt wirksam. Die Evidenz empfiehlt als
kleinsten Folgeslice **P2A - Affected-Subtree Rearrange and Bounded Settle**:
zuerst die lokale Dirty-/Affected-Menge bestimmen, dann nur diese Geometrie
neu berechnen und nur tatsaechlich betroffene Bloecke animieren. Die lange
Publish-to-observed-Luecke muss dabei als eigenes Gate erneut instrumentiert
werden.

## 2. Problem Definition

Gemessen wird `DropToStableMs`, nicht Active-Drag-Performance:

```text
T0 Pointer Up
  -> Drop/Snap aufloesen
  -> persistente Mutation + History
  -> vollstaendiges Layout
  -> Serialisierung + Host-Callback
  -> debounced Validation/Projektion
  -> Compose beobachtet neue documentVersion
  -> Placement-Animationen
T1 letzte durch den Drop gestartete Animation beendet
```

## 3. P0/P1 Context

P0 belegte globales Zeichnen und viele Placement-`Animatable`s. P1 entfernte
EMScript-/IR-Projektion aus transienten UI-Aenderungen. P2 hat P1 nicht
zurueckgebaut. Eine semantische Drop-Mutation erzeugte genau eine debounced
Projektion; Animationsframes erzeugten keine weitere.

## 4. Actual Drop Pipeline

| Phase | Klasse/Funktion | Input -> Output | Mutation | Ausfuehrung |
|---|---|---|---|---|
| Pointer Up | `WorkspacePointerGestures.workspacePointerGestures` | letzter Screen-Punkt | keine | synchron, Main |
| Drop | `BlockEditorController.onPointerUp` | `DragRenderState`, Dokument | Selection/Drag-State | synchron, Main |
| Resolution | `DragOperations.endDrag` | `TransientEditorState`, Dokument | finales Dokument | synchron, Main |
| Reducer | `WorkspaceReducer.reduce` | Disconnect/Connect/MoveRoot | immutable Dokumentkopien | synchron, Main |
| History | `applyPersistentDocumentChange` | finales Dokument | ein `WorkspaceState.record` | synchron, Main |
| Commit | `applyWorkspaceState` | WorkspaceState | Dokument/semantic key | synchron, Main |
| Layout | `LayoutEngine.build` | gesamtes Dokument | neuer `LayoutCache` | synchron, Main |
| Publish | `WorkspaceSerializer.serialize` + Host-Callback | Dokument | Dirty-/Host-State | synchron, Main |
| Derived | `scheduleDerivedOutputs` | semantic-key delta | pending projection | asynchron/debounced |
| Animation setup | `EditorCanvasLayer` `LaunchedEffect(documentVersion)` | alle visible targets | Animatable targets | Main + Coroutines |
| Settle | je geaendertem Block `Animatable.animateTo` | old -> target | visueller Offset | asynchron, Main frames |

`DragOperations.endDrag` kann intern Disconnect, Connect und MoveRoot
aufeinander anwenden und dadurch Dokumentversionen erhoehen. Der Controller
publiziert aber nur das finale Dokument einmal als persistenten Commit.

## 5. Timing Boundaries

Debug-only Instrumentierung nutzte `elapsedRealtimeNanos`. Sie wurde nach der
Messung vollstaendig entfernt.

| Device-Lauf | Typ | T0 -> Layout-Ende | Pointer-Up Return | Layout beobachtet | Animation | DropToStable |
|---|---|---:|---:|---:|---:|---:|
| LARGE-1 | einfacher RECONNECT | 26,876 ms | nicht separat erfasst | 4.124,528 ms | 255,613 ms | 4.380,141 ms |
| LARGE-2 | CONTROL/SUBTREE DETACH | 25,438 ms | 65,534 ms | 2.253,814 ms | 240,101 ms | 2.493,914 ms |
| LARGE-3 | freier SUBTREE MOVE | 21,046 ms | 62,340 ms | 2.292,180 ms | 254,203 ms | 2.546,383 ms |

`DropToCommitMs` ist nur bis Layout-Ende belastbar. Ein eigener expliziter
Stable-State existiert nicht. Fuer P2 bedeutet Stable: alle durch die neue
`documentVersion` gestarteten Placement-Animationen sind beendet; bei null
Animationen ist der Layout-Beobachtungszeitpunkt Stable.

SMALL und MEDIUM wurden auf dem echten Geraet nicht reproduzierbar als
Drop-to-Stable-Fixture geladen. Ihre Werte bleiben `UNKNOWN`. Die P0
Host-Layoutprobe (10: 0,620 ms Median; 50: 1,209 ms; 100: 2,071 ms; 200:
6,821 ms) ist nur Skalierungskontext, kein Ersatz fuer Device-DropToStable.

## 6. Main Thread Analysis

LARGE-2 blockierte den synchronen Eventpfad 65,534 ms. Davon:

- Layout: 14,519 ms
- Serialisierung: 9,479 ms
- Host-Callback: 30,050 ms
- Drop/Reducer, semantic key, History und Rest: etwa 11,486 ms

Danach blieb die UI-Pipeline bis zum beobachteten Animationstarget lange
zurueck. Das ist nicht als durchgehend synchroner Main-Thread-Freeze bewiesen.
Die Evidenz zeigt eine Kombination aus einem echten 65-ms-Block und stark
verspaetetem sichtbaren Settle. Die EMScript-Projektion lief debounced auf
einem Worker-Thread, dauerte 2,931 ms und war nicht der Freeze.

## 7. Rearrange Analysis

`LayoutEngine.build` ist global:

1. `LayoutMeasurePass.measure(document)` vermisst alle Bloecke.
2. `WorkspaceGraph.topLevelRoots` verarbeitet alle Roots.
3. `layoutBlock` baut sichtbare Bloecke, Container, Reporter, Statement-Slots,
   Branches, Hit-Primitives und Anchors rekursiv neu auf.
4. Hit- und Anchor-Spatial-Indizes werden geleert und vollstaendig neu gefuellt.
5. `LayoutPlacePass.fromFlatIndex` baut den Placed Tree neu.

Reporter, Statement-Container und IF/ELSE-Strukturen werden rekursiv neu
berechnet. AutoArrange wird nicht aufgerufen; die globale Arbeit stammt aus
dem normalen Layoutaufbau. Root-Positionen werden nicht global geschrieben:
im Messlauf gab es genau eine Root-Positionsmutation.

## 8. Affected-Set Analysis

| Lauf | Total | semantisch geaendert | vermessen | repositioniert | animiert |
|---|---:|---:|---:|---:|---:|
| einfacher RECONNECT | 181 | 3 | 181 | 143 | 143 |
| CONTROL/SUBTREE DETACH | 181 | 2 | 181 | 98 | 98 |
| freier SUBTREE MOVE | 181 | 0 | 181 | 98 | 98 |

`affectedBlocks` ist hier bewusst die Menge strukturell geaenderter
Blockknoten. Die Layout-Affected-Closure ist deutlich groesser. Eine lokale
Connection-Aenderung wird damit global vermessen und verschiebt grosse Teile
des nachfolgenden Layouts.

## 9. Measure/Layout Analysis

Pro gemessenem Drop wurde genau ein `LayoutEngine.build` nach dem finalen
Commit ausgefuehrt. Derselbe Subtree wurde nicht mehrfach durch mehrere
externe Layout-Passes berechnet. Intern traversiert der globale Pass jedoch
das gesamte Dokument und verwendet wiederholte Graphsuchen. LARGE benoetigte
14,519 bis 19,788 ms fuer diesen einen Pass.

Container-/Geometrie-Unterzaehler waren ohne Produktionsumbau nicht stabil
isolierbar und bleiben `UNKNOWN`. Sicher ist: `measuredBlocks == totalBlocks`.

## 10. Position Mutation Analysis

Der persistente Zustand schrieb pro Lauf genau eine Root-Position. Danach
wurden finale Zielpositionen einmal berechnet. Animationsframes mutieren nicht
das `WorkspaceDocument` und rufen `LayoutEngine.build` nicht erneut auf. Das
vorhandene Modell entspricht daher grundsaetzlich `final target once -> visual
transition`; problematisch ist die globale Zielberechnung und die grosse Zahl
geaenderter Ziele.

## 11. Animation Analysis

`EditorCanvasLayer` haelt genau ein `Animatable<Offset>` pro sichtbarem Block,
nicht getrennt fuer X/Y und nicht pro Connector. Die Instanzen werden in einer
remembered Map wiederverwendet. Fuer jede geaenderte Zielposition startet eine
eigene Coroutine mit 170-ms-Tween. Unveraenderte Ziele starten keine Animation.

Im LARGE-Test existierten 181 Instanzen/Targets. 98 beziehungsweise 143
Animationen starteten. Wegen Frame-Verzoegerung endeten die nominalen
170-ms-Tweens nach 240 bis 256 ms. Vier bis sechs Canvas-Draws wurden zwischen
Target-Beobachtung und Stable registriert.

## 12. Retarget Analysis

Alle Device-Laeufe hatten null Retargets. Animationsframes loesten kein neues
fachliches Layout aus. H7 und H8 sind fuer diese Laeufe nicht signifikant.

## 13. Draw Analysis

Der gemeinsame Canvas wird bei jedem Animatable-Update invalidiert. Zwischen
Animationsstart und Stable wurden vier bis sechs Draws beobachtet.
Einzelne Draw-Zeit wurde nicht zuverlaessig isoliert. Der lange Zeitraum vor
`layoutObserved` zeigt, dass die sichtbare Verzoegerung nicht allein die
nominale Tween-Dauer ist. Ob ein einzelner teurer Draw oder Scheduling/
Recomposition diese Luecke dominiert, bleibt `INCONCLUSIVE`.

## 14. Allocation/GC Analysis

Pro Drop entstehen mindestens das finale immutable Dokument, History-State,
vollstaendige Layout-/Flat-/Placed-Listen, Hit-Primitives, Anchors,
Target-Map und Coroutine-Starts. Ein belastbarer GC-Stall wurde nicht
aufgezeichnet. H12 bleibt deshalb `INCONCLUSIVE`; Allokationen sind ein Risiko,
aber keine belegte Hauptursache.

## 15. Workspace Scaling

Die globale Messmenge skaliert mit der Workspace-Groesse. Die Zahl der
Animationen skaliert dagegen mit den durch die neue Struktur verschobenen
Zielen. P0-Hostdaten zeigen steigende lineare Layoutkosten; P2-Device-Daten
zeigen, dass bei 181 Bloecken selbst eine lokale Mutation 98 bis 143 Ziele
verschieben kann. Device-SMALL/MEDIUM bleiben `UNKNOWN`.

## 16. Subtree Complexity Scaling

Ein isolierter, gleich grosser linear-vs-verschachtelt Device-Vergleich wurde
nicht zuverlaessig hergestellt. Der komplexe LARGE-Kontrollsubtree verschob
98 Ziele bei 14,519 ms Layout; der einfache RECONNECT verschob 143 Ziele bei
19,788 ms. Damit korrelieren Kosten in diesen Laeufen staerker mit der
resultierenden Layout-Closure als mit dem Label "Control". Reine
Komplexitaetsskalierung bleibt `INCONCLUSIVE`.

## 17. Locality Test

Die lokale Mutation ist semantisch klein, das Rearrange nicht:

```text
totalBlocks       181
changedBlocks       2 / 3
measuredBlocks     181 / 181
repositioned        98 / 143
animated            98 / 143
layout passes        1
semantic commits     1
root position writes 1
projection calls     1 bei semantischer Aenderung
```

Beim freien Subtree-Move waren `changedBlocks = 0`, eine Root-Position wurde
geschrieben und die semantische Projektion blieb bei null. Der temporaere
Drop-Typ-Zaehler bezeichnete ihn wegen des internen `nextChain`-Checks als
`DETACH`; die Dokumentdifferenz belegt jedoch einen reinen Presentation-Move
des bereits freien 98-Block-Subtrees.

Das aktuelle Rearrange ist global in Measure/Index/Geometry und grossflaechig
in Position/Animation.

## 18. Hypothesis Matrix H1-H13

| Hypothese | Status | Evidenz |
|---|---|---|
| H1 globales Rearrange | CONFIRMED | 181/181 vermessen |
| H2 zu grosse affected closure | CONFIRMED | 2-3 geaendert, 98-143 repositioniert |
| H3 wiederholtes Layout | NOT SIGNIFICANT | ein externer Layout-Pass/Drop |
| H4 Main-Thread Measure/Layout | CONTRIBUTING | 14,5-19,8 ms, 65,5 ms Gesamtblock |
| H5 zu viele Animatable | CONTRIBUTING | 98/143 parallele Starts |
| H6 unveraenderte Bloecke animiert | NOT SIGNIFICANT | Starts nur bei Target-Aenderung |
| H7 Retargeting | NOT SIGNIFICANT | 0 Retargets |
| H8 Animation triggert Layout | NOT SIGNIFICANT | kein Layout pro Frame |
| H9 Graph-Traversierungen dominieren | INCONCLUSIVE | global vorhanden, nicht separat getimt |
| H10 Geometry/Shape dominiert | INCONCLUSIVE | global erneuert, nicht separat getimt |
| H11 Canvas Draw dominiert | CONTRIBUTING | voller Canvas, 4/6 Draws; Einzelkosten unbekannt |
| H12 Allocation/GC dominiert | INCONCLUSIVE | Allokationen sichtbar, kein GC-Beleg |
| H13 mehrere Commits | NOT SIGNIFICANT | 1 Commit, 1 Layout-Pass |

## 19. Top Hotspots

1. **Host/Compose publish-to-observed path** - nach Controller-Return bis
   `EditorCanvasLayer` die neue Version beobachtet; einmal/Drop; 2,19 bis 4,10
   Sekunden Luecke; genaue Unterphase noch offen. Sichere Grenze: nur erneut
   instrumentieren, noch nicht offloaden.
2. **`LayoutEngine.build`** - Commit/Layout; einmal/Drop; 181 gemessene
   Elemente; 14,519-19,788 ms. Sichere Grenze: affected subtree statt
   Workflow-/Connection-Semantik aendern.
3. **`EditorCanvasLayer` Placement-Animationen** - Settle; 98/143 Coroutines
   und Animatables; 240-256 ms. Sichere Grenze: ein begrenzter Transition-Satz
   fuer die ermittelte affected closure.
4. **`WorkspaceSerializer.serialize` + `BlockEditorShellPlugin` Callback** -
   Publish; einmal/Drop; 9,479 + 30,050 ms. Sichere Grenze: Dirty-Ermittlung
   und Persistenzbenachrichtigung entkoppeln, Vertragsausgabe erhalten.
5. **`DragOperations.endDrag`/Reducer/semantic key/History** - Drop/Commit;
   einmal/Drop; Restbudget rund 11,5 ms im vollstaendigen Lauf. Sichere Grenze:
   finales Dokument weiterhin atomar genau einmal publizieren.

## 20. Recommended P2A

**P2A - Affected-Subtree Rearrange and Bounded Settle**

1. Vor Layout eine fachlich korrekte dirty/affected closure aus Connection-
   Delta, Parent/Next-Chain und Container-Abhaengigkeiten bestimmen.
2. Unveraenderte measured/placed Geometrie und Spatial-Index-Eintraege
   wiederverwenden.
3. Nur geaenderte Zielpositionen in den Transition-Satz aufnehmen.
4. Publish-to-observed-Grenzen beibehalten und feiner messen.
5. Gate: Stable IDs, Snap/Detach/Reconnect, Branches und P1-Projektionszahl.

Erwarteter Hebel: Messmenge von 181 auf die tatsaechliche closure reduzieren,
Animationsstarts von 98/143 auf lokal verschobene Elemente begrenzen und die
Drop-to-Stable-Latenz aus dem Sekundenbereich holen. Die exakte Verbesserung
ist vor Implementierung nicht serioes quantifizierbar.

## 21. Explicitly Deferred Work

Nicht implementiert: Dirty-Subtree-Layout, Incremental Layout,
`LayoutEnvelope`, `RenderFrameIndex`, Viewport-Culling, konsolidierter Animator,
Background Layout, Spatial-Index-/Shape-Cache-Redesign, M2-2 oder Aenderungen
an Workflow-/Connection-/IR-/Persistence-Semantik.

## Test Environment and Discipline

- Geraet: Samsung SM-S938B, Android 16/API 36, 1440 x 3120
- Root: `3b767c0`; BlockEditor: `8f77a59`; FlowEditor: `008b136`
- LARGE: geladener Command-Catalog-Breadth Workspace, 181 Bloecke
- Device-Szenarien: einfacher Reconnect, Control/Subtree-Detach und freier
  98-Block-Subtree-Move; isolierter Single-Block-Move und SMALL/MEDIUM blieben
  ohne falsche Ersatzmessung `UNKNOWN`
- Instrumentierter Build und Installation erfolgreich
- temporaere Instrumentierung nach Messung vollstaendig entfernt
- ShapeMaker-WIP und `tmp_device_live.png`/`tmp_factory_check.png` unangetastet
