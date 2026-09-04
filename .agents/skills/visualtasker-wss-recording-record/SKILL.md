---
name: visualtasker-wss-recording-record
description: Guides recording and record architecture in VisualTasker Studio WSS. Use for StepRecorder, Timeline, replay, watchdog, event capture, interpretation, persisted Records, scene transitions, workflow proposals, and the boundary between observed user behavior and accepted automation intent.
---

# VisualTasker WSS Recording And Record

## Purpose

Keep active recording separate from persisted records and accepted workflows.

```text
Recording = active observation process
Record = persisted structured result
Workflow = accepted intent
```

## Invariants

- Recording captures raw events and observations first.
- A Record preserves what happened, what was observed, and what was
  interpreted as separate facts.
- Demonstrated user behavior is not automatically accepted Workflow truth.
- Recording may produce workflow proposals; proposals require validation and
  acceptance before becoming workflow mutations.
- Replay uses accepted Workflow/Runtime contracts, not raw recording events as
  hidden commands.

## Condensation Path

```text
Observations
  -> Events
  -> Steps
  -> Scenes
  -> Record
  -> WorkflowProposal
```

## Modes

- `RECORD`: capture a user-guided episode.
- `WATCHDOG`: observe and diagnose deviations over time.
- `REPLAY`: compare expected workflow behavior with runtime evidence.
