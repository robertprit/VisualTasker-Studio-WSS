# Legacy VM Visual Semantics

> **Status:** Legacy Compatibility Reference
>
> **Origin:** `visualtasker-studio/docs/03_RUNTIME_VISUAL_SEMANTICS.md`
>
> **Nicht normativ für neue WSS-Runtime.** Dieses Dokument bewahrt beobachtete Alt-Semantik, damit Kompatibilitätsentscheidungen nachvollziehbar bleiben.

## Historische Operationen

Der alte EMScript-VM-Pfad dokumentierte insbesondere:

| Operation | Historische Ergebnisvariablen | Historische Semantik |
| --- | --- | --- |
| `SCREENSHOT` | `last_screenshot` | nicht werfender Aufruf setzte Erfolg |
| `SCAN` | `last_scan`, `scan_nodes` | `false` war non-fatal |
| `CROP` | `last_crop` | non-null Pfad = Erfolg, null = non-fatal failure |
| `FIND` | `last_find`, `last_find_ok` | Confidence `< 0` bedeutete non-fatal no-match |
| `REGION_SAVE` | `last_region_save` | boolesches Save-Ergebnis |
| `POINT_SAVE` | `last_point_save` | boolesches Save-Ergebnis |

## Wichtige Alt-Regeln

### SCREENSHOT

Ein nicht werfender Controller-Aufruf wurde als Erfolg behandelt, selbst wenn der zurückgegebene Pfad null war. Der Pfad war internes Metadatum.

### SCAN

`scanTree() == false` war ein gültiges non-fatales Resultat. `scan_nodes` fiel bei fehlender Information auf `0` zurück.

### CROP

Null-Ergebnisse waren non-fatal. Exceptions liefen über die normale VM-Fehlerbehandlung.

### FIND

Confidence-Werte wurden nicht normalisiert. Werte `< 0`, insbesondere `-1.0`, signalisierten historisch No-Match/Failure ohne Exception.

### REGION_SAVE / POINT_SAVE

Ein `false`-Save war non-fatal; Exceptions blieben echte Runtime-Fehler.

## Warum diese Datei existiert

Diese Semantik ist teilweise ungewöhnlich. Bei einer neuen Runtime darf sie nicht versehentlich als modernes Design übernommen werden, aber Legacy-Skripte oder Importer können davon abhängig sein.

Vor einer Änderung muss daher entschieden werden:

1. echte WSS-Semantik ändern;
2. Legacy-Normalizer/Compatibility Adapter einsetzen;
3. bestehende Skripte migrieren;
4. Verhalten explizit brechen und versionieren.

## WSS-Richtung

Neue Wahrnehmungsoperationen sollten bevorzugt typisierte Results/Observations liefern statt implizite globale `last_*`-Variablen als Primärvertrag zu benutzen.

`Observation != Entity != Resource`

OCR, Template Matching, A11y und weitere Wahrnehmung gehören über Provider/Processor/Observation in das Worldview-Modell, nicht als versteckte UI- oder VM-Nebenablage.
