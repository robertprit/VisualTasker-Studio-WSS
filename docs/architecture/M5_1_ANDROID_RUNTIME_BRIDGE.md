# M5-1 Android Runtime Bridge

## Decision

M5-1 connects the M5-0 `DeterministicExecutionRunner` to the productive Workspace Shell. The
canonical execution path is now:

`WorkspaceDocument -> WorkflowExecutionPlanner -> DeterministicExecutionRunner -> ExecutionProvider -> ExecutionTrace -> RailTrace`

The shell does not interpret workflow commands. It owns one `AndroidRuntimeBridge`, observes its
state and projects trace evidence into existing UI contracts. `WorkspaceBasicRuntime` remains
source-compatible for older tests and adapters but is no longer called by the global productive
Play/Stop controls.

## Ownership

| Component | Responsibility |
| --- | --- |
| `DeterministicExecutionRunner` | Canonical planning, validation, execution, typed result and ordered trace. |
| `AndroidRuntimeBridge` | Coroutine dispatch, one-active-run invariant, request/cancellation ownership and observable state. |
| `AndroidWaitExecutionProvider` | Native `TIMING` capability for `action.wait`/`wait`; parameter validation and cooperative cancellation. |
| `AndroidRuntimeTraceProjection` | Lossless entity-reference projection into existing `ExecutionTrace`/RailTrace transport. |
| `WorkspaceScreen` | Start/stop intent, visible state, terminal diagnostics and selection of the active canonical entity. |

No component above owns or mutates `WorkspaceDocument`. Every run receives an immutable canonical
snapshot reference and produces request-local variables, diagnostics and trace.

## Lifecycle And Threading

The shell displays `READY`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED` and `REJECTED` from the
bridge snapshot. The runner still records its complete internal transition sequence, including
validation.

- `start` atomically installs a request/token before launching work.
- A second `start` while a token is active is rejected without creating another runner job.
- Planning and execution run on `Dispatchers.Default`; the Compose/main thread is never blocked.
- `stop` requests cooperative cancellation through the existing M5-0 token.
- `close` requests cancellation and permanently rejects new work. Compose calls it on disposal.
- Terminal completion releases the active token/job, allowing a later clean start.

## Native Wait Provider

The provider advertises the catalog capability `TIMING` and accepts the canonical command ID
`action.wait` plus the source-level command name `wait`. The one argument is interpreted as
milliseconds according to the EMScript command catalog.

Durations must be numeric, finite and non-negative. Waiting occurs on the bridge worker dispatcher
in short bounded slices so cancellation is observed while the action is active. Cancellation is a
typed `ProviderResult.Cancelled`, not a provider failure, and therefore terminates the run as
`CANCELLED`.

The provider has no Tasker, Accessibility, Shizuku, OCR, Vision or AI dependency.

## Trace And UI

The runner's additive trace observer publishes events while execution is active. The bridge retains
their original monotonic indices and canonical entity/relation IDs. The existing RailTrace
projection receives these events as `LiveRun` operations. Active entity selection uses the
canonical `BlockId`; no pixel, source-line or projection-object identity is introduced.

The Workspace top bar now:

- starts the active canonical workflow through the new runner;
- disables Play while a request is active;
- enables Stop only while a request is active;
- displays the current runtime state;
- sends Stop to the cancellation token instead of merely clearing UI state.

## Acceptance

| Criterion | Status | Evidence |
| --- | --- | --- |
| AC-01 Existing ExecutionRunner used | PASS | `AndroidRuntimeBridge` owns `DeterministicExecutionRunner`; global Play calls the bridge. |
| AC-02 Native wait capability | PASS | `AndroidWaitExecutionProvider` and focused tests. |
| AC-03 Start/stop/cancellation | PASS | Bridge state/token tests and device smoke. |
| AC-04 State visible in shell | PASS | Workspace top-bar state label and enabled-state binding. |
| AC-05 Existing diagnostics/RailTrace | PASS | `AndroidRuntimeTraceProjection` and live Run rail. |
| AC-06 No parallel runtime/provider | PASS | Productive Play/Stop has one bridge and one explicit provider registry. |
| AC-07 Lifecycle/error safety | PASS | Duplicate start, close, invalid argument and missing capability tests. |
| AC-08 JVM/Android integration | PASS | Runtime bridge/provider tests and device smoke gate. |
| AC-09 M4 conformance retained | PASS | Central harness repeated three times in the final gate. |
| AC-10 Build/diff/device | PASS | Recorded by the M5-1 completion commit/report. |

## Bounded Limits

- M5-1 intentionally registers only the timing provider. Any other command fails with the existing
  `CAPABILITY_PROVIDER_MISSING` diagnostic.
- Pause remains outside the execution contract; Stop is the supported interruption mechanism.
- Runtime trace is process-local and not yet persisted as a resumable execution session.
- The legacy BasicRun implementation remains for compatibility tests and later provider extraction,
  but is disconnected from productive global execution controls.
- M5-2 provider expansion is not part of this slice.
