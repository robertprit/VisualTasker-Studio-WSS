# BlockEditor Affected-Structure Rearrange

Stand: 2026-10-05
Scope: P2A, bestehende LayoutEngine, kein LayoutTree-Redesign, kein M2-2

## 1. P2 Baseline

Der P2-Audit zeigte im LARGE-Dokument mit 181 Bloecken bei jedem Drop einen
vollstaendigen `LayoutEngine.build`. Presentation Move, Detach und Reconnect
vermassten jeweils 181 Bloecke. Dabei wurden 98 bis 143 Zielpositionen
veraendert und ebenso viele Placement-Animationen gestartet. Die gemessenen
Drop-to-Stable-Zeiten lagen zwischen 2.493,914 und 4.380,141 ms.

## 2. Existing Layout Dependency Graph

Der bestehende Pfad lautet:

```text
WorkspaceDocument
  -> LayoutMeasurePass.measure (Blockmasse fuer alle Bloecke)
  -> LayoutEngine.layoutBlock (Roots, Chains, Reporter, Statements, Branches)
  -> FlatLayoutIndex
       block bounds
       subtree bounds
       hit primitives
       connection anchors
       statement slots
       branch sections
       inline reporter layouts
       hit/anchor spatial index
  -> LayoutPlacePass.fromFlatIndex
  -> MeasuredLayoutTree + PlacedLayoutTree + LayoutCache
```

`MeasuredLayoutTree` und `PlacedLayoutTree` sind abgeleitete, immutable und
rekonstruierbare Ergebnisse. Die kanonische Wahrheit bleibt das
`WorkspaceDocument`. Der normale Layoutpfad fuehrt kein AutoArrange aus; die
globale Arbeit entstand allein durch den vollstaendigen Layoutaufbau.

## 3. Dirty Propagation Contract

`LayoutEngine.update(previousDocument, document, previousCache)` entscheidet
vor einem Full Build, ob die Dokumentdifferenz als bewiesene starre
Geometrieaenderung behandelt werden kann. Die Diagnose unterscheidet:

- `ROOT_TRANSLATION`
- `RIGID_CHAIN_RELINK`
- `FULL_BUILD`

Dirty bedeutet dabei die direkt veraenderte persistente Struktur, nicht
automatisch deren kompletter Root-Subtree. Eine Wiederverwendung ist nur
zulaessig, wenn Blockdefinition, Felder, Inputs, Reporter, Statements,
Branches und Variablen unveraendert sind oder die Aenderung nachweislich nur
lineare Previous-/Next-Links betrifft.

## 4. Upward Invalidation

Presentation-Moves haben keine Mass-Abhaengigkeit nach oben. Ein einfacher
Tail-Connect oder Tail-Detach veraendert ebenfalls keine Blockmasse. Fuer
Reporter-, Statement-, Branch-, Field- oder Variable-Aenderungen ist eine
begrenzte Masspropagation mit der aktuellen Engine noch nicht beweisbar;
diese Faelle verwenden den Full-Build-Fallback.

## 5. Downward Placement Invalidation

Beim Translation-Pfad wird ausschliesslich die Closure des bewegten Roots
verschoben. Beim linearen Relink ist das die eingefuegte beziehungsweise
abgetrennte Tail-Closure. Andere Roots und ihre Geometrie bleiben
unveraendert. Der Pfad wird abgelehnt, sobald Root-Reihenfolge, Closure oder
Connection-Vertrag nicht eindeutig erhalten bleiben.

## 6. Layout Result Reuse

Unveraendert wiederverwendet werden:

- gemessene Blockresultate
- relative Block- und Subtree-Geometrie
- relative Hit-Primitives
- relative Connection-Anchors
- Statement-Slots und Branch-Sections
- Inline-Reporter-Geometrie

Die betroffenen Rechtecke und Anchors werden um einen gemeinsamen Delta-Vektor
verschoben. Hit- und Anchor-Indizes werden aus der korrekten finalen Geometrie
neu aufgebaut, weil der vorhandene `SpatialIndex` keine sichere lokale
Update-API anbietet. Der Placed Tree wird aus dem finalen Flat Index
rekonstruiert. Es entsteht keine zweite Workspace-Wahrheit.

## 7. Presentation Move Fast Path

Wenn ausschliesslich Positionen bestehender Top-Level-Roots geaendert wurden,
bleiben interne Masse und relative Geometrie unveraendert. Der Fast Path
verschiebt jede betroffene Root-Closure starr und setzt:

```text
measuredBlocks = 0
reusedLayoutBlocks = totalBlocks
fullBuilds = 0
localLayoutOperations = Anzahl bewegter Roots
```

Der 181-Block-Test verschiebt eine freie 98-Block-Closure. Ergebnis:
0 vermessen, 181 wiederverwendet, 98 repositioniert. Das Ergebnis ist bis auf
alle Flat-/Measured-/Placed-Geometrien identisch zum globalen Orakel.

## 8. Semantic Drop Path

Ein bewiesener einfacher Connect/Detach wird lokal behandelt, wenn:

- exakt zwei Bloecke nur in Previous-/Next-Verbindungen geaendert sind,
- ein kompletter letzter Root an das Ende des unmittelbar vorherigen Roots
  angehaengt oder von dort getrennt wird,
- die bewegte Closure vor und nach der Mutation identisch ist,
- Root-Reihenfolge und Z-Order keine globale Neuanordnung verlangen.

Im 181-Block-Test werden beim Connect und Detach jeweils 0 Bloecke neu
vermessen, 181 Ergebnisse wiederverwendet und die 98-Block-Tail-Closure
repositioniert.

## 9. Full Layout Fallback

Unbewiesene oder massrelevante Aenderungen bleiben konservativ global. Das
betrifft aktuell insbesondere:

- Reporter Attach/Detach und verschachtelte Reporter
- Statement Attach/Detach
- IF/ELSEIF/ELSE- und Loop-Containeraenderungen
- Field-, Variable-, Branch- und unbekannte Strukturmutationen
- lineare Relinks, deren Root-/Z-Order-Vertrag nicht eindeutig ist

Der Fallback ist ueber `LayoutUpdateDiagnostics` sichtbar und nennt
`semantic-or-unproven-change` als Grund. Tests vergleichen jeden dieser Faelle
mit einem frischen globalen Build.

## 10. Geometry Correctness

Incremental-Resultate werden gegen `LayoutEngine.build(after)` als Orakel
verglichen. Geprueft werden sichtbare Bloecke, Block- und Subtree-Bounds,
Hit-Primitives, Connection-Anchors, Statement-Slots, Branch-Sections,
Inline-Reporter, Measured Tree und Placed Tree. Die Focused Matrix ist gruen.

## 11. Interaction Geometry Correctness

Translation aktualisiert Hit-Primitives und Connection-Anchors und baut beide
Spatial-Indizes aus der finalen Geometrie neu auf. Point-Hit- und Anchor-Query
sind nach Translation gruen. Die bestehenden Interaction-/Snap-Tests laufen
im vollstaendigen BlockEditor-Gate unveraendert mit.

## 12. Host/Compose Timing

Der instrumentierte Build startete und renderte das persistierte LARGE-Dokument
mit 181 Bloecken auf dem aktuell verbundenen SM-S918B ohne Crash oder ANR.
Automatisierte ADB-Drags griffen im stark verkleinerten Canvas jedoch nicht
reproduzierbar einen Block, sondern pannten den Viewport. Deshalb existiert
fuer P2A keine belastbare neue Controller-to-LayoutObservation- oder
Drop-to-Stable-Device-Zeit. Die temporaeren Messpunkte wurden wieder entfernt.

Die P2-Luecke von 2,19 bis 4,10 Sekunden zwischen Controller und
Layout-Beobachtung bleibt damit offen und darf nicht der LayoutEngine allein
zugeschrieben werden.

## 13. Serialization Timing

P2 mass 9,479 ms Serialisierung und 30,050 ms Host-Callback in einem Lauf.
P2A veraendert weder Serializer noch Host-Callback. Wegen des nicht
reproduzierbaren Device-Drops gibt es keine belastbare neue Vergleichszahl.

## 14. LARGE Before/After

| Fall | Total | Dirty | Measured P2 | Measured P2A | Reused P2A | Repositioned P2 | Repositioned P2A |
|---|---:|---:|---:|---:|---:|---:|---:|
| freier 98er Root-Move | 181 | 1 Presentation-Root | 181 | 0 | 181 | 98 | 98 |
| einfacher Reconnect, 98er Tail | 181 | 2 | 181 | 0 | 181 | 143 Baseline | 98 Testfixture |
| einfacher Detach, 98er Tail | 181 | 2 | 181 | 0 | 181 | 98 | 98 |
| Control-/massrelevante Mutation | 181 | 1+ | 181 | 181 | 0 | strukturabhaengig | strukturabhaengig |

Die P2A-Werte sind deterministische Struktur-/Orakeltests, keine behaupteten
Device-Zeitmessungen. Repositionierte Elemente sind beim starren Move
tatsaechlich bewegt und bleiben deshalb korrekte Animationstargets.

## 15. Remaining Hotspots

1. Reporter-/Statement-/Control-Masspropagation ist noch global.
2. Der Host-/Compose-Publish-to-observed-Pfad ist auf dem Device nicht neu
   aufgeloest.
3. Eine 98-Block-Translation erzeugt weiterhin 98 legitime Animationen.
4. Spatial-Indizes werden korrekt, aber weiterhin global rekonstruiert.
5. Serialisierung und Host-Callback bleiben unveraendert.

## 16. Recommendation

P2A stellt fuer die haeufigen und beweisbaren Move-/Tail-Relink-Pfade echte
Layout-Lokalitaet her. Vor einem P2B muss ein reproduzierbarer Device-Fixture-
oder Test-Hook denselben 181-Block-Drop deterministisch ausloesen und die
Controller-/Publish-/Compose-/Animation-Grenzen erneut messen. Erst danach
darf entschieden werden, ob P2B den Host-Publish-Pfad, Animationen oder eine
weitere lokale Masspropagation adressiert. M2-2 bleibt bis zu diesem
Performance-Gate unberuehrt.
