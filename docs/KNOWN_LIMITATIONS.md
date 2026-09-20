# Known Limitations - Stable V1 Candidate

Stand: 2026-09-18

## Release-Blocker vor Stable V1

- Die vollstaendige manuelle Geraete-Abnahmematrix ist noch nicht abgeschlossen.
- FlowEditor AutoArrange und Routing sind technisch getestet, benoetigen aber
  eine finale visuelle Abnahme mit realen grossen Workflows.

## Adapter

- OCR, OCV und YOLO besitzen noch keinen registrierten produktiven Live-Provider.
  Vision Crop/Compare bleibt lokal nutzbar.
- Termux RUN_COMMAND setzt `allow-external-apps=true` voraus. Der Wert ist fuer
  andere Android-Apps nicht direkt lesbar; Ablehnungen werden beim Live-Aufruf
  diagnostiziert. Eine vollstaendige Result-Session ist noch offen.
- Tasker Action und Feedback funktionieren als Grundpfad; ein vollstaendiges
  Locale-Event-/Condition-Paket ist nicht Bestandteil dieses Kandidaten.
- VT2VT LAN/USB-ADB uebertraegt validierte read-only Workspace- und
  RuntimeTrace-Mirrors. Discovery, WebSocket-Komfortpfad und die manuelle
  Zwei-Geraete-Abnahme des neuen Vertrags sind noch offen.

## Editoren und Assets

- Das adaptive `Studio-Layout` ist in den Workspace-Layout-Einstellungen
  verfuegbar. Es ersetzt nicht benutzerdefinierte gespeicherte Layouts und wird
  nur auf ausdruecklichen Aufruf angewandt.
- Reporter-/Operator-/Dataflow-Details sind im FlowEditor optionale Analyse-Layer
  und keine zweite BlockEditor-Darstellung.
- VisualAsset-Katalog, `.ema`-Import/Export und Integritaetspruefung existieren;
  die vollstaendige bidirektionale ShapeMaker-Bearbeitung bleibt offen.
- Rich Compose Controls auf Bloecken/Nodes sind nur als bewusstes Overlay-Konzept
  vorgesehen und nicht Teil des serialisierten Zeichenkerns.

## Daten

- Kanonische Recording-Records modellieren derzeit Tap-Interaktionen als
  Scenes/Frames/A11y/Interaction-Vertrag. Activity-, Text-, Scroll-, Swipe- und
  weitere Gesten bleiben korrelierte append-only Raw-Evidence, bis ihre
  kanonischen Schemas und Migrationen festgelegt sind.
- RAG und AI/ML duerfen erst nach Stable V1 als read-only Proposal-/Index-Schicht
  an Datastore und Worldview angebunden werden.
- Fehlende Plugin-Adapter bleiben Capability-Warnungen. Strukturell ungueltige
  Workflows oder fehlende referenzierte `.ema`-Dateien blockieren den Preflight.
