# M5-8 Provider Policy, Capability Discovery and Runtime Integration

Stand: 2026-10-11

## Ergebnis

M5-8 erweitert die vorhandene `ExecutionProviderRegistry`; es existiert keine zweite Registry und
keine zweite Runtime. `WorkflowGraph -> ExecutionPlan -> CapabilityRequest -> ProviderResolution ->
ExecutionProvider -> ProviderResult -> ExecutionTrace` bleibt der einzige produktive Pfad.

Der bestehende typisierte `ProviderResult` erfuellt die Rolle des Capability-Ergebnisses. Ein
zweites `CapabilityResult`-Modell wird bewusst nicht eingefuehrt. Kanonische `CapabilityId` und
`ProviderId` stammen weiterhin aus `emscript-language-core`; der Runtime-Adapter bildet bestehende
Capability-Namen deterministisch darauf ab.

## Vertraege

- `CapabilityDescriptor`: Capability-ID, Runtime-Capability, Eingabe-/Ergebnistyp und Seiteneffekt.
- `ProviderDescriptor`: stabile Provider-ID, Host, Capabilities, Anforderungen und Prioritaet.
- `ProviderAvailability`: Registrierung, Installation, Verbindung, Permission, Capability und
  Ausfuehrbarkeit sind getrennte Zustaende. `INSTALLED` bedeutet nicht `EXECUTABLE`.
- `ProviderSelectionPolicy`: explizite Auswahl, Ausschluss, Praeferenz und optionaler sicherer
  Fallback. Sie ist Request-Konfiguration und erzeugt keine Workflow-Entity.
- `ProviderFailure`: Fehlerklasse plus `NOT_EXECUTED`, `EXECUTED`, `MAY_HAVE_EXECUTED` oder
  `UNKNOWN`.
- `ProviderDiscovery`: deterministisch sortierte Beschreibung und request-spezifische
  Verfuegbarkeit registrierter Provider.

## Resolution und Fallback

1. Capability und kanonische Command-ID kommen aus dem ExecutionPlan.
2. `supports` filtert Capability, Command und gegebenenfalls Parametertypen.
3. Ausschluesse werden angewendet.
4. Eine explizite Provider-ID ist bindend. Fehlt sie oder ist sie nicht ausfuehrbar, endet der
   Request mit Diagnose; es gibt keine stille Substitution.
5. Automatische Auswahl sortiert nach Praeferenz, absteigender Prioritaet und stabiler Provider-ID.
6. Availability wird vor dem Invoke geprueft; doppelte Provider-IDs werden abgewiesen.

Default ist `DISABLED`. `SAFE_PRE_EXECUTION_ONLY` erlaubt einen Folgeprovider nur, wenn dieser
kompatibel und ausfuehrbar ist und der erste Fehler sicher `NOT_EXECUTED` meldet. Timeout,
Cancellation, `EXECUTED`, `MAY_HAVE_EXECUTED` und `UNKNOWN` werden niemals automatisch wiederholt.
Jede Entscheidung erscheint als `ProviderResolutionTraceInfo` in Runtime-, Execution- und
RailTrace.

## Provider-Matrix

| Capability | Provider | Host | Permission/Anforderung | Input | Result | Status |
| --- | --- | --- | --- | --- | --- | --- |
| TIMING / `wait` | `android.native.timing` | Android Runtime | keine | Number ms | Void | LIVE |
| A11Y / `clickPoint`, Swipe, PointerPath, MultiPath | `android.native.touch` via `android.native.accessibility` | AccessibilityService | verbundener Dienst, Gesture-Capability | Point/Path/MultiPath | Void | LIVE |
| A11Y / `clickText` | `android.native.accessibility` | AccessibilityService | Freigabe und verbundener Dienst | String query | Void + TargetTrace | LIVE |
| TASKER | `visualtasker.tasker` | Tasker Broadcast Host | App, RUN_TASKS, External Access, Receiver | Task/Event/Variable metadata | accepted/async | PARTIAL |
| SHIZUKU | `visualtasker.shizuku` | Shizuku Binder | App, Binder, explizite Freigabe | status/UserService/shell metadata | suspend host result | PARTIAL |
| TERMUX | `visualtasker.termux` | RunCommandService | App und RUN_COMMAND | typed path/args | accepted/async | PARTIAL |
| CUSTOM_TAB | `visualtasker.customtabs` | Browser Custom Tabs Service | kompatibler Browser | URL/settings | launch/async events | PARTIAL |

Die optionalen Hosts werden durch `AndroidOptionalProviderInventory` ueber ihre bestehenden
Registrierungsobjekte erfasst. Sie werden noch nicht als vollwertige `ExecutionProvider`
registriert: Tasker besitzt keinen allgemeinen Callback-Abschluss, Shizuku benoetigt einen
gebundenen Timeout-/Cancellation-Adapter, Termux liefert noch kein typisiertes Prozessende und
Custom Tabs trennt Launch von asynchronen Navigationsevents. Insbesondere wird keine rohe
Benutzereingabe automatisch als Shell-Befehl gebunden.

## Trace, Invarianten und Tests

Trace transportiert Capability-ID, Provider-ID, Auswahlgrund, Availability, Permission,
Ausfuehrungsstatus, Fehlerklasse, Fallbackentscheidung, Cancellation/Timeout und Resultatstatus.
Argumente, Tokens, Shell-Ausgaben und externe Payloads werden nicht in den Provider-Trace kopiert.

- Providerwahl mutiert weder WorkspaceDocument noch Source-Anker oder Editor-Layouts.
- Runtime und Provider nutzen stabile semantische Entity-IDs aus dem ExecutionPlan.
- Native Wait-, Gesture-, MultiPath- und Accessibility-Ausfuehrung bleiben auf derselben Registry.
- Nicht unterstuetzte Commands werden vor Ausfuehrung diagnostiziert.
- Optionale Provider bleiben optional und duerfen keine Permission automatisch eskalieren.

`ProviderPolicyTest` prueft eindeutige, explizite, priorisierte und ausgeschlossene Auswahl,
stabilen Tie-Break, Installation/Connection/Permission, fehlende Capability, Providerverlust,
Cancellation, Timeout, unbestimmten Seiteneffekt, erlaubten/verbotenen/inkompatiblen Fallback,
typisierte Werte, Trace und doppelte IDs. `AndroidOptionalProviderInventoryTest` prueft die vier
optionalen Hosts mit kontrollierten Status-Fakes. Bestehende Native-, Roundtrip- und M4-Gates
bleiben der Regressionsnachweis.

## Offene Grenzen

- Externe Hosts sind Discovery- und Diagnose-integriert; ihre vollstaendige ExecutionProvider-
  Ausfuehrung bleibt M9.
- Der prozessweite Headless Host und ein begrenzter Recovery-Checkpoint sind seit M5-H produktiv.
- Vollstaendige persistente Trace-Retention und Langlauf-Abnahme bleiben M5-10.
- Vision, OCR, Remote Execution und neue Sprachfeatures sind nicht Teil dieses Slices.
