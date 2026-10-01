# Block/Node Presentation Contract

## Ziel

BlockEditor, Flowchart und Inspector sind unterschiedliche Projektionen derselben semantischen Property.
Der Contract definiert Klassifikation, IDs und Projektionen, ohne eine zweite Datenwahrheit einzuführen.

## Kette

`Semantic Property -> Presentation Property -> Block Projection -> Node Projection -> Inspector Projection`

- **Semantic Property**: existiert bereits im Workspace-/IR-Modell (z. B. Feld, Value-Input, Statement-Slot, Output).
- **Presentation Property**: reine Darstellungs-/Editor-Bindung (Kategorie, Label, Slot-Kandidat, Icon-Hinweis, Sichtbarkeit).
- **Projection**: Block, Node und Inspector lesen/schreiben dieselbe semantische Property-ID.

## Kategorien

- `INPUT`: semantischer Eingabewert, kann Reporter/Expression/Variable aufnehmen.
- `OUTPUT`: semantischer Ergebniswert (Reporter/Node-Output).
- `STRUCTURE`: Branches, Statement-Bodies, Else/ElseIf, Loop-Body, Stack-/Next-Struktur.
- `CONFIG`: editor-/commandnahe Konfiguration ohne Datenflussbedeutung.
- `PRESENTATION`: Label/Icon/Farbe/Collapse/visuelle Gruppierung/Layout.

## Property-ID Regel

Der Contract verwendet stabile IDs pro semantischer Property:

- Feld: `semantic:<ownerId>:field:<fieldKey>`
- Value-Input: `semantic:<ownerId>:input:<inputName>`
- Statement: `semantic:<ownerId>:structure:<slotName>`
- Output: `semantic:<ownerId>:output`

`ownerId` ist die Blockinstanz-ID (Flowchart-Nodes normalisieren `block:<id>` auf `<id>`).

Damit referenzieren Block-, Node- und Inspector-Projektion dieselbe Property-ID.

## Designer Architektur

```
Designer State
     |
Presentation Contract
     |
+--------------------+
|                    |
v                    v
Block Projection   Node Projection
|                    |
v                    v
Block Preview      Node Preview
|                    |
v                    v
Block Renderer     Flow Renderer
```

Mutator-State sitzt vor der Aufspaltung:

```
Mutator -> Designer State -> { Block Projection, Node Projection }
```

Dadurch aktualisieren beide Previews bei Property-/Mutator-Änderungen sofort aus derselben Quelle.

## Factory UX (linear)

Die Factory-Bedienung ist absichtlich linear (Blockly-Factory-Stil):

- Palette (`INPUTS` / `FIELDS`)
- lineare Elementliste (`elements[]`)
- Element-Properties
- Live Block + Node Preview

`elements[]` ist geordnet, aber nicht verschachtelt.
`EndRow` ist ein Layout-Element ohne Runtime-Semantik.
Reorder/Drag-and-Drop verändert nur Position, nicht die Semantik einer Property.

## Reporter-/Value-Slot Regel

Ein Feld ist Slot-Kandidat, wenn:

1. Kategorie `INPUT` ist, und
2. die erlaubten Quellen Reporter/Variable (inkl. Region-Reporter) enthalten.

Presentation- oder Config-Felder werden nicht automatisch als Reporter-Slots behandelt.

## Wichtige Abgrenzung

**Presentation != Semantic Truth**

- Presentation erweitert nur die UI-Projektion.
- Werte bleiben ausschließlich im bestehenden Workspace-/IR-Modell.
- Keine zweite Parameterwahrheit, keine Persistenzmigration, keine ID-Umstellung.

## Command-Argument-Expressions

Runtime-/Dataflow-Parameter werden als `INPUT` und damit als Value-Input mit
Reporter-Verbindung projiziert. Konfigurations- und Darstellungswerte bleiben
`CONFIG` beziehungsweise `PRESENTATION`; sie werden nicht pauschal in
Expressions umgewandelt.

Für den ersten V1-Konformitätspfad `log(value: Any)` gilt:

- `Command Argument Expression != Serialized String`
- `Any != String`
- `VariableReference != variable.get Command`
- Literal-, Variablen- und Operatoridentität stammen aus Parser/Expression/IR,
  nicht aus Text-Guessing.
- Die Umwandlung eines ausgewerteten Werts in Log-Text ist Runtime-Verhalten
  und findet nicht im Workspace statt.
- Alte gespeicherte `message`-Felder sind nur ein lesender Import-Fallback;
  die kanonische Blockprojektion ist der Value-Input `value`.
