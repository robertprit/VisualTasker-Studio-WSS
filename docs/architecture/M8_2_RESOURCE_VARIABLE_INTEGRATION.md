# M8-2 Resource and Variable Integration

Stand: 2026-10-12

## Ziel und Ownership

M8-2 verbindet die bereits vorhandenen WSS-Ressourcen und Variablendeklarationen mit dem in M8-0
eingefuehrten `ProjectDocument`. Es entsteht keine zweite Registry:

```text
legacy WorkspaceResourceBundle / Canvas marker / Screenshot / EMA catalog
  -> one-time ProjectInventoryIntegrator
  -> ProjectResource + ProjectVariable + ProjectReference
  -> ProjectOperationExecutor
  -> ProjectDocument
  -> ProjectResourceBridge
  -> WorkspaceResourceBundle read projection
```

Nach der einmaligen Bestandsuebernahme ist `ProjectDocument` die persistierte Projektwahrheit.
`WorkspaceResourceBundle` bleibt das bestehende, aus dem Projekt abgeleitete Read Model fuer Canvas,
Worldview und Datastore. Die alte Datei `workspace-resources.json` wird nur noch als Legacy-Quelle
gelesen und nicht mehr bei jeder Datastore-Projektion ueberschrieben.

## Ressourcen

Produktiv angebunden sind die vorhandenen Typen:

- Images und Screenshots als `Image` beziehungsweise `Capture`,
- Templates,
- Points und Regions,
- Swipe-/Spline-/Path-Marker als `Path`,
- Draw-/Node- und `.ema`-Assets als `Shape`,
- bestehende Dataset-/Overlay-Ressourcen als `Observation` oder `Provider`.

Die vorhandene `WorkspaceResource` wird verlustfrei im Metadatenfeld des `ProjectResource`
transportiert. Binaere Bilddaten werden nicht in das Projektdokument kopiert. Reale Datei- und
Content-URIs bleiben Locations; rein logische oder eingebettete Marker erhalten eine explizite
`inline:workspace-resource:<id>`-Location. Stabile Resource-IDs werden nicht neu erzeugt.

Die produktiven Upsert-, Import- und Remove-Pfade der Workspace Shell verwenden `PutResource`,
atomare `ProjectTransaction`s und `RemoveResource`. Ein Screenshot-Capture und ein gespeicherter
Canvas-Marker werden unmittelbar dem Projektbestand hinzugefuegt. Remove entfernt nur die
Projektmitgliedschaft; physische Dateien werden in diesem Slice nicht kaskadierend geloescht.

## Variablen und Scopes

Variablendeklaration, Referenz, Laufzeitwert und Persistenz bleiben getrennte Begriffe:

| Scope | Deklarations-Owner | Lifetime | Persistenz | Runtime |
| --- | --- | --- | --- | --- |
| Local | Workflow root | Invocation | keine | implementiert |
| Script | Workflow root | WorkflowRun | Session | implementiert |
| Global / Project | ProjectDocument | Project | ProjectDocument | GAP |
| AppShared | ProjectDocument | Application | AppStore | GAP |
| External | ProjectDocument + Binding | Provider | Provider | GAP |

Beim Reconcile werden `Global`, `Project` und `AppShared` aus den Roots in den Projektbestand
uebernommen. `Local` und `Script` bleiben root-lokal. Eine externe Deklaration ohne bestehendes
Provider-Binding wird nicht als funktionsfaehig importiert, sondern als Diagnose ausgewiesen.
Der vorhandene Runtime-Datastore ist ein Laufzeitwertspeicher und wird nicht nachtraeglich als
AppShared- oder External-Provider umetikettiert.

## Referenzen und Integritaet

M8-2 erzeugt ProjectReferences nur aus eindeutigen vorhandenen Fakten:

- kanonische Variable-Reference-Relationen,
- explizite Resource-ID-Felder wie `resourceId`, `templateId`, `markerId`, `imageId`, `regionId`,
  `pointId`, `pathId` und `shapeId`.

Labels, Pixelpositionen und Renderreihenfolgen werden niemals zur Identitaetsableitung verwendet.
Fehlende explizite Resource-IDs erzeugen eine Diagnose und keine kaputte ProjectReference.
Where-used und Delete-Guards verwenden dieselben `ProjectReference`s. Eine Umbenennung erfolgt als
`PutResource` mit unveraenderter ID; bestehende Referenzen bleiben dadurch stabil. Referenzierte
Ressourcen und Variablen koennen nicht geloescht werden.

Editor-, Recorder- und Explorer-Mutationen werden nach Annahme gegen denselben Inventory-Vertrag
reconciled. Der sichtbare Projektwechsel bleibt ein History-Schritt; Undo/Redo restauriert den
vollstaendigen `ProjectDocument` inklusive Ressourcen, Variablen und Referenzen. Andere Roots werden
nicht veraendert.

## Migration

Die Bestandsuebernahme wird mit `visualtasker.inventory.version=1` im `ProjectDocument` markiert.
Sie ist idempotent und importiert die Legacy-Datei nicht bei jedem Start erneut. Bestehende
Single-Root-Projekte durchlaufen weiterhin zuerst die M8-0-Migration und danach denselben
Inventory-Schritt. Save/Reload laeuft ausschliesslich ueber `ProjectSerializer` und erhaelt IDs,
Payload, unbekannte Metadaten und Scope-Bindings.

## Minimale UI-Hygiene

Die nur fuer M5-Geraetetests sichtbare Gesture-Testflaeche liegt nicht mehr ueber produktiven
Panels. Panels behalten ihre positive Z-Order und ihre Touch-Eingaben. Es wurden keine Panels,
Tabs oder Toolbars neu gestaltet.

## Offene GAPs

- `Project`, `Global`, `AppShared` und `External` haben noch keinen produktiven Runtime-/Storage-
  Provider; der Explorer zeigt dies explizit.
- External-Variablen werden nur mit einem bereits vorhandenen Binding projektweit akzeptiert.
- Vision- und Recorder-Observations bleiben abgeleitete Worldview-Daten und werden nicht pauschal
  als langlebige Projektressourcen dupliziert.
- Physischer Asset-Lifecycle, Paketexport, Relocation, Hashpruefung und kaskadenfreies Datei-Cleanup
  bleiben M8-6/M8-9.
- Neue Dateien ausserhalb der produktiven Capture-/Save-/Importpfade werden nicht fortlaufend
  gescannt; der einmalige Legacy-Import ist absichtlich kein versteckter File-Watcher.

## Spaeterer UI-/UX-Konsolidierungsbacklog

Diese Gruppen werden erst in einem freigegebenen M6-Slice bewertet:

- DataStore / Project Explorer / Visual Asset Manager,
- Scene Inspector / RailTrace,
- Vision / Canvas / Marker,
- Debug / Log Console.

M8-2 nimmt keine Panel-Zusammenlegung und keine kosmetische Neugestaltung vor.
