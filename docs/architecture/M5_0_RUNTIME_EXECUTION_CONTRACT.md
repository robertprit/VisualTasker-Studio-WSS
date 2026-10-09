# M5-0 Runtime Execution Contract

## Decision

M5-0 establishes a deterministic, JVM-testable execution baseline for canonical VisualTasker
workflows. `WorkspaceDocument`/`WorkflowGraph` remains the semantic input. `ExecutionPlan` is an
immutable validated snapshot, not another persisted workflow model and not another source of truth.

The new runner is not wired into Android UI controls in this slice. Existing DryRun and BasicRun
entry points remain operational while later slices migrate their orchestration to this contract.

## Existing Runtime Inventory

| Component | Existing responsibility | M5-0 use |
| --- | --- | --- |
| `WorkspaceDryRunRuntime` | Direct graph interpretation, variables, expressions, branch/loop simulation and dry-run events. | Semantics and supported graph forms were retained as the baseline; no Android dependency added. |
| `EmscriptDryRunRuntime` | Parser-IR dry-run for EMScript source. | Existing parser expression model is reused for legacy inline assignment expressions. |
| `WorkspaceBasicRuntime` | Capability gate followed by Android/environment effects. | Left unchanged; Android adapters are outside M5-0. |
| `RuntimeCapabilityGate` | Catalog-driven readiness and adapter diagnostics. | Existing capability descriptors inform the provider boundary; productive gate remains unchanged. |
| `ExecutionTrace` / RailTrace projection | Stable trace transport for current DryRun/BasicRun UI. | Left unchanged; M5-0 adds a lower-level typed execution trace without new timeline UI. |
| `Validator` | Canonical graph validity, type, connectivity, reachability and cycle checks. | Mandatory plan gate; invalid graphs never execute. |
| `VisualTaskerCommandCatalog` | Command names, arguments, return types, capabilities, plugin ownership and runtime metadata. | Command/provider plan compilation and capability naming. |
| `WorkspaceGraph` | Deterministic roots, chains, statement slots and value connections. | Sole graph traversal API used by the planner. |
| `IrGenerator` / `IrGraphGenerator` | Editor/generator projections. | Audited but not used as runtime truth: statement IR lacks canonical entity references; IR graph lacks complete executable expression payloads. |

Existing Android integrations include Accessibility, Tasker, Shizuku, Termux, Custom Tabs, Vision
and recording adapters. None is a dependency of the M5-0 core. Tasker remains an optional future
provider.

## Execution Contract

Production contracts live in `com.visualtasker.wss.emscript.runtime`:

- `ExecutionRequest`: immutable request, canonical snapshot, optional explicit entry and inputs.
- `ExecutionPlan`: source revision, selected canonical entry, ordered steps and capabilities.
- `ExecutionContext`: request-local variables, workflow identity and cancellation token.
- `ExecutionStep`: assignment, invocation, branch, repeat and while structures.
- `ExecutionResult`: terminal state, typed result, variables, diagnostics and trace.
- `ExecutionDiagnostic`: stable code, message and optional canonical entity reference.
- `ExecutionTraceEvent`: ordered state, step, branch, provider, failure and cancellation evidence.
- `CancellationToken`: read-only runner boundary; `MutableCancellationToken` is the test/basic host implementation.

The lifecycle is strictly:

`CREATED -> VALIDATING -> READY -> RUNNING -> COMPLETED`

Terminal alternatives are `REJECTED` from validation, `CANCELLED` from ready/running, and `FAILED`
from running. `ExecutionLifecycle` rejects every transition outside this table.

`ExecutionValue.VoidValue`, `ExecutionValue.NullValue`, and
`ExecutionValue.BooleanValue(false)` are distinct values.

## Planning

`WorkflowExecutionPlanner` performs these steps without mutating the source document:

1. Run the shared semantic `Validator`.
2. Resolve `event.start` roots using deterministic `WorkspaceGraph` ordering.
3. Require an explicit entry for multiple roots; preserve multi-root architecture without implicit orchestration.
4. Compile sequence chains, statement slots, expressions and variables to immutable plan structures.
5. Bind command blocks through `VisualTaskerCommandCatalog`.
6. Reject unsupported blocks, void expressions, malformed branches and unresolved entry points.
7. Collect required capabilities for deterministic provider resolution.

Canonical `BlockId.value` is retained on every executable step and expression. Branch relation IDs
use the existing stable `branch:<owner-id>:<slot>` convention. No identity is derived from render
order, source line, offset or pixel position.

## Supported M5-0 Constructs

- Linear statement chains from an `event.start` root.
- LET/SET variable assignment and reads.
- Boolean, number and string literals.
- Arithmetic and comparison operators already normalized by the shared operator contract.
- AND/OR expressions.
- IF, ELSEIF and ELSE statement slots.
- REPEAT and WHILE with deterministic safety limits.
- Command statements and command reporter expressions declared by the command catalog.
- Typed fake/provider results, missing-capability diagnostics and provider failures.

Region/list/domain values remain provider-extensible but are not widened in this baseline contract.
Unsupported or opaque constructs are rejected rather than simulated.

## Runner and Provider Boundary

`DeterministicExecutionRunner` always plans and validates before entering `RUNNING`. It executes an
ordered immutable plan, keeps all variables in request-local `ExecutionContext`, checks cancellation
before/after steps and provider calls, and enforces step/loop limits.

An `ExecutionCapability` names required behavior. An `ExecutionProvider` advertises capabilities and
receives a typed `ProviderInvocation` plus execution context. The registry selects the first
registered matching provider, making host priority explicit and reproducible. Missing capability and
provider failure produce typed diagnostics. No provider can mutate the canonical workflow through
this API.

## Trace Contract

Trace events are monotonically indexed and cover:

- lifecycle changes and execution start/end;
- step start/end;
- selected branch relation;
- provider start/result including provider and capability;
- diagnostics and cancellation.

Every step/provider/branch event carries its canonical entity or relation ID. Trace collection is
request-local and has no workspace mutation API.

## Acceptance Evidence

| AC | Status | Evidence |
| --- | --- | --- |
| AC-01 Existing runtime/VM inventoried | PASS | Inventory above and source audit. |
| AC-02 Unified execution contract | PASS | `ExecutionContract.kt`, lifecycle test. |
| AC-03 Deterministic graph-to-plan | PASS | Planner and order/determinism tests. |
| AC-04 IF/expression/variable semantics | PASS | True, false, ELSEIF, nested expression and LET/SET tests. |
| AC-05 JVM runner | PASS | `DeterministicExecutionRunnerTest`, 18 tests. |
| AC-06 Capability/provider separation | PASS | Capability value, provider interface and deterministic registry. |
| AC-07 Missing/error provider handling | PASS | Typed missing-capability and provider-failure tests. |
| AC-08 Cancellation consistency | PASS | Before-run and provider-time cancellation tests. |
| AC-09 Canonical trace references | PASS | Trace entity/reference test. |
| AC-10 Invalid/unsupported rejection | PASS | Validator and unsupported construct tests. |
| AC-11 M4 conformance retained | PASS | Central M4 harness repeated three times in final gate. |
| AC-12 Tests/build/diff checks | PASS | Final gate recorded in the M5-0 completion report/commit. |

## Known Limits

- Existing UI DryRun/BasicRun orchestration is not migrated in M5-0.
- Android/Accessibility/Tasker/Shizuku/Termux/Vision providers are deliberately absent.
- Provider resolution uses registration order; policy/scoring is future work.
- Function calls, FOR/BREAK/CONTINUE, TRY/CATCH/FINALLY/THROW and new scopes are unsupported.
- Multi-root orchestration is not implemented; callers must select one root when ambiguous.
- Runtime context and trace are process-local and non-persistent in this slice.

These are bounded M5-0 product limits, not alternate semantics.
