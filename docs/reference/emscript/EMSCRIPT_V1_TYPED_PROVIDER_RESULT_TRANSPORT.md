# EMScript v1 Typed Provider Result Transport

Stand: 2026-09-28
Phase: M1B-3R
Status: READY

## Contract

Provider execution outcome and language return value are independent axes.
The existing `RuntimeAdapterResult` represents them additively:

- `success`: whether the adapter operation completed successfully;
- `value: EmscriptValue?`: a present typed language result, or no value;
- `diagnosticCode`: the structured reason for failure;
- `message`: human-readable trace text only.

The transport supports these distinct states:

| State | success | value | diagnostic |
| --- | ---: | --- | --- |
| successful Void operation | true | no value | none |
| successful Boolean true | true | `BooleanValue(true)` | none |
| successful Boolean false | true | `BooleanValue(false)` | none |
| successful nullable absence | true | `NullValue` | none |
| failed operation | false | no value | required by the migrated contract |

Failure MUST NOT carry a language value. `message` MUST NOT be parsed as a
machine-readable result. In particular, returned Boolean false MUST NOT change
execution success to false.

## Compatibility

The existing three-argument `RuntimeAdapterResult(success, message, warning)`
shape remains valid for established Void/status callers. The new payload and
diagnostic fields have defaults, so no broad provider migration is required.
Factory functions make new typed-success, Void-success and structured-failure
call sites explicit.

## M1B-3R Migration Boundary

Only `chromeTab.isSupported(): Bool` consumes this transport in M1B-3R. A
completed Android Custom Tabs service query returns successful
`BooleanValue(true)` or successful `BooleanValue(false)`. Missing host adapter
and technical service-resolution failure use `CHROME_TAB_ADAPTER_UNAVAILABLE`
and `CHROME_TAB_RESOLUTION_FAILED` respectively.

No Tasker, Shizuku, Termux, scrcpy or other provider query is migrated by this
contract. Existing nullable core queries continue to use `NullValue` for
legitimate absence and structured failure for technical failure.
