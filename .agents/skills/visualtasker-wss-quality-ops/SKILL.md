---
name: visualtasker-wss-quality-ops
description: Runs VisualTasker Studio WSS validation. Use before finalizing code changes, Android changes, plugin contract changes, or migration slices.
disable-model-invocation: true
---

# VisualTasker WSS Quality Ops

## Standard Validation

Run focused tests for changed modules first.

For broad WSS app validation:

```bash
./gradlew :visualtasker-blockeditor:blockeditor-registry:test \
  :visualtasker-blockeditor:blockeditor-compose:compileDebugKotlin \
  :app:testDebugUnitTest \
  :app:compileDebugKotlin \
  --console=plain
```

When Android behavior changed and a device is connected:

```bash
./gradlew :app:installDebug --console=plain
adb shell am start -n com.visualtasker.wss/.MainActivity
adb shell dumpsys activity activities | rg -n "topResumedActivity|ResumedActivity"
```

## Reporting

Report:

- focused tests
- broad compile/test
- install result
- foreground activity if launched
- anything not verified physically on the device
