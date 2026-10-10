# M5 Runtime Provider Matrix

## Contract

Capability describes required behavior. Provider describes a replaceable implementation.
`ExecutionContext` carries request-local state and cancellation. The runtime core knows none of the
Android service classes behind a provider.

| Capability family | Catalog/source today | M5-0 provider | Product provider status | Core dependency |
| --- | --- | --- | --- | --- |
| Timing `wait(duration)` | `action.wait`, capability `TIMING` | Deterministic fake provider | `AndroidWaitExecutionProvider` live in M5-1 | None |
| Core command invocation | `VisualTaskerCommandCatalog` | Deterministic fake provider | Not registered in M5-1 | None |
| Logging/feedback | Core catalog and existing BasicRun environment | Fake provider | Existing Android behavior, not migrated | None |
| Coordinate click `clickPoint(x,y)` | `input.clickPoint`, capability `A11Y` | Fake provider | `NativeTouchProvider` live in M5-2 | None |
| Swipe/path/multitouch/semantic click | Automation catalog | Fake provider | Not registered in M5-2 | None |
| Clipboard/file/cache/system | Core catalog | Fake provider | Existing BasicRun environment later | None |
| Tasker | Tasker catalog/plugin contracts | Not implemented | Optional provider | None |
| Shizuku | Shell capability metadata | Not implemented | Optional provider | None |
| Termux | Termux catalog/plugin registration | Not implemented | Optional provider | None |
| Vision/OCR/OCV/YOLO | Vision catalog/plugin contracts | Not implemented | Vision plugin provider later | None |
| ChartGraph | Chart query adapters | Not implemented | ChartGraph provider later | None |
| Custom Chrome Tab | Browser capability metadata | Not implemented | Integrations provider later | None |

## Resolution and Failure Rules

1. Planner derives required capabilities from catalog-bound commands.
2. Registry resolves providers in explicit registration order.
3. No match produces `CAPABILITY_PROVIDER_MISSING` before that command executes.
4. Provider failures retain their provider-supplied diagnostic code and canonical entity ID.
5. Provider results are typed `ExecutionValue` values; `Void`, `Null` and `false` are distinct.
6. Cancellation is visible through the context token and checked by the runner at controlled boundaries.
7. Provider calls and results are traceable by capability, provider ID and workflow entity.

M5-2 still contains no global provider singleton. The productive `AndroidRuntimeBridge` constructs
one explicit registry containing the native timing and native coordinate-touch providers. The touch
gateway reuses the already connected `VisualTaskerAccessibilityService`; it does not activate that
service and fails closed when Android has not connected it. All other capabilities continue to fail
closed until a later, separately approved provider slice.
