# 01 – Workspace Shell

Ziel: Bedienbarkeit, Panel-Lifecycle und Session-Zustand der WSS pruefen. Workspace-UI darf dabei keine fachliche Workflow-Wahrheit erzeugen.

## WS-001 – Panel oeffnen/schliessen
Status: `NOT_TESTED` · P1 · T1

1. Mehrere unterschiedliche Panels oeffnen.
2. Panels einzeln schliessen.
3. erneut oeffnen.

Erwartet: eindeutiger Lifecycle, keine Duplikate ohne Absicht, keine verwaisten UI-Zustaende.

## WS-002 – Panel verschieben
Status: `NOT_TESTED` · P1 · T1

1. Panel langsam und schnell ziehen.
2. zwischen anderen Panels bewegen.

Erwartet: flüssige Bewegung, Pointer bleibt logisch am Panel, keine Spruenge.

## WS-003 – Panel resize
Status: `NOT_TESTED` · P1 · T1

1. Breite und Hoehe vergroessern/verkleinern.
2. Minimal-/Maximalbereiche testen.

Erwartet: Inhalt bleibt bedienbar; Resize-Griff reagiert verlaesslich; keine negative/ungueltige Groesse.

## WS-004 – Minimieren/Wiederherstellen
Status: `NOT_TESTED` · P1 · T1

1. Panel minimieren.
2. aus Minimized Dock wiederherstellen.
3. mehrfach wiederholen.

Erwartet: Position/Typ/Inhalt bleiben konsistent.

## WS-005 – Fokus/Z-Order
Status: `NOT_TESTED` · P1 · T1

1. Panels ueberlappen lassen.
2. abwechselnd anklicken.

Erwartet: aktives Panel kommt erwartbar nach vorne; Eingaben gehen nur an das fokussierte Ziel.

## WS-006 – Grid/Snap
Status: `NOT_TESTED` · P2 · T1

1. Grid/Snap aktivieren.
2. Panel frei bewegen und an Raster/Anker fuehren.
3. deaktivieren und erneut pruefen.

Erwartet: Snap ist reproduzierbar und abschaltbar; keine unerwartete Drift.

## WS-007 – Docking/Nachbarpanel
Status: `NOT_TESTED` · P2 · T1

1. Panel an Workspace-Kante bewegen.
2. Panel an Nachbarpanel bewegen.
3. wieder loesen.

Erwartet: magnetisches Verhalten ist sichtbar und reversibel; kein unbeabsichtigtes dauerhaftes Gruppieren.

## WS-008 – Workspace-Grenzen
Status: `NOT_TESTED` · P1 · T1

1. Panel weit nach rechts/unten ziehen.
2. Resize an Grenzen versuchen.

Erwartet: harte Grenzen und Dock-Reserve verhindern unbedienbar verlorene Panels.

## WS-009 – Session Restore
Status: `NOT_TESTED` · P0 · T2

1. mehrere Panels unterschiedlich positionieren/minimieren.
2. App beenden.
3. neu starten.

Erwartet: gespeicherte Paneltypen und wesentliche View-Zustaende werden ohne Crash rekonstruiert.

## WS-010 – Theme-Wechsel
Status: `NOT_TESTED` · P2 · T1

1. System/Hell/Dunkel nacheinander aktivieren.
2. mehrere Panels pruefen.

Erwartet: alle Panels bleiben lesbar; kein Zustand geht verloren; keine Farbe ist alleiniger semantischer Traeger.

## WS-011 – Siderail/Panel-Navigation
Status: `NOT_TESTED` · P1 · T1

1. wichtige Panels ueber Navigation oeffnen.
2. zwischen Editor-/Trace-/World-Panels wechseln.

Erwartet: eindeutige Ziele, kein unerwartetes Reset bestehender Panelzustaende.

## WS-012 – Mehrfachinstanzen
Status: `NOT_TESTED` · P2 · T1

1. Panels testen, die nur einmal existieren sollen.
2. Panels testen, bei denen mehrere Instanzen erlaubt sind, falls vorhanden.

Erwartet: Instanzregeln sind konsistent und Restore respektiert sie.

## Regression-Querpruefung

Nach WS-001 bis WS-012:

- [ ] App bleibt nach Rotation/Background/Foreground stabil, soweit unterstuetzt.
- [ ] Panel-Lifecycle veraendert keinen Workflow ohne Domain-Command.
- [ ] Restore erzeugt keine unbekannten/Legacy-Paneltypen ohne kontrolliertes Fallback.
