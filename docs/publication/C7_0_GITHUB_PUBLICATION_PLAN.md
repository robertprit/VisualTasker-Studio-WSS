# C7-0 GitHub-Publikationsplan

Stand: 2026-10-11

Status: Entwurf. Keine der folgenden Remote-Aktionen ist durch dieses Dokument freigegeben.

## 1. Zielbild

Das GitHub-Repository soll eine reproduzierbar klonbare, wahrheitsgetreu beschriebene und
wartbare Projektoberflaeche fuer VisualTasker Studio bilden. Technische Arbeitsnamen,
Feature-Branches, optionale Provider und Forschungsflaechen duerfen die Produktwahrheit nicht
ueberlagern.

Empfohlenes Namensmodell, vorbehaltlich Freigabe:

| Ebene | Empfehlung |
| --- | --- |
| Produkt | `VisualTasker Studio` |
| Architekturkuerzel | `WSS` nur intern/technisch erklaeren |
| GitHub-Slug | `visualtasker-studio` |
| Android Namespace | vorerst `com.visualtasker.wss`; separate Migrationsentscheidung |
| Reifegrad | `Pre-Alpha / active development` |

Eine Umbenennung des GitHub-Repositories erfolgt erst nach ausdruecklicher Freigabe. GitHub leitet
alte Repository-URLs in der Regel weiter; AppInfo, Dokumente, Submodule, Badges, Clone-URLs und
externe Verweise muessen trotzdem bewusst migriert werden.

## 2. Nicht verhandelbare Publikationsregeln

- Kein Push, Tag, Release, Remote-Rename oder Settings-Write ohne separate Freigabe.
- Keine Bereinigung oder Aufnahme fremden Runtime-, IME- oder ShapeMaker-WIP.
- Kein Superprojekt-Push mit einem auf GitHub nicht erreichbaren Gitlink.
- Keine Stable-, RC- oder Vollstaendigkeitsbehauptung entgegen der Feature-Matrix.
- Keine pauschale Apache-2.0-Aussage fuer separat lizenzierte Modellartefakte.
- Keine lokale Dateisystempfade, Geraete-IDs, Tokens, private Screenshots oder persoenliche Daten in
  Publikationscommits.
- Abhaengigkeits-Repositories werden vor dem Superprojekt publiziert und aus einem frischen Clone
  verifiziert.

## 3. Freigabe-Gate P0: Entscheidungen

Vor jeder Umsetzung sind zu entscheiden:

1. finaler Produktname und Repository-Slug,
2. oeffentlicher Reifegrad und README-Sprache,
3. Topologie von ChartGraph und IME,
4. Submodule-Branch-/Tag-Policy,
5. Lizenz- und Distributionsweg der Vision-Modelle,
6. GitHub-Wiki versus versionierte Dokumentationsquelle,
7. Governance- und Supportniveau,
8. Pre-Release-Versionierung der Android-App.

Ergebnis dieses Gates ist ein kurzes, versioniertes Entscheidungsprotokoll. Ohne diese Freigabe
bleiben alle folgenden Schritte lokal.

## 4. Phase A: reproduzierbare Abhaengigkeiten

### A1. BlockEditor

Der aktuelle Superprojekt-Gitlink `9a99f9b` ist auf GitHub nicht aufloesbar. Vor jedem Push:

1. die 20 lokalen BlockEditor-Commits reviewen,
2. den Zielbranch festlegen,
3. nur nach Freigabe den Branch pushen,
4. `9a99f9b` ueber GitHub aufloesen,
5. einen frischen `--recurse-submodules`-Clone pruefen.

Der konfigurierte Feature-Branch darf nicht stillschweigend zum dauerhaften Releasekanal werden.

### A2. Flowchart

Der Gitlink `008b136` ist erreichbar. Vor der Publikation bleibt zu entscheiden, ob der
Feature-Branch als Integrationsbranch akzeptiert ist oder durch einen stabilen Branch/Tag ersetzt
wird.

### A3. M3ShapeMaker

Der Gitlink `e17072c` ist erreichbar, der lokale Submodule-Arbeitsbaum aber stark veraendert. Das
WIP wird weder gestaged noch durch ein Gitlink-Update verdeckt. Eine spaetere Integration braucht
einen eigenen Review-, Test- und Commitpfad.

### A4. ChartGraph und IME

Beide Verzeichnisse sind gleichzeitig eigene Git-Repositories und normale Dateien des
Superprojekts. Nach Freigabe ist genau ein Modell zu waehlen:

- **vendorter Snapshot:** Superprojekt ist alleinige Publikationswahrheit; eingebettete `.git`
  gehoeren nicht zum verteilten Checkout,
- **Submodule:** eigener Releasekanal und gepinnter Gitlink,
- **separates Produkt:** nur Vertrag/Version im WSS-Repository, kein Quellcode-Snapshot.

Die Umstellung erfordert einen eigenen migrationssicheren Slice und ist nicht Teil eines
kosmetischen README-Commits.

## 5. Phase B: lokaler Publikations-Snapshot

Nach P0 und Phase A:

1. Dirty State erneut inventarisieren.
2. C7-/Publikationsdateien explizit einzeln stagen; kein pauschales `git add`.
3. Temporaere Screenshots und lokale Hilfsdateien ausschliessen.
4. Relative Links, lokale Pfade und Beispieldaten pruefen.
5. Vollstaendigen Working-Tree- und History-Secretscan mit dokumentiertem Werkzeug ausfuehren.
6. Grosse Dateien und LFS-/Download-Policy pruefen.
7. Lizenzmanifest fuer Code, Submodule, Modelle und Bildressourcen erstellen.
8. `git diff --check` und Markdown-Linkcheck ausfuehren.

Dieser Snapshot ist noch kein Release. Er definiert nur einen reproduzierbaren, reviewbaren
Publikationskandidaten.

## 6. Phase C: oeffentliche Wahrheitstexte

Die Publikation wird in folgender Reihenfolge abgeleitet:

1. Feature-Matrix und Release-Scope bestaetigen.
2. Known Limitations auf die aktuellen sieben Hauptblocker bringen.
3. Release Notes von "Release Candidate" auf den tatsaechlichen Reifegrad korrigieren.
4. `README_C7_0_DRAFT.md` reviewen und nach Freigabe als Root-README uebernehmen.
5. Wiki-Seiten gemaess `docs/wiki/C7_0_WIKI_PLAN.md` aus versionierten Quellen erstellen.
6. AppInfo auf freigegebenen Namen, Git-URL, Version, Lizenz und Buildstand abgleichen.

Jede abgeleitete Seite nennt ihren Stand und vermeidet Zukunftsfunktionen im Praesens.

## 7. Phase D: minimale GitHub-Governance

Nach Freigabe lokal vorbereiten:

- `.github/ISSUE_TEMPLATE/bug_report.yml`,
- `.github/ISSUE_TEMPLATE/feature_request.yml`,
- `.github/pull_request_template.md`,
- `SECURITY.md`,
- `CONTRIBUTING.md`,
- optional `CODE_OF_CONDUCT.md`,
- ein minimaler CI-Workflow fuer dokumentierte JVM-/Compile-Gates.

Keine Datei verspricht Support, SLA, Sicherheitsreaktionszeiten oder externe Beitraege, solange
diese Zusagen nicht bewusst beschlossen sind.

Empfohlene GitHub-Einstellungen:

| Bereich | Empfehlung |
| --- | --- |
| Default Branch | `main` |
| Merge | bevorzugt Squash; andere Methoden nur bei Bedarf |
| Branch Cleanup | nach Merge aktivieren |
| Branch Protection | PR oder explizite Admin-Ausnahme, erforderliche CI-Gates |
| Force Push / Delete | fuer `main` sperren |
| Actions | verwendete Actions erlauben und auf Commit-SHAs pinnen |
| Signoff/Signatures | fuer Release-Commits und Tags verbindlich festlegen |
| Releases | erst nach reproduzierbarem RC-Gate |

## 8. Phase E: Metadaten und Wiki

Erst nach dem inhaltlichen Commit:

1. Beschreibung setzen, zum Beispiel:
   `Android-native visual automation studio with synchronized text, block and flow editors.`
2. Topics freigeben, zum Beispiel:
   `android`, `kotlin`, `jetpack-compose`, `automation`, `visual-programming`, `workflow`.
3. Homepage nur setzen, wenn eine gepflegte Zielseite existiert.
4. Repository gegebenenfalls umbenennen.
5. Wiki mit einer freigegebenen Home-Seite initialisieren.
6. Source-zu-Wiki-Revision dokumentieren.

Der Wiki-Inhalt wird nicht manuell vom Repository entkoppelt weiterentwickelt. Die versionierten
Quellen bleiben die Review- und History-Basis.

## 9. Phase F: Verifikationsmatrix vor Push/Release

| Gate | Nachweis |
| --- | --- |
| Clone | frischer Clone mit `--recurse-submodules` |
| Gitlinks | jeder SHA auf dem jeweiligen GitHub-Repository erreichbar |
| Docs | Linkcheck, keine lokalen Pfade, Feature-Status konsistent |
| Lizenz | Root, Third Party, Modelle und optionale Provider geklaert |
| Build | dokumentierter JDK-/SDK-Stand, fokussierte Tests und App-Compile |
| Android | Install, Launch und Foreground getrennt von UI-/Runtime-Abnahme |
| Security | Working Tree und History gescannt; Logs/Fixtures redigiert |
| Release | kein Stable-Tag ohne M10-/RC-Gates |

Ein erfolgreicher Build ersetzt weder Submodule-Reproduzierbarkeit noch Lizenz-, Dokumentations-
oder Geraeteabnahme.

## 10. Sichere Push-Reihenfolge nach spaeterer Freigabe

1. Freigegebene Abhaengigkeitsbranches/-tags pushen.
2. Gitlinks per GitHub und frischem Clone verifizieren.
3. Superprojekt-Publikationscommit reviewen.
4. Superprojekt-Branch pushen, noch keinen Tag erstellen.
5. CI und Clone erneut pruefen.
6. GitHub-Metadaten/Wiki aktualisieren.
7. Erst nach Release-Gates signierten Pre-Release-/Release-Tag erstellen.

## 11. Definition of Done fuer die Repository-Aufraeumung

- Name und Reifegrad sind ueber App, README und GitHub konsistent.
- Kein Superprojekt-Gitlink verweist auf einen nicht erreichbaren Commit.
- Embedded-Repositories haben eine dokumentierte Eigentumsstrategie.
- README, Wiki, Known Limitations und Release Notes stammen aus demselben Feature-Stand.
- Lizenz- und Modellverteilung ist nachvollziehbar.
- GitHub besitzt minimale Schutz-, CI- und Community-Konfiguration.
- Ein neuer Nutzer kann den dokumentierten Stand reproduzierbar klonen und bauen.
- Stable wird erst behauptet, wenn die Release-Gates tatsaechlich geschlossen sind.
