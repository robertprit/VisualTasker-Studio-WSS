# 04 – Runtime, RailTrace und Recording

Ziel: Ausfuehrung, Verlauf und gespeicherte Aufzeichnung als getrennte, aber korrelierte Ebenen testen.

# Runtime / DryRun

## RUN-001 – DryRun Start/Stop
Status: `NOT_TESTED` · P0 · T2

1. gueltigen kleinen Workflow laden.
2. DryRun starten.
3. stoppen.

Erwartet: kontrollierter Start/Stop; kein Side Effect ausserhalb des DryRun-Vertrags; UI bleibt konsistent.

## RUN-002 – Step-Steuerung
Status: `NOT_TESTED` · P0 · T2

1. Step-Modus starten.
2. Schritt fuer Schritt fortschalten.

Erwartet: genau ein fachlicher Step wird jeweils aktiv; Fokus/Index bleiben deterministisch.

## RUN-003 – BasicRun Grundbefehl
Status: `NOT_TESTED` · P0 · T3

1. einen bereits unterstuetzten risikoarmen Grundbefehl ausfuehren.
2. Ergebnis beobachten.

Erwartet: Runtime verarbeitet den Befehl ueber den vorgesehenen Pfad und liefert nachvollziehbaren Outcome.

## RUN-004 – Capability Gate
Status: `NOT_TESTED` · P0 · T2

1. Workflow mit noch nicht angebundenem/fehlendem Adapter ausfuehren.

Erwartet: klarer BLOCKED/unsupported Zustand; kein stiller Erfolg; keine erfundene Provider-Ausfuehrung.

## RUN-005 – Runtime-Fehler
Status: `NOT_TESTED` · P0 · T2

1. reproduzierbar fehlschlagenden Schritt ausfuehren.

Erwartet: Fehler erscheint in Runtime/Rail/Log mit Bezug zum Workflow-Step; Folgeausfuehrung respektiert Fehlerpolicy.

# RailTrace

## RAIL-001 – Program Mode
Status: `NOT_TESTED` · P1 · T1

Erwartet: geplante Workflow-Struktur wird ohne falsche Behauptung einer bereits erfolgten Ausfuehrung gezeigt.

## RAIL-002 – Step Mode
Status: `NOT_TESTED` · P1 · T2

Erwartet: aktueller Step und naechste moegliche Position sind nachvollziehbar.

## RAIL-003 – Live Mode
Status: `NOT_TESTED` · P1 · T2

Erwartet: aktuelle Runtime-Ereignisse erscheinen zeitnah; vergangene Events bleiben historisch stabil.

## RAIL-004 – Replay Mode
Status: `NOT_TESTED` · P1 · T2

1. gespeicherte Recording Session laden.
2. Replay starten.

Erwartet: bestehende Historie wird wiedergegeben, nicht neu ausgefuehrt.

## RAIL-005 – Curate Mode
Status: `NOT_TESTED` · P2 · T1

Erwartet: kuratierende Auswahl veraendert keine historische Rohaufzeichnung stillschweigend.

## RAIL-006 – Surface Records/Run/WatchDog
Status: `NOT_TESTED` · P2 · T1

Erwartet: Surface-Wechsel aendert Darstellung/Arbeitsmodus, nicht die zugrunde liegenden Records.

## RAIL-007 – Session-Auswahl
Status: `NOT_TESTED` · P1 · T1

1. mindestens zwei Sessions speichern.
2. abwechselnd auswaehlen.

Erwartet: RailTrace zeigt eindeutig die aktive Session ohne Vermischung.

## RAIL-008 – Gesamtzeit/Progress
Status: `NOT_TESTED` · P2 · T1

Erwartet: Timespan/Progress entspricht der geladenen Session und bleibt bei Seek konsistent.

## RAIL-009 – Replay Speed
Status: `NOT_TESTED` · P2 · T1

1. mehrere Geschwindigkeiten waehlen.

Erwartet: zeitliche Wiedergabe aendert sich; Event-Reihenfolge bleibt gleich.

## RAIL-010 – Auswahl-Sync
Status: `NOT_TESTED` · P1 · T2

1. Rail-Step auswaehlen.
2. Marker/Editoren pruefen.

Erwartet: referenzierbare Panels folgen demselben fachlichen Ereignis/Step.

## RAIL-011 – Recording Events an Activity/Scene
Status: `NOT_TESTED` · P1 · T2

1. Recording mit Activity-Wechsel und mindestens einem aktiven Event erzeugen.

Erwartet: aktive Events werden plausibel der zeitlich passenden Activity/Scene zugeordnet.

## RAIL-012 – Session Save/Reload
Status: `NOT_TESTED` · P0 · T2

1. Session speichern.
2. App neu starten.
3. Session laden.

Erwartet: Event-Reihenfolge, Zeitbezug und Identitaeten bleiben erhalten.

# Recording

## REC-001 – Start/Stop
Status: `NOT_TESTED` · P1 · T1

Erwartet: klare Session-Grenzen; kein unkontrolliertes Weiteraufzeichnen nach Stop.

## REC-002 – Overlay Start/Stop
Status: `NOT_TESTED` · P1 · T2

Erwartet: Start/Stop ueber Floating Overlay erzeugt dieselbe fachliche Recording Session wie der interne Steuerpfad.

## REC-003 – Rohereignis vs Record
Status: `NOT_TESTED` · P0 · T2

Erwartet: eingehende Events werden nicht allein durch ihr Auftreten automatisch zu Workflow-Wahrheit.

## REC-004 – Append-only Historie
Status: `NOT_TESTED` · P0 · T2

Erwartet: spaetere Interpretation/Kuration ersetzt historische Originaldaten nicht still.
