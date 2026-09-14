# 00 – Core Smoke Test

Dieser Testlauf ist der Einstieg vor jeder groesseren manuellen Session. Er soll in kurzer Zeit zeigen, ob der aktuelle Build fuer weitere Tests geeignet ist.

## SMK-001 – Build + Unit Tests

Status: `NOT_TESTED`  
Prioritaet: P0  
Ebene: T0

**Schritte**
1. `./gradlew test`
2. `./gradlew assembleDebug`

**Erwartet**
- keine fehlgeschlagenen Unit Tests
- Debug-APK wird erzeugt
- keine neue Compile-/Link-Regression

## SMK-002 – Install + Launch

Status: `NOT_TESTED`  
Prioritaet: P0  
Ebene: T0

**Schritte**
1. Debug-Build auf Testgeraet installieren.
2. App starten.
3. 30 Sekunden normal bedienen/Panel wechseln.

**Erwartet**
- App startet ohne Crash
- Workspace erscheint
- keine Crash-/ANR-Schleife

## SMK-003 – Workspace Basisinteraktion

Status: `NOT_TESTED`  
Prioritaet: P0  
Ebene: T1

**Schritte**
1. Ein Panel oeffnen.
2. Panel verschieben.
3. Groesse aendern.
4. minimieren und wiederherstellen.
5. schliessen und erneut oeffnen.

**Erwartet**
- jede Interaktion bleibt responsiv
- kein Panel verschwindet unkontrolliert ausserhalb des Workspace
- Fokus/Z-Order reagiert nachvollziehbar

## SMK-004 – Workspace Restart/Restore

Status: `NOT_TESTED`  
Prioritaet: P0  
Ebene: T2

**Schritte**
1. Zwei Panels positionieren und eines minimieren.
2. App sauber verlassen.
3. App neu starten.

**Erwartet**
- gespeicherter Workspace-/Panel-Zustand wird plausibel wiederhergestellt
- kein Restore-Crash

## SMK-005 – Minimaler Block-Workflow

Status: `NOT_TESTED`  
Prioritaet: P0  
Ebene: T1

**Schritte**
1. BlockEditor oeffnen.
2. minimalen gueltigen Workflow aus Start + einfacher Aktion/Grundbefehl erzeugen.
3. Block verbinden.

**Erwartet**
- Snap/Docking funktioniert
- Validator akzeptiert den minimalen Workflow
- kein Phantom-/Doppelblock

## SMK-006 – Block -> EMScript

Status: `NOT_TESTED`  
Prioritaet: P0  
Ebene: T2

**Schritte**
1. Workflow aus SMK-005 beibehalten.
2. TextEditor oeffnen.

**Erwartet**
- Textprojektion repraesentiert denselben Workflow
- keine semantisch zusaetzliche Aktion
- keine fehlende Kernaktion

## SMK-007 – EMScript Apply -> Workflow

Status: `NOT_TESTED`  
Prioritaet: P0  
Ebene: T2

**Schritte**
1. EMScript-Draft um eine einfache unterstuetzte Aenderung erweitern.
2. Apply Preview pruefen.
3. Apply bestaetigen.
4. BlockEditor ansehen.

**Erwartet**
- Preview beschreibt eine plausible semantische Aenderung
- Apply erfolgt atomar
- BlockEditor zeigt die neue kanonische Struktur
- keine zweite konkurrierende Text-/Block-Wahrheit

## SMK-008 – Workflow -> Flow Projektion

Status: `NOT_TESTED`  
Prioritaet: P0  
Ebene: T2

**Schritte**
1. FlowEditor oeffnen.
2. denselben Workflow betrachten.

**Erwartet**
- Nodes entsprechen dem kanonischen Workflow
- Reihenfolge/Branching stimmt semantisch mit Text und Blocks ueberein

## SMK-009 – DryRun Step-Fokus

Status: `NOT_TESTED`  
Prioritaet: P0  
Ebene: T2

**Schritte**
1. RailTrace oeffnen.
2. DryRun starten.
3. mindestens einen Step ausfuehren/fortschalten.

**Erwartet**
- Runtime/DryRun erzeugt einen eindeutigen aktuellen Step
- RailTrace aktualisiert Auswahl/Position nachvollziehbar
- kein echter Side Effect, sofern DryRun dies fuer den Befehl verspricht

## SMK-010 – Gemeinsamer Fokus Text/Block/Flow/Rail

Status: `NOT_TESTED`  
Prioritaet: P0  
Ebene: T3

**Schritte**
1. waehrend DryRun einen konkreten Workflow-Step fokussieren.
2. TextEditor, BlockEditor, FlowEditor und RailTrace nacheinander betrachten.

**Erwartet**
- alle vier Projektionen beziehen sich auf denselben fachlichen Schritt
- Textzeile, Block, Node und Rail-Item widersprechen einander nicht
- Wechsel zwischen Panels veraendert nicht den kanonischen Workflow

## Smoke-Abnahme

```text
Datum:
Commit:
Build:
Geraet:

SMK-001:
SMK-002:
SMK-003:
SMK-004:
SMK-005:
SMK-006:
SMK-007:
SMK-008:
SMK-009:
SMK-010:

P0 Regression vorhanden: ja/nein
Weiter mit Detailtests: ja/nein
```
