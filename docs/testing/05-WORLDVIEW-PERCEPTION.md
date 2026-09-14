# 05 – Worldview, Ressourcen und Perception

Ziel: die zentrale Trennung `Observed != Interpreted != Intended` pruefen. Providerdaten duerfen keine Worldview-Wahrheit still erzeugen oder ueberschreiben.

## WV-001 – Observation != Entity
Status: `NOT_TESTED` · P0 · T2

1. eine einzelne Provider-Beobachtung erzeugen, z. B. A11y/OCR/Marker.
2. Worldview/Inspector betrachten.

Erwartet: Observation bleibt als Quelle referenzierbar; eine aufgeloeste Entity ist ein eigener fachlicher Zustand.

## WV-002 – Resource != Observation
Status: `NOT_TESTED` · P0 · T2

1. Screenshot/Template/Marker als Resource speichern.
2. pruefen, ob allein dadurch eine Observation/Entity entsteht.

Erwartet: persistentes Artefakt wird nicht automatisch als aktuelle Beobachtung oder Wahrheit behandelt.

## WV-003 – WorldEntity/Scene Projektion
Status: `NOT_TESTED` · P1 · T2

1. vorhandene Worldview-Daten oeffnen.
2. Scene und zugehoerige Entities pruefen.

Erwartet: Scene ist zeitlich/semantisch zusammenhaengender Kontext; Entities sind referenzierbar und nicht bloss UI-Kopien von Providerdaten.

## WV-004 – Ambiguity sichtbar
Status: `NOT_TESTED` · P1 · T2

1. zwei plausible Kandidaten fuer dieselbe Interpretation erzeugen, soweit aktueller Build dies erlaubt.

Erwartet: Unsicherheit/Konflikt wird explizit dargestellt; kein stilles Auto-Merge ohne nachvollziehbare Policy.

## WV-005 – Marker Resource
Status: `NOT_TESTED` · P1 · T1

1. Point/Region oder aktuell unterstuetzten Marker erfassen.
2. speichern.
3. wieder laden.

Erwartet: stabile Identitaet, Koordinaten/Normalisierung nachvollziehbar, kein Verlust durch Panelwechsel.

## WV-006 – Screenshot Resource
Status: `NOT_TESTED` · P1 · T1

1. Screenshot ueber vorhandenen Pfad erzeugen/importieren.
2. im Datastore/Canvas pruefen.

Erwartet: Resource besitzt nachvollziehbare Herkunft/Referenz und bleibt persistierbar.

## WV-007 – Template Resource
Status: `NOT_TESTED` · P2 · T1

1. Template/Crop erzeugen oder importieren, soweit implementiert.

Erwartet: Template bleibt Resource; Verwendung in Vision erzeugt getrennte Observation/Match-Ergebnisse.

## WV-008 – Canvas/Datastore Projektion
Status: `NOT_TESTED` · P1 · T2

1. bekannte Ressourcen im Datastore pruefen.
2. korrespondierenden Canvas-/Worldview-Slice oeffnen.

Erwartet: beide Panels projizieren dieselben fachlichen Ressourcen statt Kopien mit eigener Wahrheit.

## WV-009 – Vision Input/Live-Crop
Status: `NOT_TESTED` · P2 · T1

1. Vision Panel mit Screenshot/Live-Crop verwenden, soweit implementiert.

Erwartet: Eingangsdaten und abgeleitete Ergebnisse sind unterscheidbar; Filter/Threshold veraendern nicht das Originalartefakt.

## WV-010 – Provider-Provenance
Status: `NOT_TESTED` · P0 · T2

1. Observation aus mindestens zwei Quellen erzeugen, soweit verfuegbar.

Erwartet: Quelle/Provider, Zeitbezug und Referenz bleiben erkennbar; Fusion loescht Herkunft nicht.

## WV-011 – Keine stille Provider-Mutation
Status: `NOT_TESTED` · P0 · T2

1. vorhandene Entity mit neuer widersprechender Observation konfrontieren.

Erwartet: neue Evidenz fuehrt zu Resolution/Ambiguity/Update ueber Domain-Pfad; Provider schreibt Entity nicht direkt um.

## WV-012 – Unknown bleibt Unknown
Status: `NOT_TESTED` · P0 · T2

1. Fall ohne ausreichende Evidenz erzeugen.

Erwartet: System darf `UNKNOWN`, fehlende Evidenz oder Ambiguity darstellen und muss nicht zwanghaft eine Entity/Scene behaupten.

## Aktuell geplante/teilweise Bereiche

Marker, Vision, Canvas und Datastore sind laut Roadmap noch nicht releasefinal. Entsprechende Tests duerfen daher `NOT_IMPLEMENTED` oder `BLOCKED` ergeben. Das ist ein erwartbarer Entwicklungsstatus, solange kein bereits als stabil deklarierter Contract verletzt wird.
