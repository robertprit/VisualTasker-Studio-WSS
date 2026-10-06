# BlockEditor Drop Publish / Compose Stall Audit (P2B)

## 1. Ausgangsbefund

Nach einem Drop bleibt der LARGE-Workspace trotz flüssigem Drag mehrere Sekunden praktisch ohne neuen Frame. P2B zerlegt diesen Zeitraum diagnostisch; es enthält keine Performance-Optimierung.

Gemessen wurden 19 gültig klassifizierte Drops auf SM-S938B und SM-S918B mit dem LARGE-Workspace (181 Blöcke). Die Kernvergleichsgruppen umfassen je drei Linear-Detach-, Reporter-Detach-, Presentation-Move-, Linear-Reconnect-, Reporter-Move- und Control-Fallback-Drops. Ein zusätzlicher Linear-Detach auf SM-S918B trennte Parser und Workspace-Assembler. Reporter-Attach ließ sich per ADB nicht zuverlässig als Snap reproduzieren und wird nicht als Attach-Messung ausgegeben.

## 2. P2A-Kausaltrennung

P2A bleibt aktiv. Sichere Presentation- und Reporter-Moves verwendeten `ROOT_TRANSLATION` mit:

- `measuredBlocks=0`
- `reusedBlocks=181`
- `fullBuilds=0`
- `localOperations=1`

Trotzdem lagen diese Drops bei rund vier Sekunden bis zur Interaktivität. Das Layout ist damit für diese Fälle kausal ausgeschlossen. Die im Test gewählten strukturellen Detach/Reconnect- und Control-Fälle waren dagegen als `semantic-or-unproven-change` klassifiziert und verwendeten erwartungsgemäß den Full-Build-Fallback.

## 3. Instrumentation

`BlockEditorDropTrace` ist debug-only und korreliert alle Ereignisse über eine Drop-ID und Dokumentrevision. Erfasst werden monotone Wall-Zeit, Thread-CPU-Zeit, Threadname/-ID, Blockanzahl, Drop-Typ, Layoutdiagnostik, Serialisierungsgröße, Frame-Gaps und Placement-Animationen.

Zusätzliche Host-Marker trennen:

- Controller-Publikation und BlockEditor-Host-Callback
- Workspace Sync Guard und `WorkspaceWorkflowState`-Aufbau
- Source-Line-Auflösung für generierte Projektion und Text-Draft
- EMScript Parser, WorkspaceAssembler und Identity-Reconcile
- Workspace-Root, Panel-Schleife und BlockEditor-Composable

## 4. Timeline-Modell

Die T0-T24-Marker folgen dem realen Pfad:

`pointerUp -> endDrag/reducer -> history -> layout -> serialize -> callback -> state publication -> WSS sync/state rebuild -> source mapping -> Compose observed -> recomposition -> measure/layout -> draw -> first frame -> animation stable`.

Alle dominanten Phasen liefen synchron auf `main/2`.

## 5. DropToInteractive

Definition: T0 bis zum ersten Choreographer-Frame nach der neuen Dokumentpublikation (T23). Da während des dominanten Abschnitts kein Frame produziert wurde und der Main Thread CPU-gebunden war, ist T23 hier ein belastbarer Proxy für wieder mögliche Eingabeverarbeitung.

## 6. DropToStable

Definition: T0 bis zum Ende aller registrierten Placement-Animationen (T24). Diese Metrik bleibt getrennt von DropToInteractive.

## 7. Normal Drop Measurements

Linear Detach, SM-S938B, drei Wiederholungen:

| Metrik | Mittel | Bereich |
|---|---:|---:|
| DropToInteractive | 3925.5 ms | 3838.6-4068.6 ms |
| DropToStable | 4153.0 ms | 4039.4-4331.4 ms |
| endDrag/reducer | 8.3 / 8.2 ms | 6.2-12.4 / 6.1-12.2 ms |
| History | 0.64 ms | 0.03-1.85 ms |
| Layout | 19.36 ms | 12.95-31.81 ms |
| Controller-Serialisierung | 7.07 ms | 5.69-9.09 ms |
| Host-Callback gesamt | 33.13 ms | 24.41-44.49 ms |
| State Publication | 0.12 ms | 0.03-0.28 ms |
| Publication -> Compose observed | 3837.1 ms | 3775.0-3944.8 ms |
| Root Source-Line-Auflösung | 3615.3 ms | 3587.9-3663.8 ms |
| Recomposition | 3.96 ms | 3.66-4.15 ms |
| Compose Measure/Layout | 0.26 ms | 0.24-0.29 ms |
| Layout -> First Draw | 38.71 ms | 27.96-52.25 ms |
| Placement Animation | 250.91 ms | 215.16-299.18 ms |

## 8. Reporter Measurements

Reporter Detach, SM-S938B, drei Wiederholungen:

| Metrik | Mittel | Bereich |
|---|---:|---:|
| DropToInteractive | 3877.5 ms | 3844.7-3899.0 ms |
| DropToStable | 4085.2 ms | 4048.7-4105.0 ms |
| Root Source-Line-Auflösung | 3675.0 ms | 3640.7-3692.6 ms |
| Layout | 13.67 ms | 12.91-14.35 ms |

Reporter Move, drei Wiederholungen, bestätigte P2A-Lokalpfade:

| Metrik | Mittel | Bereich |
|---|---:|---:|
| DropToInteractive | 4064.8 ms | 3961.9-4180.9 ms |
| DropToStable | 4267.7 ms | 4168.0-4390.0 ms |
| Root Source-Line-Auflösung | 3879.1 ms | 3777.1-3991.9 ms |
| Layout | 4.36 ms | 4.05-4.52 ms |

Ein gültiger Reporter-Attach wurde durch die ADB-Geste nicht erreicht; zwei Versuche wurden korrekt als `REPORTER_MOVE` klassifiziert und nicht als Attach gewertet.

## 9. Control Fallback Measurements

Control/Branch Fallback, drei Wiederholungen:

| Metrik | Mittel | Bereich |
|---|---:|---:|
| DropToInteractive | 2312.6 ms | 2251.4-2431.9 ms |
| DropToStable | 2521.0 ms | 2461.7-2638.6 ms |
| Root Source-Line-Auflösung | 2092.1 ms | 2032.2-2206.9 ms |
| Layout | 42.21 ms | 39.54-44.92 ms |

Die geringere Zeit korreliert mit dem nach dem Detach deutlich kleineren/anders strukturierten generierten Script, nicht mit schnellerem Compose.

## 10. Thread Classification

Die dominante Phase läuft synchron auf dem Android-Main-Thread. CPU-Zeit liegt nahe an Wall-Zeit:

- Projektion-Assembler: 2835.7 ms wall / 2770.1 ms CPU
- Draft-Assembler: 2581.4 ms wall / 2530.3 ms CPU

Klassifikation: **CPU-BOUND**. Keine Dispatcher-Transition, kein Blocking Wait, kein Lock, kein Timeout und kein suspendierter Delay im dominanten Pfad.

## 11. Frame Evidence

Während der Source-Line-Auflösung wurde kein Choreographer-Frame produziert. Der erste Frame kam bei den drei normalen Drops nach 3838.6-4068.6 ms. Nach diesem ersten Frame lagen weitere große Gaps typischerweise bei etwa 40-83 ms während Draw/Placement-Animation.

Die größte beobachtete T0-bis-erster-Frame-Lücke war 5043.8 ms bei einem Linear-Reconnect; die größte normale Linear-Detach-Lücke war 4068.5 ms. Das ist ein eingefrorener Main Thread, kein bloßes Input-Gating.

## 12. Serialization

Der Controller serialisiert das gesamte 181-Block-Dokument einmal pro Drop (etwa 228 KiB) in rund 4-9 ms. Danach folgen weitere vollständige Serialisierungs-/Deserialisierungszyklen im Host, Sync Guard und Workflow-State-Aufbau. Diese Arbeit ist redundant und messbar, erklärt mit insgesamt niedrigen zweistelligen bis niedrigen dreistelligen Millisekunden aber nicht den Mehrsekundenstall.

## 13. Host Callback

`onWorkspaceDocumentChanged` ist synchron. Der unmittelbare Callback inklusive Dirty-Normalisierung brauchte bei normalen Drops durchschnittlich 33.1 ms. Anschließend beobachtet ein Host-Collector das Dokument, serialisiert erneut und führt synchron Sync Guard sowie `WorkspaceWorkflowState.fromSerialized` aus. Der BlockEditor wartet nicht in `endDrag`, aber die nachfolgende Main-Thread-Arbeit verhindert Frames und Input.

## 14. Compose Publication

Die eigentliche Controller-State-Publikation braucht durchschnittlich 0.12 ms. Der große Abstand `publication -> BlockEditorHost observes state` beträgt rund 3.84 s, liegt aber nach weiterer Instrumentierung fast vollständig **vor** dem BlockEditor-Composable in `WorkspaceSelectionResolver.sourceLines(...)`.

Der Workspace-State selbst war beim normalen SM-S938B-Lauf nach ungefähr 0.20-0.49 s aufgebaut; anschließend blockierte Source-Mapping den Root-Composable mehrere Sekunden.

## 15. Recomposition/Layout/Draw

Nach der Source-Line-Auflösung benötigt die BlockEditor-Recomposition nur etwa 3-4 ms, Compose Measure/Layout weniger als 0.4 ms und der erste Draw etwa 20-52 ms. Die Panel-Schleife und der BlockEditor-Composable selbst liegen im einstelligen Millisekundenbereich.

Damit ist der frühere Sammelbegriff "Compose-Lücke" präzisiert: Die teure Arbeit wird zwar synchron während einer Root-Recomposition aufgerufen, ist aber EMScript-Import/Workspace-Assembly und keine teure Compose-Komposition oder -Messung.

## 16. Dominant Stall

Konkreter Pfad:

`WorkspaceScreen.workspaceSourceLines remember -> WorkspaceSelectionResolver.sourceLines / derivedSourceLines -> EmscriptWorkspaceImporter.import -> WorkspaceAssembler.build -> WorkspaceReducer.reduce`.

Ein detaillierter SM-S918B-Lauf ergab:

| Teilphase | Dauer | CPU | Aufrufe/Umfang |
|---|---:|---:|---:|
| Projektion Parser | 9.9 ms | 9.9 ms | 40 Top-Level-Statements |
| Projektion WorkspaceAssembler | 2835.7 ms | 2770.1 ms | 179 Blöcke, 696 Reducer-Aufrufe |
| Projektion Identity-Reconcile | ca. 7 ms | ca. 7 ms | 179 Blöcke |
| Draft Parser | 6.3 ms | 6.3 ms | 41 Top-Level-Statements |
| Draft WorkspaceAssembler | 2581.4 ms | 2530.3 ms | 181 Blöcke, 706 Reducer-Aufrufe |
| Draft Identity-Reconcile | ca. 7 ms | ca. 7 ms | 181 Blöcke |

Der Assembler erzeugt das Dokument über hunderte einzelne `WorkspaceReducer.reduce(...)`-Aktionen und immutable Dokument-/Map-Kopien. Diese Arbeit wird zweimal pro Workspace-Revision ausgeführt.

## 17. Reporter Differential

Die erste messbare Differenz liegt nicht in Reducer, Layout, Serialisierung oder Compose. Reporter Detach war in diesen Messungen insgesamt sogar geringfügig schneller als Linear Detach; Reporter Move lag innerhalb der normalen Laufvarianz etwa 140 ms darüber. Beide Gruppen verlieren nahezu die gesamte Zeit in derselben Source-Line-/Assembler-Phase.

Die zuvor manuell beobachtete zusätzliche Reporter-Sekunde wurde nicht reproduziert. Daher ist keine reporter-spezifische Zusatzursache belegt. Ein echter Reporter-Attach bleibt als gezielte manuelle Abschlussprobe offen.

## 18. Root Cause Classification

**Root Cause:** Bei jeder `workflowState.revision`, einschließlich reiner View-/Positionsänderungen, berechnet `WorkspaceScreen` die Source-Line-Zuordnung neu. Dazu werden sowohl generiertes EMScript als auch der Text-Draft jeweils vollständig geparst und als neuer Workspace assembliert. Der Parser ist schnell; zwei synchrone `WorkspaceAssembler`-Läufe mit zusammen ungefähr 1400 Reducer-Aktionen blockieren den Main Thread für mehrere Sekunden.

Keine verdächtige 3-/4-Sekunden-Konstante wurde gefunden. Der vorhandene 200-ms-Debounce der abgeleiteten BlockEditor-Ausgaben ist nicht Teil der dominanten Phase. Im Messfenster gab es keinen signifikanten GC-Hinweis.

## 19. Recommended Smallest Fix

Ein eng begrenzter Folgeslice sollte:

1. Source-Line-Mappings nach semantischem Script-/Dokumentschlüssel statt nach der allgemeinen View-Revision wiederverwenden, sodass Position/Selection/Viewport keine Reimporte auslösen.
2. Projektion und unveränderten Draft nicht beide bei jeder Root-Recomposition neu importieren.
3. Für tatsächlich geänderte Scripts den WorkspaceAssembler in einem Batch/Bulk-Build aufbauen oder Source-Mapping direkt aus der bestehenden Parse-/Projektionspipeline übernehmen, statt 696-706 immutable Reducer-Aktionen auf dem Main Thread auszuführen.
4. Danach dieselbe P2B-Matrix erneut messen.

Keine dieser Maßnahmen wurde in P2B umgesetzt.

## 20. Remaining Unknowns

- Reporter-Attach wurde nicht gültig klassifiziert gemessen.
- Die genaue asymptotische Kurve des Assemblers wurde nicht mit mehreren Fixture-Größen vermessen; 179-181 Blöcke und rund 700 Reducer-Aufrufe belegen jedoch bereits den LARGE-Hotspot.
- Allocation/GC wurde nur über Logcat auf auffällige Pausen geprüft, nicht mit einem breiten Memory-Profil.
- T23 ist ein framebasierter Interaktivitäts-Proxy; ein separater synthetischer Input-Ack wurde nicht eingeführt.

## Test- und Build-Gates

- BlockEditor fokussierter Trace-Test: grün
- BlockEditor vollständiges `./gradlew test`: grün
- Hauptprojekt `:app:testDebugUnitTest`: grün
- Hauptprojekt `:app:assembleDebug`: grün
- Installation/Start und Drop-Messungen: SM-S938B und SM-S918B erfolgreich
- Commit/Push: nicht durchgeführt
