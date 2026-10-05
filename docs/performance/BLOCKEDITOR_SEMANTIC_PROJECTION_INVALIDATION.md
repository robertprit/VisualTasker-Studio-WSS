# BlockEditor Semantic Projection Invalidation

Stand: 2026-10-04
Scope: P1, keine Viewport-Culling-, Render- oder M2-2-Arbeit

## Ergebnis

`codePreview` ist jetzt ein gecachter, abgeleiteter Snapshot des letzten
erfolgreich projizierten semantischen Dokumentzustands. Reine UI-Bewegung ruft
den EMScript-/IR-Generator nicht mehr auf. Code-relevante persistente
Mutationen invalidieren die Projektion weiterhin debounced und aktualisieren
Preview sowie Host-Callback.

## BEFORE Dependency Chain

```text
Pointer MOVE
  -> BlockEditorController.dragRender = render.copy(...)
  -> BlockEditorHost recomposes
  -> controller.codePreview
  -> generateDraft(false)
  -> WorkspaceCodeGenerator.generate(document)
  -> EmscriptGenerator
  -> IrGenerator.generate(document)
  -> kompletter Canvas-Draw
```

Obwohl `WorkspaceDocument` waehrend Active Drag unveraendert blieb, war der
Getter selbst rechenaktiv. Jede Host-Recomposition konnte deshalb die gesamte
Codeprojektion erneut ausfuehren.

## AFTER Dependency Chain

```text
Transient UI state
  -> Drag / Selection / Snap Preview / Viewport / Animation
  -> Host recomposes
  -> controller.codePreview
  -> lastValidDraft                         (kein Generator)

Persistent document mutation
  -> semanticProjectionKey(newDocument)
  -> Key geaendert?
       nein -> Validation, keine Codeprojektion
       ja   -> debounced WorkspaceCodeGenerator.generate(document)
            -> lastValidDraft + Host-Callback
```

`regenerateCode()` bleibt der explizite, sofortige Regenerationspfad.

## Semantic Invalidation Key

Der Schluessel wird ausschließlich bei einer persistenten Dokumentaenderung
berechnet, nie pro Pointer-, Draw- oder Animationsframe. Er umfasst:

- Dokument-ID und Root-Reihenfolge,
- Block-ID und Blocktyp,
- alle Felder,
- Previous-/Next-/Output-Verbindungen,
- Value- und Statement-Inputs inklusive Verbindungen,
- die code-relevanten Metadaten `emscript.declaredType` und `if.branchCount`,
- die Variable Registry.

Bewusst ausgeschlossen sind:

- Dokumentversion,
- `rootPositions` und Legacy-Root-Koordinaten,
- Collapse-Zustand,
- Reporter-Darstellungsmodus und sonstige Presentation-Metadaten,
- Selection, Hover, Fokus, Drag- und Snap-Preview-State,
- Viewport und Animation.

Damit bleibt das kanonische `WorkspaceDocument` die einzige Wahrheit. Der Key
ist kein zweites semantisches Modell und kein String-/JSON-Cache; er ist nur
eine minimale Invalidierungsgrenze vor dem bestehenden Generator.

Mehrere persistente Aenderungen innerhalb des Debounce-Fensters koennen eine
bereits anstehende semantische Projektion nicht durch eine spaetere reine
Presentation-Aenderung verlieren.

## Regressionstests

Instrumentierte Controller-Tests mit injiziertem Invocation Counter sichern:

- wiederholtes `codePreview`-Lesen: Delta 0,
- Selection, Fokus, Zoom: Delta 0,
- 100 Drag-Moves: Delta 0,
- reiner Root-Positions-Drop: Delta 0,
- sichtbarer Snap-Kandidat: Delta 0,
- Connection Commit: genau eine neue Projektion,
- semantischer Field Edit: genau eine neue Projektion und aktuelles Preview,
- explizites `regenerateCode()`: sofortige Projektion,
- Generatorfehler behalten den letzten gueltigen Draft.

Animationen koennen die Projektion nicht mehr ausloesen, weil der vom
Composable gelesene Getter nur `lastValidDraft` zurueckgibt und kein
Animations-State Bestandteil des persistenten Invalidierungswegs ist.

## P0/P1 Performance

Geraet: Samsung SM-S918B, Android 16, LARGE Command-Catalog-Breadth mit
181 Bloecken. Die Generatorprobe verwendet dasselbe Fixture und dieselbe
Host-JVM-Methode wie P0.

| Messwert | P0 | P1 |
|---|---:|---:|
| Generator Median | 3,126 ms | 2,849 ms |
| Generator P95 | 5,925 ms | 4,490 ms |
| Repraesentativer Active-Drag Median | 200 ms | 125 ms |
| Repraesentativer Active-Drag P95 | 300 ms | 150 ms |
| Janky Frames | 100 % (9/9) | 100 % (9/9) |
| Projection Calls bei 100 transienten Moves | bis zu 100 | 0 |

Ein zusaetzlicher Lauf mit Auto-Scroll-/komplexerer Draw-Arbeit ergab 12/12
janky Frames, Median 150 ms und P95 400 ms. Er wird nicht als Verbesserung
verkauft: Device-Framewerte bleiben stark vom sichtbaren Subtree, Auto-Scroll
und ADB-Scheduling abhaengig. Belegt ist die eliminierte Projektionsarbeit;
der verbleibende Hauptengpass liegt weiterhin im globalen Canvas-Draw.

## Gates

- Fokussierte Projection-/Drag-/Snap-/Mutation-Tests: gruen.
- Vollstaendige BlockEditor-Test-Suite: gruen.
- Hauptprojekt `:app:testDebugUnitTest`: gruen.
- Hauptprojekt `:app:assembleDebug`: gruen.
- Debug-APK auf SM-S918B installiert und gestartet.
- LARGE-Workspace und BlockEditor geladen; Auswahl und wiederholter Drag ohne
  Exception oder ANR ausgefuehrt.
- Root- und BlockEditor-`git diff --check`: gruen.
- FlowEditor unveraendert.

## Verbleibender Hauptengpass

P1 entfernt nur die falsche semantische Abhaengigkeit. Bei jedem
`dragRender`-Update wird weiterhin der gemeinsame Canvas breit invalidiert;
alle 181 Bloecke werden ohne Viewport-Culling verarbeitet, inklusive
graphweiter Abfragen und kurzlebiger Draw-Objekte.

Der kleinste sinnvolle Folgeschritt ist P2: sichtbare Rendermenge und
frame-lokale Grapharbeit begrenzen, ohne Workflow-, Connection- oder
Layoutsemantik zu veraendern.
