# EMScript Capability Descriptor Schema — WSS Reference

> **Status:** Adapted Reference
>
> **Origin:** `visualtasker-studio/docs/emscript/v0.1/EMSCRIPT_CAPABILITY_SCHEMA.md`

## Kernidee

Capability-Descriptoren sind syntaxunabhängig und enthalten keine Android-UI- oder Framework-Klassen. Ein stabiler Descriptor beschreibt, **was** eine Operation semantisch erwartet und liefert; Provider entscheiden, **wie** sie ausgeführt wird.

```kotlin
@JvmInline value class CapabilityId(val value: String)
@JvmInline value class NamespaceId(val value: String)
@JvmInline value class FunctionId(val value: String)

data class FunctionDescriptor(
    val capabilityId: CapabilityId,
    val namespaceId: NamespaceId,
    val functionId: FunctionId,
    val parameters: List<ParameterDescriptor>,
    val returns: ReturnDescriptor,
    val permissions: List<PermissionRequirement>,
    val providers: List<ProviderRequirement>,
    val diagnostics: List<DiagnosticDescriptor>,
    val flowNode: FlowNodeDescriptor?,
    val block: BlockDescriptor?,
    val legacyAliases: List<LegacyAliasDescriptor>,
)
```

Die konkrete Kotlin-Form ist Referenz, nicht WSS-Implementierungsbehauptung.

## Verantwortungen

Ein Descriptor darf enthalten:

- stabile Capability-ID;
- Namespace/Funktion;
- typisierte Parameter;
- Required/Nullable/Default-Regeln;
- erlaubte Werte und Validierungsregeln;
- Return-Type;
- Permission-Anforderungen;
- Provider-Anforderungen;
- stabile Diagnostic-Codes;
- Projektionsmetadaten für Block und Flow;
- explizite Legacy-Aliase.

Ein Descriptor darf **nicht** besitzen:

- Android `Context`, `Intent` oder Compose State;
- konkrete Provider-Implementierung;
- Panel-Layout;
- Runtime-Session-Zustand;
- persistente Workflow- oder Worldview-Wahrheit.

## Wiederverwendung

Dieselbe Descriptor-Schicht kann als gemeinsame Quelle dienen für:

- Parser- und Validator-Prüfung;
- Code Completion;
- EMScript-Dokumentation;
- Blockeditor-Definitionen;
- Flowchart-Metadaten;
- AI/Function-Calling Toolbeschreibungen;
- Provider-Capability-Prüfung;
- Generator-Signaturen.

Das ist ein besonders wertvoller Vertrag für WSS, weil damit nicht jede Projektion ihre eigene Funktionsdefinition erfinden muss.

## WSS-Anpassung

Im heutigen WSS sollte die Kette fachlich so aussehen:

`Intent / EMScript / Block / Flow -> CapabilityId + typed args -> Validator -> ProviderResolver -> Provider`

Provider-Ergebnisse können Actions, typed Results oder Observations erzeugen. Observation-Resultate gehen anschließend über Worldview-Verträge weiter; der Capability-Descriptor selbst wird dadurch nicht Worldview-Authority.

## Value Constructors

Der alte Vertrag trennt Capability Calls von unveränderlichen Value Constructors. Diese Trennung bleibt sinnvoll:

- Capability Call: darf Aktion/Observation auslösen.
- Value Constructor: erzeugt validierten strukturierten Wert, ohne Side Effect.

Arbitrary Reflection, freie Methodenaufrufe und implizite Android-Typen sollten daraus nicht entstehen.
