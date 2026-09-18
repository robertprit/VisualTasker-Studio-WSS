# VT2VT Mirror Contract

Stand: 2026-09-18

## Zweck

VT2VT spiegelt einen Workspace und seinen RuntimeTrace auf ein zweites Geraet.
Der Observer ist eine read-only Projektion und besitzt keine Autoritaet, den
lokalen `WorkspaceDocument` oder dessen getrennte View-Dokumente zu ersetzen.

## Workspace Mirror

- Quelle ist das kanonische serialisierte `WorkspaceDocument`.
- Der Transport nutzt `gzip+base64`, SHA-256 und ein versioniertes Payload-Schema.
- Der Empfaenger prueft Schema, Kodierung, Groessenlimit und Checksumme.
- EMScript wird nur als lesbare Projektion mitgefuehrt.

## RuntimeTrace Mirror

- Uebertragen werden Run-, Session- und Document-Identitaet, Sequenz, aktiver
  Step und Node, alle Node-States, durchlaufene Kanten und Diagnosen.
- Aeltere Revisionen und Runtime-Sequenzen werden verworfen.
- Selection, Log und Runtime bleiben getrennte Nachrichtentypen.

## Session und Transport

- Nachrichten werden anhand Source und Message-ID dedupliziert.
- Target-Peer und optionaler sechsstelliger Pair-Code werden geprueft.
- LAN/TCP und USB/ADB-Bridge verwenden denselben gerahmten Vertrag.
- Frames sind hart auf 8 MiB begrenzt; dekomprimierte Workspaces auf 4 MiB.
- Gleichzeitige Live-Mirror-Sendungen werden serialisiert.

Discovery und WebSocket sind Komfortschichten oberhalb dieses Vertrags und
duerfen seine Autoritaets-, Validierungs- und Read-only-Regeln nicht umgehen.
