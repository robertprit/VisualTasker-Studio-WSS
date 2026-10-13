# C7-0 Repository- und Publikationsaudit

Stand: 2026-10-11

## 1. Ziel und Grenze

Dieses Dokument erfasst den lokalen und den auf GitHub sichtbaren Stand von VisualTasker Studio
WSS vor einer spaeteren Repository-Bereinigung oder Veroeffentlichung. Es nimmt keine
Remote-Umbenennung, keinen Push, keinen Checkout, keine Bereinigung und keine Aenderung fremder
Arbeitsstaende vor.

Geprueft wurden:

- Git-Remotes, Branches, Tags, Ahead/Behind und Dirty State,
- Submodule und eingebettete eigenstaendige Git-Repositories,
- README, Lizenz, Third-Party-Hinweise und GitHub-Konfiguration,
- Wiki-Quellen und Dokumentationsstruktur,
- die Feature-Matrix und die daraus ableitbare oeffentliche Produktdarstellung.

Nicht Teil dieses Audits sind ein vollstaendiger History-Secretscan, eine Rechtsberatung, eine
erneute Build-/Geraeteabnahme und eine semantische Einzelpruefung aller Dokumentationsaussagen.

## 2. Audit-Baseline

| Befund | Stand |
| --- | --- |
| Lokaler Branch | `main` |
| Lokaler HEAD | `711e13f` (`docs: consolidate release gates and scope`) |
| GitHub-Remote | `origin` -> `robertprit/VisualTasker-Studio-WSS` |
| GitHub `main` | `f5e6ce3` |
| Divergenz | lokal 16 Commits voraus, 0 Commits zurueck |
| Remote-Tags | keine |
| Staging Area vor C7-0 | sauber |
| Working Tree vor C7-0 | nicht sauber; fremdes WIP vorhanden |
| Repository-Sichtbarkeit | `public` |
| Default Branch | `main` |

Der Remote-HEAD und die Remote-Branches wurden read-only gegen GitHub abgeglichen; es wurde kein
`fetch` ausgefuehrt. Das lokale `origin/main` entsprach dem dabei sichtbaren Remote-Stand.

## 3. Branches und unveroeffentlichter Stand

### Lokal

- `main` bei `711e13f`, Tracking `origin/main`, 16 Commits voraus.
- `audit/intelligence-learning-consolidation` bei `7ce7af7`, ohne Upstream.

### GitHub

- `main` bei `f5e6ce3`.
- `docs/legacy-reference-migration` bei `400f8a3`.

Die 16 lokalen Commits enthalten M4-0 bis M4-3, M5-0 bis M5-7, die Feature-Inventur und die
Release-1.0-Konsolidierung. Ein Push des Superprojekts ist noch nicht reproduzierbar vorbereitet,
weil mindestens ein referenzierter Submodule-Commit auf GitHub fehlt; siehe Abschnitt 5.

## 4. Fremdes WIP und Dirty-Worktree-Grenze

Vor C7-0 waren folgende Bereiche bereits veraendert oder untracked:

- das eingebettete IME-Keypad-Projekt,
- das M3ShapeMaker-Submodule inklusive Design-Editor,
- laufende Runtime-/Provider-Arbeit im WSS-Host,
- zwei temporaere Geraete-/UI-Screenshots im Repository-Root.

Dieses WIP ist nicht Bestandteil von C7-0. Insbesondere duerfen Bereinigung, Staging, Commit,
Submodule-Update oder Publikationsvorbereitung diese Aenderungen nicht implizit aufnehmen.

Die temporaeren Root-Screenshots sind nicht durch `.gitignore` abgedeckt. Ein spaeteres breites
`git add` koennte sie daher versehentlich aufnehmen.

## 5. Submodule und eingebettete Repositories

### Deklarierte Submodule

| Pfad | Gitlink | Konfigurierter Branch | Remote-Nachweis | Lokaler Zustand | Bewertung |
| --- | --- | --- | --- | --- | --- |
| `visualtasker-blockeditor` | `9a99f9b` | `codex/blockeditor-compact-badge-bounds` | Commit von GitHub nicht aufloesbar; Remote-Branch steht bei `49f0414` | lokaler Branch 20 Commits voraus | **Publikationsblocker** |
| `visualtasker-flowchart` | `008b136` | `codex/flowchart-visual-semantics` | Gitlink ist auf dem konfigurierten Remote-Branch vorhanden | sauber, 0/0 | reproduzierbar |
| `VisualTasker M3ShapeMaker Plugin/m3shapemake` | `e17072c` | `main` | Gitlink entspricht Remote-`main` | Arbeitsbaum stark veraendert | Gitlink reproduzierbar, WIP schuetzen |

Vor einem Push des Superprojekts muss der BlockEditor-Commit zuerst in das richtige Repository und
auf einen freigegebenen Branch gepusht werden. Danach ist der Gitlink erneut per GitHub/API oder
frischem Test-Clone zu verifizieren. Die Reihenfolge ist zwingend: Abhaengigkeit zuerst,
Superprojekt danach.

### Eingebettete eigenstaendige Git-Repositories

`VisualTasker ChartGraph Plugin` und `VisualTasker IME Keypad` besitzen jeweils ein eigenes
`.git`, werden im Superprojekt aber als normale Dateien und nicht als Gitlinks gefuehrt. Das erzeugt
zwei parallele Versionswahrheiten:

- ChartGraph: 38 Dateien im Superprojektindex, eigenes Repository derzeit sauber.
- IME Keypad: 45 Dateien im Superprojektindex, eigenes Repository mit lokalem WIP.

Vor einer strukturellen Bereinigung ist zu entscheiden, ob diese Projekte bewusst vendorte
Snapshots, echte Submodule oder vollstaendig getrennte Repositories sein sollen. Eine Umwandlung ist
nicht Teil von C7-0 und waere ohne Migrationsplan destruktiv.

## 6. GitHub-Konfiguration

| Einstellung | Aktueller Stand | Publikationsbewertung |
| --- | --- | --- |
| Name | `VisualTasker-Studio-WSS` | Produktname/Slug noch freigeben |
| Beschreibung | leer | ergaenzen |
| Homepage | leer | optional festlegen |
| Topics | keine | ergaenzen |
| Issues / Projects | aktiv | nutzbar |
| Wiki | aktiviert, aber kein Wiki-Repository aufloesbar | noch nicht initialisiert/publiziert |
| Pages / Discussions | deaktiviert | bewusste Entscheidung noetig |
| Branch Protection | keine | vor Team-/Releasebetrieb einrichten |
| Rulesets | keine | vor Team-/Releasebetrieb einrichten |
| Actions | aktiv, alle Actions erlaubt | Workflow- und Pinning-Policy fehlt |
| Merge-Methoden | Merge, Squash und Rebase aktiv | auf eine Policy reduzieren |
| Auto-Merge | aus | optional |
| Branch nach Merge loeschen | aus | optional aktivieren |
| Web Commit Signoff | aus | Policy-Entscheidung |
| Community Profile | 28 % | Support-/Governance-Dateien fehlen |

Im Root fehlt `.github/` vollstaendig. GitHub erkennt nur README und Apache-2.0-Lizenz. Es fehlen
unter anderem Issue-/PR-Templates, `SECURITY.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md` und ein
CI-Workflow. Diese Dateien sollen erst nach Freigabe der Governance- und Supportgrenzen erstellt
werden; sie duerfen keine Reaktionszeiten oder Supportzusagen erfinden.

Das Repository ist bereits oeffentlich. Die Bereinigung ist deshalb keine Vorab-Privatisierung:
Alles auf dem derzeitigen Remote-`main` ist bereits extern sichtbar.

## 7. Name und Identitaet

Der aktuelle Bestand verwendet mehrere technische Schreibweisen:

- Produktanzeige: `VisualTasker Studio WSS`,
- Gradle-Root: `VisualTaskerStudio-WSS`,
- GitHub-Slug: `VisualTasker-Studio-WSS`,
- Paket/Namespace: `com.visualtasker.wss`.

Fuer die oeffentliche Darstellung bietet sich `VisualTasker Studio` als Produktname an. `WSS` kann
als Architektur-/Entwicklungskuertzel erhalten bleiben. Eine andere Marke waere eine
produktweite Migration und darf nicht nur durch Umbenennen des GitHub-Repositories entstehen.
Paketname, Deep Links, Tasker-Integration, Dokumentlinks und AppInfo muessen bei jeder
Namensentscheidung separat betrachtet werden.

## 8. README-Befund

Die bestehende `README.md` wurde zuletzt am 2026-09-14 geaendert und ist gegenueber dem aktuellen
Stand sichtbar gedriftet:

- Sie nennt `compileSdk` und `targetSdk` 35; der Build verwendet inzwischen 36.
- Sie beschreibt einen allgemeinen aktiven Prototyp, aber weder die 146-Feature-Matrix noch die
  aktuellen Release-Grenzen.
- Sie trennt nachgewiesene Kernpfade nicht von PARTIAL-, PLANNED- und DEFERRED-Funktionen.
- Sie nennt weder die sieben aktuellen Release-Blocker noch den fehlenden Stable-/Tag-Stand.
- Sie erklaert Submodule, optionale Companion-Projekte, Berechtigungen und Sicherheitsgrenzen nur
  unzureichend.

`README_C7_0_DRAFT.md` ist der erste faktenbasierte Ersatzentwurf. Er ersetzt die bestehende README
noch nicht.

## 9. Wiki- und Dokumentationsquellen

### Bestand

- Unter `docs/` liegen 117 Markdown-Dateien sowie EMScript-Fixtures, CSV/EBNF/XML und ein DOCX-
  Referenzentwurf.
- Das Superprojekt besitzt noch kein `docs/wiki/`.
- Das BlockEditor-Submodule enthaelt 19 versionierte Seiten unter `docs/wiki/` sowie ein
  `PUBLIC_READINESS.md`.
- Die BlockEditor-Seiten bezeichnen sich selbst teilweise als Draft/Pre-Public und duerfen nicht
  ungeprueft als WSS-Produktwahrheit gespiegelt werden.

Ein read-only Linkscan ueber Root-README und `docs/**/*.md` fand in 118 Dateien keine fehlenden
relativen Markdown-Links und keine absoluten lokalen Markdown-Links. Zwei getrackte Markdown-Texte
enthalten dennoch fest codierte lokale Dateisystempfade als Inline-Code/Plaintext:

- die C7-Grundlage in `docs/ROADMAP.md`, deren externe Quelldatei lokal nicht vorhanden war,
- eine Legacy-Referenz in der WSS-Orchestrator-Skilldatei.

Diese Pfade sind fuer eine oeffentliche Dokumentation nicht portabel und muessen spaeter durch
repository-relative Quellen oder eine nachvollziehbare historische Notiz ersetzt werden.

### Wahrheitsreihenfolge

Fuer oeffentliche Texte gilt bis zur naechsten Audit-Runde:

1. `docs/FEATURE_STATUS_MATRIX.md` fuer Implementierungsstatus,
2. `docs/architecture/RELEASE_1_0_SCOPE.md` fuer Produktumfang,
3. `docs/architecture/RELEASE_1_0_PLAN.md` fuer Gates und kritischen Pfad,
4. Architektur- und Milestone-Audits fuer konkrete Nachweise,
5. README, Wiki, Release Notes und Known Limitations als abgeleitete Publikationen.

`docs/RELEASE_NOTES_DRAFT.md` und `docs/KNOWN_LIMITATIONS.md` stammen im Kern noch vom
18./20. September. Die Release Notes nennen einen Release-Kandidaten, obwohl die aktuelle
Release-Scope-Datei sieben wesentliche Blocker ausweist. Beide Dokumente muessen vor einer
Veroeffentlichung gegen die aktuelle Matrix neu geschrieben werden.

## 10. Lizenz- und Artefaktbefund

- GitHub erkennt die Root-Lizenz korrekt als Apache-2.0.
- `THIRD_PARTY_NOTICES.md` dokumentiert zentrale Abhaengigkeiten und weist auf gesonderte
  Ultralytics-Bedingungen hin.
- Sieben getrackte YOLO/LiteRT-Modelldateien belegen zusammen rund 21,70 MiB.
- Die Modelldateien liegen zwar in einem separaten Vision-Provider-Pfad und nicht im Host-APK,
  sind aber Bestandteil desselben oeffentlichen Git-Repositories.

Vor einer bewussten Neuveroeffentlichung ist deshalb fuer jedes Modell Herkunft, konkrete Lizenz,
Weitergaberecht, Modellkarte und erforderliche Attribution nachzuweisen. Das Root-Label
`Apache-2.0` darf nicht als pauschale Lizenzbehauptung fuer separat lizenzierte Modelle verstanden
werden. Dieses Audit ersetzt keine rechtliche Pruefung.

## 11. Feature-basierte Produktwahrheit

Die am Audit-HEAD `711e13f` committete Matrix umfasst exakt 146 Features:

| Status | Anzahl |
| --- | ---: |
| `IMPLEMENTED` | 54 |
| `PARTIAL` | 44 |
| `PLANNED` | 30 |
| `DEFERRED` | 12 |
| `CONCEPT` | 5 |
| `BLOCKED` | 1 |

Die oeffentliche Darstellung darf daher weder "nur UI-Prototyp" noch "Stable 1.0" behaupten.
Sachlich korrekt ist ein aktives Pre-Alpha/Entwicklungsrepository mit belastbaren Kernpfaden und
wesentlichen offenen Release-Gates.

Waehrend des Audits wurde die Matrix durch fremdes, uncommittetes M5-8-WIP weiterbearbeitet. Dieser
Entwurf uebernimmt diese laufenden Statusverschiebungen nicht. Nach Abschluss und Review von M5-8
muss der Publikationsstand bewusst neu gegen den dann akzeptierten Commit abgeglichen werden.

## 12. Priorisierte Befunde

### P0 vor jedem Push des aktuellen lokalen `main`

1. BlockEditor-Gitlink `9a99f9b` im Remote-Repository erreichbar machen und per frischem Clone
   verifizieren.
2. Fremdes Runtime-, IME- und ShapeMaker-WIP klar aus einem Publikationscommit heraushalten.
3. Lizenz-/Distributionsentscheidung fuer die sieben Vision-Modelle treffen.
4. Release Notes, Known Limitations und README auf die aktuelle Feature-Matrix zurueckfuehren.

### P1 vor einer beworbenen oeffentlichen Projektseite

1. Produktname, Repository-Slug, Reifegrad und Sprache freigeben.
2. Eingebettete Repositories versus Submodule verbindlich festlegen.
3. GitHub-Beschreibung, Topics, Branch-Schutz, Merge-Policy und minimale Community-Dateien setzen.
4. Wiki aus versionierten Quellen initialisieren; keine manuell driftende zweite Wahrheit anlegen.
5. Frischen Clone mit Submodulen, Build/Test und dokumentierten Voraussetzungen pruefen.

## 13. Offene Freigabeentscheidungen

- Oeffentlicher Produktname: `VisualTasker Studio` oder `VisualTasker Studio WSS`?
- GitHub-Slug: bestehenden Slug behalten oder nach Freigabe auf `visualtasker-studio` kuerzen?
- Reifegrad: empfohlene Kennzeichnung `Pre-Alpha / active development` akzeptieren?
- Sprache: Deutsch als Hauptsprache, Englisch oder zweisprachiger Einstieg?
- Repository-Topologie: ChartGraph und IME als vendorte Snapshots, Submodule oder getrennte
  Projekte?
- Submodule-Branches: Feature-Branches weiter referenzieren oder auf stabile Integrationsbranches/
  Tags umstellen?
- Wiki-Modell: versionierte `docs/wiki` als Source of Truth mit GitHub-Wiki als Projektion?
- Vision-Modelle: im Repository behalten, extern beziehen oder erst nach Lizenznachweis
  veroeffentlichen?
- GitHub-Governance: Schutzregeln, Merge-Methode, Actions-Allowlist, Signoff und Supportgrenzen?
- Versionssignal: `versionName = 1.0` vor Stable beibehalten oder auf eine Pre-Release-Version
  umstellen?

## 14. C7-0 Ergebnis

C7-0 ist als Bestandsaufnahme abgeschlossen. Es wurden nur die vier vorgesehenen Dokumente neu
angelegt. Repository, Remotes, Branches, Gitlinks, bestehende README und fremdes WIP wurden nicht
veraendert. Die Ausfuehrung des Publikationsplans benoetigt die Entscheidungen aus Abschnitt 13.
