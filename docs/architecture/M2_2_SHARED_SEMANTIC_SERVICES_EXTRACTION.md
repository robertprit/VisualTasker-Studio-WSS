# M2-2 Shared Semantic Services Extraction

## 1. Ausgangslage

M2-1 neutralisierte `WorkspaceDocument`, Nodes, IDs, Variablen, Felder,
Connections, Reducer und Persistenz in `workflow-core`. Command Catalog,
Workspace-Typsystem, Validierung und IR-Lowering lagen danach weiterhin in
optional benannten BlockEditor-Modulen. M2-2 verschiebt deren kanonische
Implementierungen ohne Schema-, Syntax-, Connection- oder Identity-Redesign in
das reine JVM-Modul `de.visualtasker.workflow:workflow-semantics`.

## 2. Ownership Audit

| Gegenstand | Alter Owner | Klasse | Neuer Owner |
|---|---|---|---|
| Command IDs, Namen, Aliase, Signaturen, Return-Typen | `blockeditor-registry` | CORE_SEMANTIC | `workflow-semantics` |
| Capabilities und Side Effects | `blockeditor-registry` | CORE_SEMANTIC/RUNTIME_SPECIFIC | `workflow-semantics` |
| Block-Label, Slot-Layout, Shape, Toolbox-Farbe | `blockeditor-registry` | EDITOR_PROJECTION | unverändert Registry |
| V1-Typen, nullable, `List<T>`, Domainwerte | `emscript-language-core` | LANGUAGE_SPECIFIC/CORE_SEMANTIC | unverändert Language Core |
| Workspace-Typauflösung und LET/SET-Kompatibilität | `blockeditor-registry` | CORE_SEMANTIC | `workflow-semantics` |
| Structural-, Expression-, Branch- und Variable-Validation | `blockeditor-validation` | CORE_SEMANTIC | `workflow-semantics.validation` |
| Validation-Diagnostics | `blockeditor-validation` | CORE_SEMANTIC | `workflow-semantics.validation` |
| IR-Modelle, Integrität und Lowering | `blockeditor-ir` | CORE_SEMANTIC | `workflow-semantics.ir` |
| Viewport, Selection, Drag, Snap-Visuals | BlockEditor | EDITOR_PROJECTION | unverändert BlockEditor |
| Parser/Importer | App und Language Core | LANGUAGE_SPECIFIC | unverändert, konsumiert neutral |
| Ausführungsadapter | App Runtime | RUNTIME_SPECIFIC | unverändert, konsumiert neutral |

Alle extrahierten Implementierungen sind frei von Android, Compose,
Editor-State, Pixelgeometrie und View-Objekten.

## 3. Command Catalog Ownership

`VisualTaskerCommandCatalog` ist nun die einzige normative Quelle für Command
ID, akzeptierte Namen, Argumente, Typen, Rückgabetypen, Seiteneffekte und
Capabilities. Die Registry leitet daraus Blockdarstellung ab.
`CommandCatalogPresentation.kt` ergänzt nur Anzeige-Metadaten.

Der historische Name `CommandBlockBinding` bezeichnet die Bindung an einen
kanonischen `WorkspaceDocument`-Elementtyp und bleibt aus API-Kompatibilität.
Der historische Flow-Node-Hinweis ist noch Projection-Metadaten-Leak, wird aber
nicht für Typechecking, Validation oder IR verwendet.

## 4. Type System Ownership

Die Typmodelle verbleiben im neutralen `emscript-language-core`.
`workflow-semantics` konsumiert `LanguageTypeRef`, `CoreTypes` und
`LanguageTypeCompatibility`; es führt kein zweites String/Number/Bool/Any/Void,
nullable-, Listen- oder Domain-Type-System ein.

## 5. Type Compatibility

`WorkspaceValueTypeSystem` liegt neutral und entscheidet Expression-, Slot-,
Variablen- und LET/SET-Kompatibilität. Der BlockEditor projiziert nur Shape,
Snap-Feedback und Invalid-Markierung. Erhalten bleiben unter anderem Number nach
Bool ungültig, String nach Number ungültig, nullable nach non-null ungültig und
strict Bool Conditions.

## 6. Validation Ownership

`Validator` akzeptiert einen neutralen `WorkflowSemanticDefinitionProvider`.
Required Inputs, Connections, Typen, Branch-Slots, Variablen, Orphans und Zyklen
werden ohne Registry oder Editor-State geprüft. Overlap, Hit Test, Viewport und
Snap Candidate bleiben Editorlogik.

## 7. Diagnostics

Semantische Diagnostics sind reine Kotlin-Daten mit stabilem `code`,
semantischer Block-/Connection-Referenz und Meldungsdaten. Android-, Compose-,
Pixel- oder View-Typen sind nicht Teil des neutralen Vertrags.

## 8. IR Ownership

IR-Modelle, `IrGenerator`, `IrGraphGenerator`, Integritätsprüfung und
REM-Metadaten liegen in `workflow-semantics.ir`. Eingang ist
`WorkspaceDocument` plus neutrale Definitionen/Catalog. Das Ergebnis enthält
keine Rects, Offsets, Viewports, Selections, Animationen oder Toolbox-Daten.

## 9. Dependency Direction

```text
emscript-language-core
          |
     workflow-core
          |
 workflow-semantics
    /      |       \
runtime  importer  projections
                   /  |  \
                 Text Block Flow
```

- `workflow-core -> BlockEditor`: NEIN
- `workflow-semantics -> BlockEditor`: NEIN
- IR -> BlockEditor Presentation/Domain: NEIN
- Runtime-Semantik -> BlockEditor: NEIN
- Persistenz -> BlockEditor: NEIN
- Importer-Semantik -> BlockEditor: NEIN

Die App besitzt weiterhin die erwartete BlockEditor-UI-Abhängigkeit; Runtime
und Importer benötigen sie nicht mehr zur semantischen Auswertung.

## 10. Compatibility Strategy

`blockeditor-validation` und `blockeditor-ir` bleiben dünne Gradle/API-Fassaden,
die `workflow-semantics` exportieren. Dort existiert kein zweiter
Produktionsvalidator und kein zweiter IR-Generator. Bestehende Tests laufen in
diesen Modulen weiter, verwenden aber neutrale Packages. `BlockTypes` und
`BlockCategories` sind Präsentationsfassaden über `WorkflowElementTypes` und
`CommandCategories`.

## 11. Semantic Equivalence

Die vollständigen Tests decken lineare Sequenzen, LET/SET,
Number/String/Bool/Any, nullable, `List<T>`, Operator/Compare, Command Reporter,
IF/ELSEIF/ELSE, WHILE/REPEAT, Provider Queries sowie Perception- und
Chart-Domainwerte ab. Der neutrale Fixture beweist zusätzlich Load/Save,
Typecheck, Validation und IR ohne Editor-Präsentation. Persistenz bleibt Schema
1; Connections, IDs, Root-Reihenfolge und Felder wurden nicht verändert.

## 12. Runtime Independence

Für die von M2-2 betrachteten semantischen Services: **YES**. Runtime-Code
importiert Catalog, Capabilities, Typen und IR aus neutralen Packages.
Android-Ausführungsadapter bleiben naturgemäß im Runtime-Layer der App.

## 13. BlockEditor Removability

`SharedSemanticServicesTest` liegt in `workflow-semantics` und hat keine
Registry-, Compose- oder BlockEditor-Abhängigkeit. Er erstellt ein Dokument,
serialisiert/deserialisiert es, validiert es, prüft Typen und erzeugt IR.

## 14. Updated M2 Metrics

| Kategorie | nach M2-1 | nach M2-2 | Begründung |
|---|---:|---:|---|
| `DUPLICATED_TRUTH` | 9 | 9 | Keine der neun in M2-0 gezählten Identity-/Relationsduplikationen wurde in diesem Slice redesigniert. |
| `MISSING_CONTRACT` | 13 | 12 | Der fehlende neutrale Validation-Owner existiert jetzt. Die übrigen M2-0-Verträge bleiben offen. |

IR- und Catalog-Ownership waren Modul-Leaks, aber keine zusätzlichen
M2-0-Zählpositionen; sie werden nicht als künstliche Metrikreduktion verbucht.

## 15. Remaining M2 Gaps

Das größte Leak ist das Relation-/Identity-Modell: Sequence-, Value- und
Statement-Verbindungen besitzen paarige Endpunkte; ELSEIF/Branches und
Expressions haben keine eigenen neutralen IDs. Daneben verbleibt der historische
Flow-Projection-Hinweis im Catalog. Nicht Teil von M2-2 waren Connection-Kinds,
neue IDs, Component Model, WorkspaceOperation oder UI-Arbeit.

## 16. Recommended Next Slice

**M2-3 Neutral Relation and Identity Audit/Contract**: Sequence-, Value-,
Statement- und Branch-Relationen zuerst exakt spezifizieren und ihre kompatible
Migration planen. Component Model und VT2VT-Operationsprotokoll bleiben daraus
ausgeschlossen.
