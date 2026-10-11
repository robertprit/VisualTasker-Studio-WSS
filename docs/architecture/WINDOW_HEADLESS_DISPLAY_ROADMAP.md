# Window, Headless And Display Architecture Roadmap

Stand: 2026-10-11

## Scope

This document records the current implementation and the contracts for M5-H, M6-W, M7-V and M9-D.
It is planning and audit evidence only. No production implementation is implied.

## Current Inventory

| Area | Status | Existing implementation | Missing contract |
| --- | --- | --- | --- |
| Runtime core | DONE | `DeterministicExecutionRunner` is UI-independent and accepts an explicit provider registry and cancellation token. | None for the core runner. |
| Android runtime bridge | DONE | `AndroidRuntimeBridge` rejects double starts, exposes `StateFlow`, supports cancellation and closes its active run. | Its ownership is currently tied to a Compose coroutine scope. |
| Headless host | GAP | No application/service-owned runtime host exists. | One process-wide owner, foreground/background lifecycle, recovery and UI reattachment. |
| Runtime recovery | GAP | `AndroidRuntimeSnapshot` and live trace are in memory and projected in `WorkspaceScreen`. | Bounded persistent state, interrupted-session diagnosis and no automatic hazardous resume. |
| Compose workspace panels | DONE | `PanelState`, `WorkspaceSessionStore` and `WorkspaceScreen` provide position, size, Z-order, minimize/maximize, lock, focus, snap and restoration. | Display identity and a reusable WindowSession contract are absent. |
| Native floating overlays | DONE | `StudioOverlayService` uses `TYPE_APPLICATION_OVERLAY`, drag/resize, visibility restoration and `OverlayStateStore`. | It is a separate View-based host with no shared session, docking, profile or display contract. |
| Overlay service lifecycle | GAP | Service is `START_STICKY` and launched with `startService`. | No foreground notification/type, explicit recovery policy or target-36 background-start contract. |
| Recording display metadata | GAP | `WindowSnapshot` and `WindowContext` contain nullable `displayId`. | Capture and target resolution do not consistently populate or enforce it. |
| Screenshot capture | GAP | Accessibility screenshot and `AndroidRecorderCaptureSource` work on `Display.DEFAULT_DISPLAY`. | Explicit display selection, per-display metrics and capability failure. |
| Gesture dispatch | GAP | Accessibility gestures validate physical bounds and support typed single/multi-pointer paths. | `GestureDescription.Builder.setDisplayId` is not used; display ID is absent from provider requests and trace. |
| Virtual display | PLANNED | No `VirtualDisplay`, `MediaProjection` or display-surface owner exists. | M7-V feasibility contract and owned test surface. |
| VT2VT | DONE | Peer, transport, pairing, workspace/runtime mirror and reconnect foundations exist. | No DisplaySession registry or remote-surface contract. |

The inventory establishes that M5-H and M6-W are consolidation slices, not greenfield replacements.
M7-V is new exploratory infrastructure. M9-D is deliberately later integration work.

## Shared Invariants

1. `WorkflowGraph` remains the canonical intent truth.
2. `DeterministicExecutionRunner` remains the only execution engine.
3. A runtime host owns lifecycle; an Activity, panel or overlay only observes and commands it.
4. Window management owns presentation state, never workflow semantics or runtime decisions.
5. Headless means UI-independent, not permission-independent or automatically restartable.
6. Every gesture, capture and visual target request identifies one `DisplayContext`.
7. Display, screenshot/image and Workspace Canvas coordinates are distinct coordinate spaces.
8. Physical, virtual and remote displays have separate capability and permission profiles.
9. Missing, stale or unsupported display context fails diagnostically; no default-display fallback.
10. Virtual display means a logical display rendering into an owned `Surface`, not Android
    virtualization and not permission to control arbitrary third-party apps.

## M5-H Headless Execution Contract

Status: `PLANNED`, Stable 1.0 Core.

### Required Ownership

- One process-wide `HeadlessExecutionSession` owns the existing runner, provider registry,
  cancellation token and active request.
- Compose, notifications, overlays, Tasker and VT2VT submit commands to that owner; none may create
  a parallel runtime.
- Start is idempotent by request/workflow identity. A second incompatible start is rejected.
- Stop and cancellation are explicit and remain observable after the UI disconnects.
- The host records a bounded status/trace checkpoint sufficient for UI reattachment and diagnosis.
- Process death marks an active request `INTERRUPTED`. It never resumes gestures or other hazardous
  actions automatically.
- Recovery can offer an explicit user-approved new execution referencing the interrupted run.

### Android Host Gate

- Classify the actual long-running work against Android foreground-service types before coding.
- A target-36 foreground service must declare the justified type and permissions and show a user-
  visible notification. A generic hidden background service is not acceptable.
- Start must originate from an allowed user-visible or documented exception path; background start
  restrictions are part of the contract.
- Notification actions provide Stop and Return-to-Studio. System/user service termination produces
  a terminal diagnostic and resource cleanup.

Gate: an approved workflow continues without a visible Studio Activity, remains observable through
a notification, can be stopped, and reconnects to the UI without creating another runner.

## M6-W Window And Floating Workspace Manager

Status: `PLANNED`, Stable 1.0 Core.

### Reuse

- Preserve `PanelState` and `WorkspaceSessionStore` for Compose workspace layouts.
- Preserve `StudioOverlayService` and `OverlayStateStore` as the current native overlay adapter.
- Extract a neutral `WindowSession` identity and geometry contract instead of merging workflow and
  presentation models.

### Contract

- Session identity includes host kind, display context, bounds, Z-order, focus/input policy,
  minimized/maximized state and layout profile.
- Compose panels, bottom sheets, rails, dockable toolbars and native overlays share geometry and
  persistence semantics while retaining host-specific rendering.
- Drag, resize, snap, dock and auto-arrange use one display-aware coordinate mapper.
- Focusable input surfaces and touch-passthrough overlays declare different policies explicitly.
- Workspace content remains native Compose/Android UI; a WebView is not a substitute panel host.
- Rotation, density, insets, cutouts and display removal trigger deterministic clamp/reflow.
- Layout changes never create a canonical workflow revision or runtime operation.
- WindowManager permission and add/update/remove failures are typed and recoverable.

Gate: windows can move, resize, dock, restore and survive orientation/profile changes without
mutating workflow semantics or runtime state.

## M7-V VirtualDisplay And Capture Foundation

Status: `PLANNED`, optional for Stable 1.0.

### Feasibility Slice

- Define `DisplayContext(id, kind, widthPx, heightPx, densityDpi, rotation, generation)` and a
  `DisplayCoordinateMapper` before adding capture or input.
- Inventory physical displays through `DisplayManager`; represent virtual displays only when owned
  by an explicit `DisplaySession`.
- Create one private, non-secure virtual display backed by an app-owned `Surface`.
- Launch only a WSS-owned test Activity where the platform permits it.
- Capture or observe that surface, verify rotation/density mapping and release display, surface and
  callbacks deterministically.
- Probe Accessibility screenshot, tree and gesture capability per display. Unsupported combinations
  remain capability failures, not emulated success.
- Keep direct app-owned VirtualDisplay rendering separate from MediaProjection screen capture.

### Safety Boundaries

- Protected/secure content must remain unavailable or blank unless the platform grants a capability
  that ordinary third-party apps generally do not possess.
- MediaProjection requires explicit user consent per session and its own foreground-service type.
- A MediaProjection token/session is not persisted or silently reused after stop/process death.
- No claim that arbitrary foreign apps can be launched, inspected or controlled on a private
  virtual display.

Gate: a WSS-owned test surface is created on a supported virtual display, observed with correct
display metadata, optionally targeted only when supported, and fully released without leaks.

## M9-D Display Sessions And Remote Surfaces

Status: `DEFERRED` until M7-V and the M9-5 VT2VT scope are accepted.

- A `DisplaySession` registry distinguishes local physical, local virtual and remote VT2VT targets.
- Capability selection is per session: render, capture, accessibility tree, gesture, remote input,
  remote execution and mirroring are independent flags.
- Remote rendering, WorkspaceOperation synchronization and remote runtime dispatch remain separate
  protocols and failure domains.
- Reconnect creates a new generation and invalidates stale coordinate mappings and permissions.
- No virtual display is described as a virtual Android instance.

Gate: a session can disconnect/reconnect or lose permission without redirecting capture/input to
another display or corrupting workspace/runtime state.

## Cross-Slice Pebbles

- Display ID in gesture, semantic target, capture and trace contracts.
- `DisplayCoordinateMapper` with explicit source and target spaces.
- `WindowSession`, `DisplaySession` and `HeadlessExecutionSession` identities.
- Overlay touch routing, passthrough, Z-order and focus rules.
- MultiDisplay trace metadata without copying full frame payloads.
- Headless status reattachment and bounded trace retention.
- VirtualDisplay callback, surface and projection cleanup.
- Capability/permission matrix per display and per session generation.
- Android API-, foreground-service- and permission-compatibility tests.

## Android Platform Constraints

The project currently targets API 36 with minimum API 29. The following constraints are therefore
release requirements, not optional compatibility notes:

- Android 12+ restricts foreground-service starts from the background. Android 14+ checks declared
  foreground-service types and matching permissions when the service starts.
- Android 15+ only grants the `SYSTEM_ALERT_WINDOW` background-start exemption while an overlay is
  actually visible. Overlay permission alone is insufficient.
- `TYPE_APPLICATION_OVERLAY` requires explicit user grant and remains below critical system windows;
  Android may adjust its position, size or visibility.
- Accessibility gesture dispatch can identify a logical display through
  `GestureDescription.Builder.setDisplayId` from API 30, but support must still be probed.
- Accessibility screenshot capture accepts a display ID from API 30; current WSS code passes only
  `Display.DEFAULT_DISPLAY`.
- A VirtualDisplay renders into an app-supplied `Surface` and must be explicitly released. Private,
  public, presentation, auto-mirror and secure flags have different permissions.
- Non-secure virtual displays blank secure windows. Creating a secure display requires a privileged
  permission unavailable to ordinary third-party apps.
- Launching an Activity on a secondary display is device- and display-policy-dependent; private or
  invalid displays can reject it, and devices may lack the secondary-display activity feature.
- Android 14+ MediaProjection requires fresh user consent for each session and does not permit reuse
  of one token/instance for multiple `createVirtualDisplay()` calls. Stop callbacks must release the
  virtual display and surface.

Primary platform references:

- <https://developer.android.com/develop/background-work/services/fgs/launch>
- <https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start>
- <https://developer.android.com/develop/background-work/services/fgs/service-types>
- <https://developer.android.com/reference/android/view/WindowManager.LayoutParams>
- <https://developer.android.com/reference/android/accessibilityservice/GestureDescription.Builder>
- <https://developer.android.com/reference/android/accessibilityservice/AccessibilityService>
- <https://developer.android.com/reference/android/hardware/display/DisplayManager>
- <https://developer.android.com/reference/android/hardware/display/VirtualDisplay>
- <https://developer.android.com/media/grow/media-projection>
- <https://developer.android.com/reference/android/app/ActivityOptions#setLaunchDisplayId(int)>

## Release Priority And Dependencies

| Slice | Release priority | Dependencies | Blocks |
| --- | --- | --- | --- |
| M5-H | 1.0 Core | M5-8 provider policy, existing runner/trace | M5-10 Runtime Closure |
| M6-W | 1.0 Core | M6-0 inventory, current panel/overlay persistence | M6-7 editor/product gate |
| M7-V | 1.0 Optional | M6-W display context, M7-1 capture contract | Only selected MultiDisplay scope |
| M9-D | Post-1.0 / experimental | M7-V, M9-5 VT2VT, M5-H | No 1.0 core gate |
| Virtual app sessions | Post-1.0 / experimental | M7-V proof plus explicit platform/policy review | No 1.0 gate |

## Smallest Future Slices

1. M5-H contract test: replace Compose ownership with a fake process host around the same runner.
2. M5-H Android host: notification, start/stop, interruption record and UI reattachment.
3. M6-W neutral `WindowSession` projection over existing Compose panel and overlay placement models.
4. M6-W display-aware clamp/reflow tests without changing rendering.
5. M7-V feasibility spike only after M7-1: app-owned surface, WSS test Activity and complete cleanup.

No M7-V or M9-D production implementation starts from this roadmap update.
