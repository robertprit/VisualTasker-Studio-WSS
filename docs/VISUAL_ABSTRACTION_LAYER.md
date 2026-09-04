# WSS Visual Abstraction Layer

## Ziel

Der Visual Abstraction Layer uebersetzt Domain-Semantik in visuelle Semantik.
Er ist kein Theme, keine Compose-Komponentenbibliothek und kein Ort fuer
Material- oder Android-Typen.

Die Abhaengigkeit bleibt strikt:

```text
Domain/Projection -> Visual Semantics -> Visual Policy -> Material/Compose
```

## MVP-Scope

Der erste Slice besteht aus:

- `VisualSemanticState`: orthogonale semantische Achsen.
- `VisualDescriptor`: renderer-neutrale visuelle Rollen.
- `DefaultVisualPolicyResolver`: zentrale Prioritaetsregeln.
- Projektionsadapter fuer Flowchart, Blockeditor und Worldview.
- `VisualSemanticsReporter`: kompakte Developer-Zusammenfassung fuer Debug-Panels.
- `MaterialVisualResolver`: erste M3-nahe Aufloesung von Descriptoren.

Noch nicht enthalten:

- konkrete Compose-Farben oder `Dp`-Werte,
- Material-Resolver,
- grosse Umstellung bestehender Panels,
- dekorative Expressive-Styles.

## Semantische Achsen

Der MVP nutzt:

- `role`: strukturelle Rolle, z. B. Workflow-Action, Condition, Resource.
- `authority`: Canonical, Derived, AI-Proposal, Human-confirmed, Unresolved.
- `certainty`: Known, Uncertain, Ambiguous, Conflicting.
- `activity`: Idle, Running, Waiting, Succeeded, Failed usw.
- `validation`: Valid, Warning, Invalid.
- `focus`: None, Focused, Selected, Editing.
- `availability`: Enabled, Disabled, Unavailable.
- `freshness`: Current, Historical, Stale.

Diese Achsen ersetzen keine Domainmodelle. Sie beschreiben nur, was fuer die
Darstellung relevant ist.

## Policy-Regeln

Wichtige Signale haben Prioritaet:

1. Invalid/Error ueberschreibt Warning, Runtime und Selection beim Outline.
2. Runtime-Aktivitaet bleibt als Motion sichtbar.
3. AI-Proposal, Ambiguity, Warning und Blocked bleiben diskrete Badges.
4. Projection kann die Form aendern, aber nicht die Bedeutung.
5. Farbe darf spaeter unterstuetzen, aber nie alleinige Semantik tragen.

## Naechste Integration

1. Flowchart-Shell: Node-Semantik ueber `FlowchartNodeVisualAdapter` ableiten.
2. Blockeditor: Blockrollen auf dieselben `VisualRole`/`SurfaceRole` mappen.
3. Inspector/Debug: `VisualSemanticState` und `VisualDescriptor` im Developer-Modus anzeigen.
4. Material-Resolver: renderer-neutrale Rollen in M3-Expressive-Tokens uebersetzen.
5. Runtime: Dry-Run/Live-Run Status ueber VAL vereinheitlichen.

## Implementierter Einstiegspunkt

- Flowchart Runtime Inspector zeigt fuer selektierte Nodes `VAL State` und
  `VAL Descriptor`.
- DebugInfo zeigt eine `VAL Flowchart`-Zusammenfassung fuer Node-Rollen,
  Runtime-Aktivitaet und Outlines.
- Tests sichern Policy, Material-Resolver und Projektionen ab.

## Rueckkehr zum Hauptarbeitsstamm

Der VAL-Slice ist ausreichend vorbereitet, sobald:

1. Flowchart-, Blockeditor- und Worldview-Adapter vorhanden sind.
2. DebugInfo und Flowchart-Inspector die Semantik anzeigen koennen.
3. Der Material-Resolver existiert, aber noch kein Rendering hart umgestellt ist.
4. Tests und App-Compile gruen sind.

Danach geht die Arbeit wieder an EMScript/Runtime/Plugin-Kommandos weiter.
