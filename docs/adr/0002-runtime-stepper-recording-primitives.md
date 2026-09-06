# ADR 0002: Runtime Stepper And Recording Primitives

## Status

Accepted

## Context

VisualTasker Studio WSS must not copy surface features from Blockly, Automate,
FRep2, or Tasker when the underlying principle can be modeled more generally.

The current architecture already distinguishes:

- workflow intent
- editor projections
- flow views
- runtime traces
- Worldview evidence
- resources

The next stabilization area is runtime interaction: stepper, recording, traces,
capabilities, provider resolution, and future perception-driven waits.

## Decision

WSS treats Stepper, Recording, and Runtime as separate concepts.

```text
WorkflowDocument
  -> Runtime / Sandbox
  -> ExecutionTrace
  -> Record
  -> StepViewDocument
  -> Stepper UI
```

Recording is a process. A Record is persisted interpreted history. A Stepper is
a runtime/record projection. None of them becomes the WorkflowDocument.

## Principles

### Generalize Product Patterns

Features inspired by Blockly, Automate, FRep2, or Tasker must be translated into
VisualTasker concepts.

Examples:

- `ImageWait`, `WaitUntil`, plugin events, and element waits converge toward
  `AwaitCondition`.
- Tasker, Shizuku, native gestures, browser, Termux, and remote systems are
  providers for capabilities, not separate workflow semantics.
- Flowchart runtime states are semantic states, not hard-coded colors.

### Editor Is Not Data Model

EMScript, BlockEditor, and Flowchart are editing/projection surfaces over the
same workflow intent.

### ViewDocument Is Not WorkflowDocument

`BlockViewDocument`, `FlowViewDocument`, and future `StepViewDocument` persist
projection-specific UI state only.

### Recording Is Not Workflow Truth

```text
Recording
  -> raw events / observations
  -> Record
  -> interpretation
  -> proposal
  -> validation / approval
  -> WorkflowDocument
```

### Passive Events Need Interpretation

Raw logcat, accessibility, overlay, runtime, and system events must not become
workflow steps directly. They first pass through filtering and interpretation.

## Target Concepts

### ActiveWorkflowId

Future multi-workflow projects require a shared active workflow identity. Text,
Block, and Flow editors should switch together.

### AwaitCondition

Common runtime primitive for asynchronous conditions:

- success
- timeout
- cancel
- error
- unavailable
- uncertain

Condition sources may include Accessibility, OCR, OpenCV, YOLO, Worldview,
Tasker plugins, Android events, browser DOM, audio, and remote providers.

### OperationResult

Typed execution result contract for provider operations:

- success
- failure
- timeout
- cancelled
- denied
- unavailable
- uncertain

### ExecutionTrace

Runtime trace chain:

```text
WorkflowNode
  -> Capability
  -> Provider
  -> Operation
  -> Observation
  -> Result
```

Trace entries should carry timestamps, duration, correlation id, causation id,
provider, result, errors, evidence references, and scene references when
available.

### CaptureFrame

Shared perception frame so OCR, OpenCV, YOLO, and other providers can evaluate
the same screenshot or screen state.

### GroundingCandidate

Recording must distinguish raw coordinates from interpreted targets. Candidate
sources may include Accessibility element, OCR text, OCV match, WorldEntity,
DOM element, and raw coordinate.

### DynamicWorkflowStructure

Generic structure support for mutators and dynamic workflow constructs such as
elseif, switch/case, try/catch/finally, function parameters, variadic operators,
lists, multiswipe, fork/join, and subflows.

## Current Implementation Status

```text
WorkflowDocument              PARTIAL - implemented as WorkspaceDocument
IRGraph                       PARTIAL - semantic projection exists
FlowGraphDocument             PARTIAL - IR projection exists
FlowViewDocument              PARTIAL - layout/view save exists
EMScriptDraft                 PARTIAL - text draft save/load exists
Stepper UI                    PARTIAL - dry-run events can feed timeline
ExecutionTrace                PARTIAL - dry-run/runtime events exist
WorldviewDocument             ARCHITECTURE_READY
Record                        PLANNED
StepViewDocument              PLANNED
ActiveWorkflowId              PLANNED
AwaitCondition                PLANNED
OperationResult               PLANNED
CaptureFrame                  PLANNED
GroundingCandidate            PLANNED
```

## Minimal Implementation Slices

1. Stepper displays dry-run/runtime events as semantically labelled steps.
2. Floating overlay exposes a recording toggle and stores raw overlay session
   events as a record precursor.
3. Add a Record/StepView contract after the Stepper UI has proven the runtime
   projection.
4. Add ActiveWorkflowId before multi-script/multi-root workflows are promoted
   to first-class project features.
5. Add AwaitCondition and typed outcomes before broad provider-specific command
   expansion.

## Non-Goals

- Do not turn raw overlay events into canonical WorkflowDocument steps.
- Do not model provider names as workflow semantics.
- Do not auto-promote text drafts or recordings into workflow truth.
- Do not create a competing Stepper data model hidden in Compose.
- Do not add full multi-workflow persistence before migration rules are defined.
