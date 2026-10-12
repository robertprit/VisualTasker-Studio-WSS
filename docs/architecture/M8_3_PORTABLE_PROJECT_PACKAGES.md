# M8-3 Portable Project Packages

Stand: 2026-10-12

## Ziel und Ownership

M8-3 fuehrt das versionierte ZIP-basierte Format `.vtpkg` ein. Es gibt weiterhin genau eine
Projektserialisierung: `ProjectSerializer` schreibt und liest das kanonische `ProjectDocument`.
`ProjectPackageCodec` besitzt nur Manifest, Abhaengigkeitsaufloesung, Integritaet und Paketgrenzen.

```text
ProjectDocument
  -> Dependency Resolution
  -> portable ProjectSerializer snapshot
  -> manifest.json + project.json + assets/*
  -> deterministic ZIP

.vtpkg
  -> bounded Preflight
  -> ProjectSerializer validation
  -> isolated staging
  -> atomic directory finalize
  -> atomic active-project persistence
```

## Format

Paketformat `1` enthaelt:

- `manifest.json`,
- `project.json` als kanonischen ProjectSerializer-Snapshot,
- sortierte `assets/*`-Eintraege fuer paketierte Ressourcen.

Das Manifest enthaelt Paket- und Projektschema, Projekt-/Root-/Resource-IDs, Ressourcentyp und
Scope, Dependency-Art, portable Referenz, MIME-Typ, Groesse, SHA-256, Provider-Abhaengigkeiten,
Exportzeitpunkt und Inhalts-Fingerprint. ZIP-Eintraege werden sortiert und mit neutraler Zeit
geschrieben. Der Exportzeitpunkt ist absichtlich nicht Teil des Inhalts-Fingerprints.

Projektlokale absolute Pfade werden weder in `manifest.json` noch in `project.json` transportiert.
Sie werden als `package:<resourceId>` serialisiert und beim Import in das private Projektverzeichnis
relokiert. Externe oder AppShared-Ressourcen muessen eine portable logische Referenz besitzen;
geraetelokale Datei- und Content-URIs werden fuer diese Scopes abgewiesen.

## Dependency Resolution

| Art | Export | Import |
| --- | --- | --- |
| `Bundled` / ProjectLocal | Payload ins Paket, Hash und Groesse im Manifest | Verifiziert in projektlokalen Storage relokiert |
| `Embedded` | Payload ins Paket, portable `embedded:`-Referenz | Verifiziert und unter Erhalt des Embedded-Scopes relokiert |
| `AppShared` | Nur portable Abhaengigkeit deklarieren | Nicht heimlich kopieren; Provider bleibt erforderlich |
| `External` | Nur Provider-/externe Referenz deklarieren | Nicht heimlich kopieren; Provider bleibt erforderlich |
| `VolatileCapture` | Nur `capture-transient:` deklarieren | Kein alter Frame wird als aktuelle Datei ausgegeben |

Die aktuelle Registry ist konservativ: alle im `ProjectDocument` registrierten ProjectLocal- und
Embedded-Ressourcen gehoeren zum Projektbestand und werden paketiert. Dadurch werden noch nicht von
allen Consumer-Projektionen erfasste, aber bewusst registrierte Assets nicht still verloren.
Explizite ProjectReferences und Provider-Bindings bleiben im ProjectDocument erhalten.

## Preflight und Sicherheit

Der Import liest das ZIP begrenzt und extrahiert vor erfolgreichem Preflight nichts. Geprueft werden:

- Paket- und Projektschema,
- Manifest-Vollstaendigkeit und ProjectSerializer-Validierung,
- Datei- und Gesamgroessen, Eintragszahl und Kompressionsverhaeltnis,
- relative, normalisierte Pfade ohne Traversal, Laufwerkspraefix oder Backslash,
- doppelte ZIP-Pfade, Root-/Resource-IDs und Asset-Pfade,
- Manifestinventar gegen ProjectDocument,
- Groesse und SHA-256 jedes paketierten Assets,
- unbekannte, nicht im Manifest deklarierte ZIP-Eintraege,
- Provider-Abhaengigkeiten und Projekt-ID-Konflikte,
- deterministischen Inhalts-Fingerprint.

ZIP-Inhalte werden ausschliesslich als normale Dateien in einen kanonisch begrenzten Staging-Pfad
geschrieben. Es werden keine Archiv-Symlinks angelegt oder verfolgt. Fehler und Cancellation liefern
typisierte Diagnosen und fuehren zu keinem partiellen Import.

## Atomarer Import und Recovery

`ProjectPackageFileRepository` registriert zuerst einen unveraenderten Snapshot des aktuell aktiven
Projekts. Der neue Import entsteht in `.staging-*`, wird dort gehasht, serialisiert und erneut
dekodiert. Erst danach wird das Verzeichnis auf demselben Dateisystem umbenannt. Anschliessend wird
das aktive ProjectDocument mit synchronem SharedPreferences-Commit gesetzt. Scheitert diese letzte
Aktivierung, wird nur das gerade importierte, anhand seiner Project-ID verifizierte Verzeichnis
zurueckgerollt. Fremde Dateien und bestehende Projektverzeichnisse werden nicht geloescht.

Asset-Freigabe ist absichtlich nur ein Guard: Eine physische Loeschung ist erst erlaubt, wenn weder
ein registriertes Projekt noch Undo-/Recovery-Historie die Location haelt. M8-3 implementiert keine
automatische Garbage Collection.

## Identitaet und Konflikte

Ein normaler Roundtrip erhaelt Project-, Root-, Definition-, Resource- und semantische IDs sowie
Source Anchors, unbekannte Metadaten und Projektionsdaten. Relokation aendert nur die physische
Resource-Location und den daraus abgeleiteten Hashhinweis.

Bei einem Projekt-ID-Konflikt gilt standardmaessig `Abort`. `ImportAsCopy` erzeugt eine neue
Project-ID und einen Kopienamen. Root-, Definition-, Resource-, Entity- und Relation-IDs bleiben
unveraendert, weil sie innerhalb des neuen Projekt-Owners bereits konsistent referenziert sind. Ein
pauschales Remapping wuerde Source Anchors und interne Referenzen ohne fachlichen Grund brechen.

Beim Projektwechsel werden Explorer-History und projektionsweite Selection geloescht. Dadurch kann
Undo keinen Zustand aus dem vorherigen Projekt in das importierte Projekt tragen.

## Android SAF

Der Project Explorer besitzt ein kompaktes Paketmenue fuer Export, Import und Import als Kopie.
Export verwendet `CreateDocument`, Import `OpenDocument`; es werden keine Storage-Permissions
angefordert. Vor dem Schreiben in das SAF-Ziel wird das vollstaendige Paket in einer temporaeren
App-Datei erzeugt, synchronisiert und erneut per Preflight validiert.

Die atomare Finalisierung des internen Imports ist garantiert. Die externe SAF-Implementierung des
gewaehlten DocumentProviders kann dagegen kein universelles atomisches Rename garantieren. Ein bei
Providerfehler teilweise geschriebenes Dokument besteht den Paket-Preflight nicht und wird niemals
als gueltiger Import akzeptiert.

## Tests und Abnahme

Die Paket-Suite deckt leere, Single-/Multi-Root- und metadatenreiche Projekte, alle aktuellen
Resource-Typen, Embedded/AppShared/External/Transient, Hash/Size/Schema/Manifest, Pfad-Traversal,
Limits, Cancellation, Konflikt/Kopie, Relokation, Rollback, Reload, Fingerprint und Release-Guards
ab. Die bestehenden M4-, Runtime-, BlockEditor- und FlowEditor-Gates belegen Three-Editor- und
ExecutionPlan-Kompatibilitaet des unveraenderten ProjectDocument-Vertrags.

| AC | Status | Nachweis / Grenze |
| --- | --- | --- |
| AC-01 Versionierter Paketvertrag | PASS | `.vtpkg`, Formatversion 1, Manifest + ProjectSerializer |
| AC-02 Dependency Resolution | PASS | Fuenf explizite Dependency-Arten und Providerliste |
| AC-03 Asset-Integritaet | PASS | SHA-256, Groesse, Relokation und Missing-Asset-Diagnose |
| AC-04 Deterministischer Export | PASS | sortierte Eintraege, neutrale ZIP-Zeit, stabiler Inhalts-Fingerprint |
| AC-05 Sicherer Preflight | PASS | bounded read, Traversal-/Duplikat-/Hash-/Schema-Pruefung |
| AC-06 Atomarer Import | PASS | isoliertes Staging, Rename, verifizierter Rollback |
| AC-07 ID-/Referenzintegritaet | PASS | normaler Roundtrip erhaelt IDs; Copy-Policy ist explizit |
| AC-08 Konfliktbehandlung | PASS | Abort oder explizites ImportAsCopy |
| AC-09 Android SAF | PASS | minimale CreateDocument-/OpenDocument-Einstiegspunkte |
| AC-10 Editor-/Runtime-Kompatibilitaet | PASS | bestehende Projekt-, M4- und Runtime-Vertraege unveraendert |
| AC-11 Datenintegritaet/Recovery | PASS | Snapshot, Staging-Cleanup, Aktivierungsrollback, Release-Guard |
| AC-12 Regressionen | PASS | Package-, App-, Block-, Flow-, M4- und Build-Gates |

## Bewusste Grenzen

- Kein Cloud-/VT2VT-Transport und keine Signatur oder Verschluesselung.
- AppShared-/External-Provider werden deklariert, aber nicht in M8-3 implementiert.
- Kein automatisches Asset-GC; Freigabe bleibt eine explizite, pruefbare Entscheidung.
- Der Explorer bietet Import/Export, aber noch keinen vollstaendigen Mehrprojekt-Katalog zum
  beliebigen Umschalten zwischen allen registrierten Snapshots.
- Ein echter SAF-Import/Export auf dem Geraet ist wegen der geforderten isolierten Testumgebung ein
  separates, ausdruecklich freizugebendes Abnahmegate.
