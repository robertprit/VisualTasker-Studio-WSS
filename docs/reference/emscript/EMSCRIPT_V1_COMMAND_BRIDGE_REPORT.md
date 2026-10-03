# EMScript v1 Command Bridge Report

Stand: 2026-10-03
Phase: M1B-3Y Perception Domain Value Convergence
Status: twenty-one queries migrated; two chart query return conflicts remain

## Inventory

| Metric | Count |
| --- | ---: |
| Catalog entries | 127 |
| Unique canonical names, case-insensitive | 124 |
| Declared aliases | 89 |
| Alias/canonical collisions | 1 |
| Provider-owned entries | 89 |
| Query projections | 26 |
| Native V1 definitions | 3 |
| CLEAN | 22 |
| NORMALIZABLE | 68 |
| CONFLICT | 37 |
| UNMAPPABLE | 0 |
| Live status LIVE_CONFIRMED | 29 |
| Live status LIVE_PROVIDER_DEPENDENT | 74 |
| Live status DRY_RUN_ONLY | 1 |
| Live status CATALOG_ONLY | 3 |
| Live status NO_DISPATCH | 20 |
| Live status UNKNOWN | 0 |

## Bridge Contract

Each legacy entry is analyzed exactly once. `event.start`, `action.wait` and `feedback.beep` use native V1 definitions and verify their generated legacy compatibility entries through one shared adapter. Every other generated `CommandDefinition` remains a migration proposal. Typed issues carry the reason and repository evidence. M1B-3H-A adds read-only structural classification for legacy `input.touch`; it does not alter parser, Workspace, IR, source serialization or runtime dispatch.

## Complete Entry Matrix

| Legacy ID | Current name | Proposed name | Class | Return | Naming | Type | Expression | Overall | Issues |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `event.start` | `onStart` | `onStart` | EVENT | Void/unspecified -> Void | CANONICAL | CANONICAL | NOT_APPLICABLE | CLEAN | none |
| `action.wait` | `wait` | `wait` | ACTION | Void/unspecified -> Void | CANONICAL | CANONICAL | NOT_APPLICABLE | CLEAN | none |
| `action.clickText` | `click` | `clickText` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | CONFLICT | LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>SIGNATURE_CONFLICT |
| `action.findTemplate` | `findTemplate` | `templateFind` | QUERY | ImageMatch? -> legacy.imageMatch? | NEEDS_NORMALIZATION | CONFLICT | LOSSLESS | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>DEFAULT_SOURCE_CONFLICT<br>DEFAULT_SOURCE_CONFLICT<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>PLUGIN_OWNER_NOT_PROVIDER |
| `action.swipe` | `swipe` | `swipe` | ACTION | Void/unspecified -> Void | CANONICAL | CONFLICT | NOT_APPLICABLE | NORMALIZABLE | UNTYPED_DEFAULT<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER<br>UNTYPED_PARAMETER |
| `input.clickPoint` | `clickPoint` | `clickPoint` | ACTION | Void/unspecified -> Void | CANONICAL | CANONICAL | NOT_APPLICABLE | CLEAN | none |
| `input.touch` | `touch` | `touch` | ACTION | Void/unspecified -> Void | CANONICAL | CONTRACT_DECIDED | NOT_APPLICABLE | NORMALIZABLE | UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `vision.screenshot` | `screenshot` | `screenshot` | ACTION | Void/unspecified -> Void | CANONICAL | CANONICAL | NOT_APPLICABLE | CLEAN | none |
| `vision.ocr` | `ocr` | `ocr` | ACTION | Void/unspecified -> Void | CANONICAL | CONFLICT | NOT_APPLICABLE | CONFLICT | DEFAULT_SOURCE_CONFLICT<br>LIVE_FLAG_WITHOUT_DISPATCH |
| `vision.findText` | `findText` | `findText` | QUERY | TextMatch? -> legacy.textMatch? | CANONICAL | CANONICAL | LOSSLESS | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>PLUGIN_OWNER_NOT_PROVIDER |
| `vision.highlight` | `highlight` | `highlight` | ACTION | Void/unspecified -> Void | CANONICAL | CONFLICT | NOT_APPLICABLE | CONFLICT | DEFAULT_SOURCE_CONFLICT<br>LIVE_FLAG_WITHOUT_DISPATCH |
| `vision.markerSave` | `markerSave` | `markerSave` | ACTION | Void/unspecified -> Void | CANONICAL | CONFLICT | NOT_APPLICABLE | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>DEFAULT_SOURCE_CONFLICT<br>PLUGIN_OWNER_NOT_PROVIDER |
| `vision.markerLoad` | `markerLoad` | `markerLoad` | QUERY | Marker? -> legacy.marker? | CANONICAL | CANONICAL | LOSSLESS | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>PLUGIN_OWNER_NOT_PROVIDER |
| `vision.markerDelete` | `markerDelete` | `markerDelete` | ACTION | Void/unspecified -> Void | CANONICAL | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>PLUGIN_OWNER_NOT_PROVIDER |
| `vision.templateDefine` | `templateDefine` | `templateDefine` | ACTION | Void/unspecified -> Void | CANONICAL | CONFLICT | NOT_APPLICABLE | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>DEFAULT_SOURCE_CONFLICT<br>PLUGIN_OWNER_NOT_PROVIDER |
| `vision.templateCompare` | `templateCompare` | `templateCompare` | QUERY | Number -> Number | CANONICAL | CONFLICT | LOSSLESS | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>DEFAULT_SOURCE_CONFLICT<br>PLUGIN_OWNER_NOT_PROVIDER |
| `scene.save` | `sceneSave` | `sceneSave` | ACTION | Void/unspecified -> Void | CANONICAL | CONFLICT | NOT_APPLICABLE | CONFLICT | DEFAULT_SOURCE_CONFLICT<br>LIVE_FLAG_WITHOUT_DISPATCH |
| `system.datastorePut` | `datastorePut` | `datastorePut` | ACTION | Void/unspecified -> Void | CANONICAL | RESOLVED | LOSSLESS | CLEAN | none |
| `system.datastoreGet` | `datastoreGet` | `datastoreGet` | QUERY | String? -> String? | CANONICAL | CANONICAL | LOSSLESS | CLEAN | none |
| `feedback.beep` | `beep` | `beep` | ACTION | Void/unspecified -> Void | CANONICAL | CANONICAL | NOT_APPLICABLE | CLEAN | none |
| `feedback.vibrate` | `vibrate` | `vibrate` | ACTION | Void/unspecified -> Void | CANONICAL | RESOLVED | LOSSLESS | CLEAN | none |
| `debug.log` | `log` | `log` | ACTION | Void/unspecified -> Void | CANONICAL | RESOLVED | LOSSLESS | CLEAN | none |
| `file.readText` | `File.readText` | `file.readText` | QUERY | String? -> String? | NEEDS_NORMALIZATION | CANONICAL | LOSSLESS | NORMALIZABLE | LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME |
| `file.writeText` | `file.writeText` | `file.writeText` | ACTION | Void/unspecified -> Void | RESOLVED_WITH_LEGACY_ALIAS | CANONICAL | NOT_APPLICABLE | CLEAN | none |
| `clipboard.get` | `Clipboard.get` | `clipboard.get` | QUERY | String -> String | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME |
| `clipboard.set` | `clipboard.set` | `clipboard.set` | ACTION | Void/unspecified -> Void | RESOLVED_WITH_LEGACY_ALIAS | CANONICAL | NOT_APPLICABLE | CLEAN | none |
| `cache.clear` | `cache.clear` | `cache.clear` | ACTION | Void/unspecified -> Void | RESOLVED_WITH_LEGACY_ALIAS | CANONICAL | NOT_APPLICABLE | CLEAN | none |
| `system.info` | `Sys.info` | `sys.info` | QUERY | String -> String | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME |
| `system.env` | `Env.get` | `env.get` | QUERY | String -> String | NEEDS_NORMALIZATION | CANONICAL | LOSSLESS | NORMALIZABLE | LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME |
| `chromeTab.open` | `ChromeTab.open` | `chromeTab.open` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | NORMALIZABLE | LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `chromeTab.close` | `ChromeTab.unbind` | `chromeTab.unbind` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.runTask` | `Tasker.runTask` | `tasker.runTask` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `tasker.setVariable` | `Tasker.setVariable` | `tasker.setVariable` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>DEFAULT_SOURCE_CONFLICT<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `tasker.emitEvent` | `Tasker.emitEvent` | `tasker.emitEvent` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.exec` | `Shizuku.exec` | `shizuku.exec` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `shizuku.shell` | `Shizuku.shell` | `shizuku.shell` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `termux.run` | `Termux.run` | `termux.run` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `termux.shell` | `Termux.shell` | `termux.shell` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `termux.api` | `Termux.api` | `termux.api` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.start` | `Scrcpy.start` | `scrcpy.start` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.stop` | `Scrcpy.stop` | `scrcpy.stop` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `scrcpy.touch` | `Scrcpy.touch` | `scrcpy.touch` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER<br>UNTYPED_PARAMETER |
| `chart.create` | `Chart.create` | `chart.create` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>LIVE_FLAG_WITHOUT_DISPATCH<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `chart.show` | `Chart.show` | `chart.show` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>LIVE_FLAG_WITHOUT_DISPATCH<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `chart.export` | `Chart.export` | `chart.export` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>LIVE_FLAG_WITHOUT_DISPATCH<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `variable.set` | `set` | `set` | ACTION | Void/unspecified -> Void | CANONICAL | RESOLVED | LOSSLESS | CLEAN | none |
| `variable.get` | `get` | `get` | QUERY | Any -> Any | CANONICAL | RESOLVED | NOT_APPLICABLE | CLEAN | none |
| `control.repeat` | `repeat` | `repeat` | CONTROL | Void/unspecified -> Void | CANONICAL | CANONICAL | NOT_APPLICABLE | CLEAN | none |
| `control.while` | `while` | `while` | CONTROL | Void/unspecified -> Void | CANONICAL | CANONICAL | CONFLICT | CLEAN | none |
| `control.if` | `if` | `if` | CONTROL | Void/unspecified -> Void | CONFLICT | CANONICAL | CONFLICT | CONFLICT | AMBIGUOUS_ALIAS<br>CONTROL_DUPLICATE<br>DUPLICATE_CANONICAL_NAME |
| `control.ifElse` | `if` | `if` | CONTROL | Void/unspecified -> Void | CONFLICT | CANONICAL | CONFLICT | CONFLICT | AMBIGUOUS_ALIAS<br>CONTROL_DUPLICATE<br>DUPLICATE_CANONICAL_NAME |
| `control.ifElseIfElse` | `if` | `if` | CONTROL | Void/unspecified -> Void | CONFLICT | CANONICAL | CONFLICT | CONFLICT | AMBIGUOUS_ALIAS<br>CONTROL_DUPLICATE<br>DUPLICATE_CANONICAL_NAME |
| `logic.screenContains` | `screenContains` | `screenContains` | QUERY | Boolean -> Bool | CANONICAL | CANONICAL | NOT_APPLICABLE | CLEAN | none |
| `logic.boolean` | `boolean` | `boolean` | QUERY | Boolean -> Bool | CANONICAL | RESOLVED | NOT_APPLICABLE | CLEAN | none |
| `logic.and` | `and` | `and` | EXPRESSION | Boolean -> Bool | CANONICAL | CANONICAL | LOSSLESS | CONFLICT | OPERATOR_OR_LITERAL_MODEL |
| `logic.or` | `or` | `or` | EXPRESSION | Boolean -> Bool | CANONICAL | CANONICAL | LOSSLESS | CONFLICT | OPERATOR_OR_LITERAL_MODEL |
| `logic.operate` | `operate` | `operate` | EXPRESSION | Any -> Any | CANONICAL | CANONICAL | LOSSLESS | CLEAN | none |
| `logic.compare` | `compare` | `compare` | EXPRESSION | Boolean -> Bool | CANONICAL | CANONICAL | LOSSLESS | CLEAN | none |
| `literal.number` | `number` | `number` | VALUE | Number -> Number | CANONICAL | CANONICAL | NOT_APPLICABLE | CONFLICT | OPERATOR_OR_LITERAL_MODEL |
| `literal.string` | `string` | `string` | VALUE | Text -> String | CANONICAL | CANONICAL | NOT_APPLICABLE | CLEAN | none |
| `literal.boolean` | `boolean` | `boolean` | VALUE | Boolean -> Bool | CANONICAL | RESOLVED | NOT_APPLICABLE | CLEAN | none |
| `rem.region` | `rem.region` | `rem.region` | PROJECTION | Void/unspecified -> Void | CANONICAL | CANONICAL | NOT_APPLICABLE | CONFLICT | LIVE_FLAG_WITHOUT_DISPATCH<br>PLUGIN_OWNER_NOT_PROVIDER<br>PROJECTION_MODELED_AS_COMMAND |
| `rem.variableBulk` | `rem.variableBulk` | `rem.variableBulk` | PROJECTION | Void/unspecified -> Void | CANONICAL | CONFLICT | NOT_APPLICABLE | CONFLICT | LIVE_FLAG_WITHOUT_DISPATCH<br>PLUGIN_OWNER_NOT_PROVIDER<br>PROJECTION_MODELED_AS_COMMAND<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `rem.expressionCapsule` | `rem.expressionCapsule` | `rem.expressionCapsule` | PROJECTION | Void/unspecified -> Void | CANONICAL | CONFLICT | NOT_APPLICABLE | CONFLICT | LIVE_FLAG_WITHOUT_DISPATCH<br>PLUGIN_OWNER_NOT_PROVIDER<br>PROJECTION_MODELED_AS_COMMAND<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `rem.flowBreak` | `rem.flowBreak` | `rem.flowBreak` | PROJECTION | Void/unspecified -> Void | CANONICAL | CANONICAL | NOT_APPLICABLE | CONFLICT | LIVE_FLAG_WITHOUT_DISPATCH<br>PLUGIN_OWNER_NOT_PROVIDER<br>PROJECTION_MODELED_AS_COMMAND |
| `rem.offPageOut` | `rem.offPageOut` | `rem.offPageOut` | PROJECTION | Void/unspecified -> Void | CANONICAL | CANONICAL | NOT_APPLICABLE | CONFLICT | LIVE_FLAG_WITHOUT_DISPATCH<br>PLUGIN_OWNER_NOT_PROVIDER<br>PROJECTION_MODELED_AS_COMMAND |
| `rem.offPageIn` | `rem.offPageIn` | `rem.offPageIn` | PROJECTION | Void/unspecified -> Void | CANONICAL | CANONICAL | NOT_APPLICABLE | CONFLICT | LIVE_FLAG_WITHOUT_DISPATCH<br>PLUGIN_OWNER_NOT_PROVIDER<br>PROJECTION_MODELED_AS_COMMAND |
| `rem.group` | `rem.group` | `rem.group` | PROJECTION | Void/unspecified -> Void | CANONICAL | CANONICAL | NOT_APPLICABLE | CONFLICT | LIVE_FLAG_WITHOUT_DISPATCH<br>PLUGIN_OWNER_NOT_PROVIDER<br>PROJECTION_MODELED_AS_COMMAND |
| `rem.layoutHint` | `rem.layoutHint` | `rem.layoutHint` | PROJECTION | Void/unspecified -> Void | CANONICAL | CANONICAL | NOT_APPLICABLE | CONFLICT | LIVE_FLAG_WITHOUT_DISPATCH<br>PLUGIN_OWNER_NOT_PROVIDER<br>PROJECTION_MODELED_AS_COMMAND |
| `chromeTab.isSupported` | `chromeTab.isSupported` | `chromeTab.isSupported` | QUERY | Bool -> Bool | CANONICAL | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | PLUGIN_OWNER_NOT_PROVIDER |
| `chromeTab.bind` | `ChromeTab.bind` | `chromeTab.bind` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `chromeTab.create` | `ChromeTab.create` | `chromeTab.create` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | NORMALIZABLE | LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `chromeTab.mayLaunchUrl` | `ChromeTab.mayLaunchUrl` | `chromeTab.mayLaunchUrl` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `chromeTab.requestPostMessageChannel` | `ChromeTab.requestPostMessageChannel` | `chromeTab.requestPostMessageChannel` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `chromeTab.postMessage` | `ChromeTab.postMessage` | `chromeTab.postMessage` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `chromeTab.validateRelationship` | `ChromeTab.validateRelationship` | `chromeTab.validateRelationship` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.isInstalled` | `tasker.isInstalled` | `tasker.isInstalled` | QUERY | Bool -> Bool | CANONICAL | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.isEnabled` | `tasker.isEnabled` | `tasker.isEnabled` | QUERY | Bool -> Bool | CANONICAL | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.cancel` | `Tasker.cancel` | `tasker.cancel` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.getVariable` | `tasker.getVariable` | `tasker.getVariable` | QUERY | String? -> String? | CANONICAL | CONFLICT | LOSSLESS | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>DEFAULT_SOURCE_CONFLICT<br>PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.clearVariable` | `Tasker.clearVariable` | `tasker.clearVariable` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>DEFAULT_SOURCE_CONFLICT<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.getVariables` | `tasker.getVariables` | `tasker.getVariables` | QUERY | List<TaskerVariable> -> List<TaskerVariable> | CANONICAL | CANONICAL | LOSSLESS | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.lastResult` | `Tasker.lastResult` | `tasker.lastResult` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.error` | `Tasker.error` | `tasker.error` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.action` | `Tasker.action` | `tasker.action` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `tasker.pluginAction` | `Tasker.pluginAction` | `tasker.pluginAction` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `tasker.profileEnable` | `Tasker.profileEnable` | `tasker.profileEnable` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.profileDisable` | `Tasker.profileDisable` | `tasker.profileDisable` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.profileToggle` | `Tasker.profileToggle` | `tasker.profileToggle` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.profileState` | `Tasker.profileState` | `tasker.profileState` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.isInstalled` | `shizuku.isInstalled` | `shizuku.isInstalled` | QUERY | Bool -> Bool | CANONICAL | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.isAvailable` | `shizuku.isAvailable` | `shizuku.isAvailable` | QUERY | Bool -> Bool | CANONICAL | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.getUid` | `shizuku.getUid` | `shizuku.getUid` | QUERY | Number? -> Number? | CANONICAL | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.permissionState` | `Shizuku.permissionState` | `shizuku.permissionState` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.requestPermission` | `Shizuku.requestPermission` | `shizuku.requestPermission` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.bindUserService` | `Shizuku.bindUserService` | `shizuku.bindUserService` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.unbindUserService` | `Shizuku.unbindUserService` | `shizuku.unbindUserService` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.systemService` | `Shizuku.systemService` | `shizuku.systemService` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.call` | `Shizuku.call` | `shizuku.call` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `termux.isInstalled` | `termux.isInstalled` | `termux.isInstalled` | QUERY | Bool -> Bool | CANONICAL | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>PLUGIN_OWNER_NOT_PROVIDER |
| `termux.canRunCommands` | `Termux.canRunCommands` | `termux.canRunCommands` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `termux.writeStdin` | `Termux.writeStdin` | `termux.writeStdin` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `termux.cancel` | `Termux.cancel` | `termux.cancel` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `termux.get` | `termux.get` | `termux.get` | QUERY | String? -> String? | CANONICAL | CANONICAL | LOSSLESS | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.hostAvailable` | `Scrcpy.hostAvailable` | `scrcpy.hostAvailable` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.devices` | `Scrcpy.devices` | `scrcpy.devices` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.connect` | `Scrcpy.connect` | `scrcpy.connect` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.disconnect` | `Scrcpy.disconnect` | `scrcpy.disconnect` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.isRunning` | `scrcpy.isRunning` | `scrcpy.isRunning` | QUERY | Bool -> Bool | CANONICAL | CANONICAL | LOSSLESS | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.get` | `scrcpy.get` | `scrcpy.get` | QUERY | String? -> String? | CANONICAL | CANONICAL | LOSSLESS | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.key` | `Scrcpy.key` | `scrcpy.key` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.text` | `Scrcpy.text` | `scrcpy.text` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.scroll` | `Scrcpy.scroll` | `scrcpy.scroll` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.setClipboard` | `Scrcpy.setClipboard` | `scrcpy.setClipboard` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.setScreenPower` | `Scrcpy.setScreenPower` | `scrcpy.setScreenPower` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.rotate` | `Scrcpy.rotate` | `scrcpy.rotate` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `chart.hide` | `Chart.hide` | `chart.hide` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>LIVE_FLAG_WITHOUT_DISPATCH<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `chart.remove` | `Chart.remove` | `chart.remove` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>LIVE_FLAG_WITHOUT_DISPATCH<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `chart.exists` | `chart.exists` | `chart.exists` | QUERY | Bool -> Bool | CANONICAL | CANONICAL | LOSSLESS | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>PLUGIN_OWNER_NOT_PROVIDER |
| `chart.setData` | `Chart.setData` | `chart.setData` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>LIVE_FLAG_WITHOUT_DISPATCH<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `chart.setOptions` | `Chart.setOptions` | `chart.setOptions` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>LIVE_FLAG_WITHOUT_DISPATCH<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `chart.add` | `Chart.add` | `chart.add` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>LIVE_FLAG_WITHOUT_DISPATCH<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `chart.update` | `Chart.update` | `chart.update` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>LIVE_FLAG_WITHOUT_DISPATCH<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `chart.removeData` | `Chart.removeData` | `chart.removeData` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CONFLICT | NOT_APPLICABLE | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>LIVE_FLAG_WITHOUT_DISPATCH<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER<br>UNTYPED_DEFAULT<br>UNTYPED_PARAMETER |
| `chart.clear` | `Chart.clear` | `chart.clear` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>LIVE_FLAG_WITHOUT_DISPATCH<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |
| `chart.get` | `chart.get` | `chart.get` | QUERY | ChartSnapshot? -> legacy.chartSnapshot? | CANONICAL | CANONICAL | LOSSLESS | NORMALIZABLE | CAPABILITY_PROVIDER_MIXED<br>PLUGIN_OWNER_NOT_PROVIDER |
| `chart.capture` | `Chart.capture` | `chart.capture` | ACTION | Void/unspecified -> Void | NEEDS_NORMALIZATION | CANONICAL | NOT_APPLICABLE | CONFLICT | CAPABILITY_PROVIDER_MIXED<br>LEGACY_ALIAS_REQUIRED<br>LIVE_FLAG_WITHOUT_DISPATCH<br>NAMING_FAMILY_OUTLIER<br>NON_CANONICAL_NAME<br>PLUGIN_OWNER_NOT_PROVIDER |

## Duplicate Canonical Names

| Name | IDs | Actual semantic difference |
| --- | --- | --- |
| `boolean` | `logic.boolean`, `literal.boolean` | Both are visual projections of one Bool-literal expression: logic.boolean is the V1.x legacy alias and literal.boolean is canonical. |
| `if` | `control.if`, `control.ifElse`, `control.ifElseIfElse` | Three control-block variants with different statement-slot contracts; parser syntax selects structure, not a catalog command overload. |

## Alias Resolution Collisions

Current lookup lowercases input and returns the first candidate. The bridge reports every candidate and never uses the winner as semantic truth.

| Input | Candidates | Current winner | V1 result |
| --- | --- | --- | --- |
| `if` | `control.if`, `control.ifElse`, `control.ifElseIfElse` | `control.if` | AMBIGUOUS |

## Command Family Naming Audit

| Family | Current command | Sibling pattern | Proposed canonical | Legacy alias | Confidence | Reason |
| --- | --- | --- | --- | --- | --- | --- |
| action | `click` | lowerCamelCase namespace | `clickText` | true | HIGH | Frozen V1 separates coordinate click from clickText. |
| template | `findTemplate` | templateDefine, templateCompare | `templateFind` | true | HIGH | Sibling commands templateDefine and templateCompare establish the template+operation family. |
| file | `File.readText` | lowerCamelCase namespace | `file.readText` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| clipboard | `Clipboard.get` | lowerCamelCase namespace | `clipboard.get` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| sys | `Sys.info` | lowerCamelCase namespace | `sys.info` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| env | `Env.get` | lowerCamelCase namespace | `env.get` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chromeTab | `ChromeTab.open` | lowerCamelCase namespace | `chromeTab.open` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chromeTab | `ChromeTab.unbind` | lowerCamelCase namespace | `chromeTab.unbind` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| tasker | `Tasker.runTask` | lowerCamelCase namespace | `tasker.runTask` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| tasker | `Tasker.setVariable` | lowerCamelCase namespace | `tasker.setVariable` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| tasker | `Tasker.emitEvent` | lowerCamelCase namespace | `tasker.emitEvent` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| shizuku | `Shizuku.exec` | lowerCamelCase namespace | `shizuku.exec` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| shizuku | `Shizuku.shell` | lowerCamelCase namespace | `shizuku.shell` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| termux | `Termux.run` | lowerCamelCase namespace | `termux.run` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| termux | `Termux.shell` | lowerCamelCase namespace | `termux.shell` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| termux | `Termux.api` | lowerCamelCase namespace | `termux.api` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| scrcpy | `Scrcpy.start` | lowerCamelCase namespace | `scrcpy.start` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| scrcpy | `Scrcpy.stop` | lowerCamelCase namespace | `scrcpy.stop` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| scrcpy | `Scrcpy.touch` | lowerCamelCase namespace | `scrcpy.touch` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chart | `Chart.create` | lowerCamelCase namespace | `chart.create` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chart | `Chart.show` | lowerCamelCase namespace | `chart.show` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chart | `Chart.export` | lowerCamelCase namespace | `chart.export` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chromeTab | `ChromeTab.bind` | lowerCamelCase namespace | `chromeTab.bind` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chromeTab | `ChromeTab.create` | lowerCamelCase namespace | `chromeTab.create` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chromeTab | `ChromeTab.mayLaunchUrl` | lowerCamelCase namespace | `chromeTab.mayLaunchUrl` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chromeTab | `ChromeTab.requestPostMessageChannel` | lowerCamelCase namespace | `chromeTab.requestPostMessageChannel` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chromeTab | `ChromeTab.postMessage` | lowerCamelCase namespace | `chromeTab.postMessage` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chromeTab | `ChromeTab.validateRelationship` | lowerCamelCase namespace | `chromeTab.validateRelationship` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| tasker | `Tasker.cancel` | lowerCamelCase namespace | `tasker.cancel` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| tasker | `Tasker.clearVariable` | lowerCamelCase namespace | `tasker.clearVariable` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| tasker | `Tasker.lastResult` | lowerCamelCase namespace | `tasker.lastResult` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| tasker | `Tasker.error` | lowerCamelCase namespace | `tasker.error` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| tasker | `Tasker.action` | lowerCamelCase namespace | `tasker.action` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| tasker | `Tasker.pluginAction` | lowerCamelCase namespace | `tasker.pluginAction` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| tasker | `Tasker.profileEnable` | lowerCamelCase namespace | `tasker.profileEnable` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| tasker | `Tasker.profileDisable` | lowerCamelCase namespace | `tasker.profileDisable` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| tasker | `Tasker.profileToggle` | lowerCamelCase namespace | `tasker.profileToggle` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| tasker | `Tasker.profileState` | lowerCamelCase namespace | `tasker.profileState` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| shizuku | `Shizuku.permissionState` | lowerCamelCase namespace | `shizuku.permissionState` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| shizuku | `Shizuku.requestPermission` | lowerCamelCase namespace | `shizuku.requestPermission` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| shizuku | `Shizuku.bindUserService` | lowerCamelCase namespace | `shizuku.bindUserService` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| shizuku | `Shizuku.unbindUserService` | lowerCamelCase namespace | `shizuku.unbindUserService` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| shizuku | `Shizuku.systemService` | lowerCamelCase namespace | `shizuku.systemService` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| shizuku | `Shizuku.call` | lowerCamelCase namespace | `shizuku.call` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| termux | `Termux.canRunCommands` | lowerCamelCase namespace | `termux.canRunCommands` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| termux | `Termux.writeStdin` | lowerCamelCase namespace | `termux.writeStdin` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| termux | `Termux.cancel` | lowerCamelCase namespace | `termux.cancel` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| scrcpy | `Scrcpy.hostAvailable` | lowerCamelCase namespace | `scrcpy.hostAvailable` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| scrcpy | `Scrcpy.devices` | lowerCamelCase namespace | `scrcpy.devices` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| scrcpy | `Scrcpy.connect` | lowerCamelCase namespace | `scrcpy.connect` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| scrcpy | `Scrcpy.disconnect` | lowerCamelCase namespace | `scrcpy.disconnect` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| scrcpy | `Scrcpy.key` | lowerCamelCase namespace | `scrcpy.key` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| scrcpy | `Scrcpy.text` | lowerCamelCase namespace | `scrcpy.text` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| scrcpy | `Scrcpy.scroll` | lowerCamelCase namespace | `scrcpy.scroll` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| scrcpy | `Scrcpy.setClipboard` | lowerCamelCase namespace | `scrcpy.setClipboard` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| scrcpy | `Scrcpy.setScreenPower` | lowerCamelCase namespace | `scrcpy.setScreenPower` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| scrcpy | `Scrcpy.rotate` | lowerCamelCase namespace | `scrcpy.rotate` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chart | `Chart.hide` | lowerCamelCase namespace | `chart.hide` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chart | `Chart.remove` | lowerCamelCase namespace | `chart.remove` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chart | `Chart.setData` | lowerCamelCase namespace | `chart.setData` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chart | `Chart.setOptions` | lowerCamelCase namespace | `chart.setOptions` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chart | `Chart.add` | lowerCamelCase namespace | `chart.add` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chart | `Chart.update` | lowerCamelCase namespace | `chart.update` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chart | `Chart.removeData` | lowerCamelCase namespace | `chart.removeData` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chart | `Chart.clear` | lowerCamelCase namespace | `chart.clear` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| chart | `Chart.capture` | lowerCamelCase namespace | `chart.capture` | true | HIGH | V1 qualified command names require lowerCamelCase namespace segments. |
| Marker | `markerSave`, `markerLoad`, `markerDelete` | marker+operation | unchanged | false | HIGH | Consistent sibling family. |
| Scene | `sceneSave` | scene+operation | unchanged | false | MEDIUM | Current family has one member. |
| Vision/Image/OCR | `findTemplate`, `screenshot`, `ocr`, `findText`, `highlight`, `markerSave`, `markerLoad`, `markerDelete`, `templateDefine`, `templateCompare` | mixed | NEEDS_DECISION | true | MEDIUM | Mixed standalone verbs and domain aliases; no mechanical rename. |
| Input | `click`, `swipe`, `clickPoint`, `touch` | operation+target | clickText | true | HIGH | Coordinate click and text click require separate signatures. |
| Feedback | `beep`, `vibrate` | verb | unchanged | false | HIGH | Names are consistent; vibrate signature is not. |
| File/Storage | `datastorePut`, `datastoreGet`, `File.readText`, `file.writeText`, `Clipboard.get`, `clipboard.set`, `cache.clear`, `Sys.info`, `Env.get` | lowerCamel namespace | namespace case only | true | HIGH | Uppercase legacy namespace segments violate V1 naming. |
| Provider namespaces | `findTemplate`, `findText`, `markerSave`, `markerLoad`, `markerDelete`, `templateDefine`, `templateCompare`, `ChromeTab.open`, `ChromeTab.unbind`, `Tasker.runTask`, `Tasker.setVariable`, `Tasker.emitEvent`, `Shizuku.exec`, `Shizuku.shell`, `Termux.run`, `Termux.shell`, `Termux.api`, `Scrcpy.start`, `Scrcpy.stop`, `Scrcpy.touch`, `Chart.create`, `Chart.show`, `Chart.export`, `rem.region`, `rem.variableBulk`, `rem.expressionCapsule`, `rem.flowBreak`, `rem.offPageOut`, `rem.offPageIn`, `rem.group`, `rem.layoutHint`, `chromeTab.isSupported`, `ChromeTab.bind`, `ChromeTab.create`, `ChromeTab.mayLaunchUrl`, `ChromeTab.requestPostMessageChannel`, `ChromeTab.postMessage`, `ChromeTab.validateRelationship`, `tasker.isInstalled`, `tasker.isEnabled`, `Tasker.cancel`, `tasker.getVariable`, `Tasker.clearVariable`, `tasker.getVariables`, `Tasker.lastResult`, `Tasker.error`, `Tasker.action`, `Tasker.pluginAction`, `Tasker.profileEnable`, `Tasker.profileDisable`, `Tasker.profileToggle`, `Tasker.profileState`, `shizuku.isInstalled`, `shizuku.isAvailable`, `shizuku.getUid`, `Shizuku.permissionState`, `Shizuku.requestPermission`, `Shizuku.bindUserService`, `Shizuku.unbindUserService`, `Shizuku.systemService`, `Shizuku.call`, `termux.isInstalled`, `Termux.canRunCommands`, `Termux.writeStdin`, `Termux.cancel`, `termux.get`, `Scrcpy.hostAvailable`, `Scrcpy.devices`, `Scrcpy.connect`, `Scrcpy.disconnect`, `scrcpy.isRunning`, `scrcpy.get`, `Scrcpy.key`, `Scrcpy.text`, `Scrcpy.scroll`, `Scrcpy.setClipboard`, `Scrcpy.setScreenPower`, `Scrcpy.rotate`, `Chart.hide`, `Chart.remove`, `chart.exists`, `Chart.setData`, `Chart.setOptions`, `Chart.add`, `Chart.update`, `Chart.removeData`, `Chart.clear`, `chart.get`, `Chart.capture` | lowerCamel provider.operation | namespace case only | true | HIGH | Provider identity must be separate from pluginOwner. |

`findTemplate` is **NORMALIZABLE** to `templateFind`; `findTemplate` remains a required legacy alias. This is supported by the existing siblings `templateDefine` and `templateCompare`.

## `if` Variant Audit

| ID | Kind | Parameters | Return | Block | Runtime | Semantics |
| --- | --- | --- | --- | --- | --- | --- |
| `control.if` | CONTROL | condition, THEN | Void | `control.if` | branch | Distinct branch-slot variant of one control construct. |
| `control.ifElse` | CONTROL | condition, THEN, ELSE | Void | `control.ifElse` | branch | Distinct branch-slot variant of one control construct. |
| `control.ifElseIfElse` | CONTROL | condition, THEN, elseIfCondition, ELIF, ELSE | Void | `control.ifElseIfElse` | branch | Distinct branch-slot variant of one control construct. |

Classification: **B, variants of the same control family**. They are not three independently callable V1 commands and are not merged in M1B-2A.

## `boolean` Duplicate Audit

| ID | Block | Runtime | Actual meaning |
| --- | --- | --- | --- |
| `logic.boolean` | `logic.boolean` | evaluate | V1.x legacy Bool-literal projection alias |
| `literal.boolean` | `literal.boolean` | evaluate | Canonical Bool-literal projection |

The duplicate is a semantic conflict, not a display-only duplicate. A V1 naming decision is required before migration.

## Query Return Audit

| Command | Kind | Current return | Runtime result | Block output | Proposed return | Evidence | Confidence |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `action.findTemplate` | REPORTER | ImageMatch? | ImageMatchValue or NullValue | binding:emscript.command.action.findTemplate | ImageMatch? | WorkspaceBasicRuntime transports ImageMatchValue or NullValue through the generic command reporter. | HIGH |
| `vision.findText` | REPORTER | TextMatch? | TextMatchValue or NullValue | binding:emscript.command.vision.findText | TextMatch? | WorkspaceBasicRuntime transports the best TextMatchValue or NullValue through the generic command reporter. | HIGH |
| `vision.markerLoad` | REPORTER | Marker? | MarkerValue or NullValue | binding:emscript.command.vision.markerLoad | Marker? | WorkspaceBasicRuntime transports MarkerValue with stable identity or NullValue through the generic command reporter. | HIGH |
| `vision.templateCompare` | REPORTER | Number | numeric score | binding:emscript.command.vision.templateCompare | Number | WorkspaceBasicRuntime obtains a non-null numeric score from templateCompare; failures use diagnostics. | HIGH |
| `system.datastoreGet` | REPORTER | String? | text/value | binding:emscript.command.system.datastoreGet | String? | WorkspaceBasicRuntime obtains a nullable datastore value. | HIGH |
| `file.readText` | REPORTER | String? | text/value | binding:emscript.command.file.readText | String? | WorkspaceBasicRuntime obtains nullable file text. | HIGH |
| `clipboard.get` | REPORTER | String | text/value | binding:emscript.command.clipboard.get | String | WorkspaceBasicRuntime obtains clipboard text. | HIGH |
| `system.info` | REPORTER | String | text/value | binding:emscript.command.system.info | String | WorkspaceBasicRuntime obtains system information text. | HIGH |
| `system.env` | REPORTER | String | String | binding:emscript.command.system.env | String | WorkspaceBasicRuntime obtains an environment value. | HIGH |
| `variable.get` | VARIABLE | Any | Any | binding:variable.get | Any | Catalog declares the concrete typed reporter return Any. | HIGH |
| `logic.screenContains` | REPORTER | Boolean | Boolean | binding:logic.screenContains | Boolean | Reporter has Boolean return type and is evaluated as a condition. | HIGH |
| `logic.boolean` | REPORTER | Boolean | Boolean | binding:logic.boolean | Boolean | Catalog declares the concrete typed reporter return Boolean. | HIGH |
| `chromeTab.isSupported` | REPORTER | Bool | provider result/error | binding:emscript.command.chromeTab.isSupported | Bool | WorkspaceBasicRuntime obtains a typed Bool payload from the Custom Tabs adapter; failures use diagnostics. | HIGH |
| `tasker.isInstalled` | REPORTER | Bool | provider result/error | binding:emscript.command.tasker.isInstalled | Bool | Catalog declares the concrete typed reporter return Bool. | HIGH |
| `tasker.isEnabled` | REPORTER | Bool | provider result/error | binding:emscript.command.tasker.isEnabled | Bool | Catalog declares the concrete typed reporter return Bool. | HIGH |
| `tasker.getVariable` | REPORTER | String? | provider result/error | binding:emscript.command.tasker.getVariable | String? | Catalog declares the concrete typed reporter return String?. | HIGH |
| `tasker.getVariables` | REPORTER | List<TaskerVariable> | provider result/error | binding:emscript.command.tasker.getVariables | List<TaskerVariable> | Catalog declares the concrete typed reporter return List<TaskerVariable>. | HIGH |
| `shizuku.isInstalled` | REPORTER | Bool | provider result/error | binding:emscript.command.shizuku.isInstalled | Bool | Catalog declares the concrete typed reporter return Bool. | HIGH |
| `shizuku.isAvailable` | REPORTER | Bool | provider result/error | binding:emscript.command.shizuku.isAvailable | Bool | Catalog declares the concrete typed reporter return Bool. | HIGH |
| `shizuku.getUid` | REPORTER | Number? | provider result/error | binding:emscript.command.shizuku.getUid | Number? | Catalog declares the concrete typed reporter return Number?. | HIGH |
| `termux.isInstalled` | REPORTER | Bool | provider result/error | binding:emscript.command.termux.isInstalled | Bool | Catalog declares the concrete typed reporter return Bool. | HIGH |
| `termux.get` | REPORTER | String? | provider result/error | binding:emscript.command.termux.get | String? | Catalog declares the concrete typed reporter return String?. | HIGH |
| `scrcpy.isRunning` | REPORTER | Bool | provider result/error | binding:emscript.command.scrcpy.isRunning | Bool | Catalog declares the concrete typed reporter return Bool. | HIGH |
| `scrcpy.get` | REPORTER | String? | provider result/error | binding:emscript.command.scrcpy.get | String? | Catalog declares the concrete typed reporter return String?. | HIGH |
| `chart.exists` | REPORTER | Bool | BooleanValue | binding:emscript.command.chart.exists | Bool | WorkspaceBasicRuntime maps one chart repository lookup to a typed Bool; lookup failures remain diagnostics. | HIGH |
| `chart.get` | REPORTER | ChartSnapshot? | ChartSnapshotValue or NullValue | binding:emscript.command.chart.get | ChartSnapshot? | WorkspaceBasicRuntime transports an immutable ChartSnapshotValue or NullValue through the generic command reporter. | HIGH |

Entries reported as `NEEDS_DECISION` remain Void in the proposed bridge definition so the conflict cannot be mistaken for an invented return type.

## M1B-3U Shizuku Availability Convergence

M1B-3U migrated `shizuku.isAvailable` as non-null Bool through the generic `RuntimeAdapterResult.value` payload. Normal unavailable states remain successful false; installation, permission and Binder inspection failures retain distinct structured diagnostics.

## M1B-3W Scalar Provider Query Convergence

The current `D_QUERY_RETURN` inventory contains 0 language commands. M1B-3W added typed scalar reporters, M1B-3X the Tasker collection reporter, M1B-3Y the perception domain reporters, and M1B-3Z the two chart reporters.

| Class | Count |
| --- | ---: |
| A_NULLABLE_SCALAR_READY | 0 |
| B_NONNULL_SCALAR_READY | 0 |
| C_STRUCTURED_RESULT_TYPE | 0 |
| D_RUNTIME_RESULT_GAP | 0 |
| E_PROVIDER_CONTRACT_GAP | 0 |
| F_SENTINEL_OR_ERROR_COLLISION | 0 |
| G_LANGUAGE_SEMANTICS_GAP | 0 |
| H_AMBIGUOUS | 0 |

| Stable ID | Observed runtime | Proposed V1 | Class | Runtime | Provider | Risk |
| --- | --- | --- | --- | --- | --- | --- |

Provider-local inspections preserve VALUE, ABSENT, legitimate false/empty/zero results and technical FAILURE as separate outcomes.

## M1B-3X Tasker Collection Query Convergence

`tasker.getVariables(pattern?): List<TaskerVariable>` now uses the existing Tasker receiver snapshot and generic reporter/CommandCall projection. A successful query always returns an ordered list; no matches is an empty list, while provider and snapshot failures remain diagnostics.

## M1B-3Y Perception Domain Value Convergence

`action.findTemplate(...): ImageMatch?`, `vision.findText(...): TextMatch?` and `vision.markerLoad(...): Marker?` now use immutable EMScript domain values and generic reporter/CommandCall projection. Completed no-match/no-resource queries return `NullValue`; capture, engine, repository and decode failures remain diagnostics. Only `chart.exists` and `chart.get` remain in `D_QUERY_RETURN`.

## M1B-3Z Chart Domain Value Convergence

`chart.exists(...): Bool` and `chart.get(...): ChartSnapshot?` now share one repository lookup contract. Missing resources map to false/`NullValue`; repository and decode failures remain diagnostics. `ChartSnapshotValue` is immutable and no runtime/UI handle crosses the EMScript boundary. `D_QUERY_RETURN` is zero.

## Default Value Audit

| Command | Parameter | Declared | Raw | Parser | Runtime | V1 type | Bridge result |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `action.wait` | `ms` | DURATION_MS | `500` | numeric literal | numeric literal | `Number` | typed:500 |
| `action.clickText` | `text` | TEXT | `OK` | legacy string token | legacy string token | `String` | typed:OK |
| `action.findTemplate` | `imagePath` | IMAGE_TEMPLATE | `` | legacy string token | legacy string token | `emscript.templateRef` | typed: |
| `action.findTemplate` | `threshold` | PERCENT | `0.82` | numeric literal | numeric literal | `Number` | typed:0.82 |
| `action.findTemplate` | `timeoutMs` | DURATION_MS | `3000` | numeric literal | numeric literal | `Number` | typed:3000 |
| `action.findTemplate` | `retryCount` | NUMBER | `1` | numeric literal | numeric literal | `Number` | typed:1 |
| `action.findTemplate` | `searchRegion` | REGION | `` | legacy string token | legacy string token | `emscript.region` | typed: |
| `action.swipe` | `points` | ANY | `` | expression/raw token | command-specific raw argument parsing | `Any` | typed: |
| `action.swipe` | `repeat` | ANY | `1` | expression/raw token | command-specific raw argument parsing | `Any` | typed:1 |
| `input.clickPoint` | `x` | NUMBER | `0` | numeric literal | numeric literal | `Number` | typed:0 |
| `input.clickPoint` | `y` | NUMBER | `0` | numeric literal | numeric literal | `Number` | typed:0 |
| `input.clickPoint` | `repeat` | NUMBER | `1` | numeric literal | numeric literal | `Number` | typed:1 |
| `input.touch` | `sequence` | ANY | `` | expression/raw token | command-specific raw argument parsing | `Any` | typed: |
| `vision.screenshot` | `path` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `vision.ocr` | `region` | REGION | `` | legacy string token | legacy string token | `emscript.region` | typed: |
| `vision.ocr` | `timeoutMs` | DURATION_MS | `3000` | numeric literal | numeric literal | `Number` | typed:3000 |
| `vision.findText` | `text` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `vision.findText` | `timeoutMs` | DURATION_MS | `3000` | numeric literal | numeric literal | `Number` | typed:3000 |
| `vision.highlight` | `region` | REGION | `` | legacy string token | legacy string token | `emscript.region` | typed: |
| `vision.markerSave` | `name` | TEXT | `marker` | legacy string token | legacy string token | `String` | typed:marker |
| `vision.markerSave` | `region` | REGION | `` | legacy string token | legacy string token | `emscript.region` | typed: |
| `vision.markerSave` | `mode` | TEXT | `region` | legacy string token | legacy string token | `String` | typed:region |
| `vision.markerSave` | `threshold` | PERCENT | `0.85` | numeric literal | numeric literal | `Number` | typed:0.85 |
| `vision.markerLoad` | `name` | TEXT | `marker` | legacy string token | legacy string token | `String` | typed:marker |
| `vision.markerDelete` | `name` | TEXT | `marker` | legacy string token | legacy string token | `String` | typed:marker |
| `vision.templateDefine` | `name` | TEXT | `template` | legacy string token | legacy string token | `String` | typed:template |
| `vision.templateDefine` | `region` | REGION | `` | legacy string token | legacy string token | `emscript.region` | typed: |
| `vision.templateDefine` | `processing` | TEXT | `grayscale` | legacy string token | legacy string token | `String` | typed:grayscale |
| `vision.templateCompare` | `name` | TEXT | `template` | legacy string token | legacy string token | `String` | typed:template |
| `vision.templateCompare` | `region` | REGION | `` | legacy string token | legacy string token | `emscript.region` | typed: |
| `vision.templateCompare` | `processing` | TEXT | `grayscale` | legacy string token | legacy string token | `String` | typed:grayscale |
| `scene.save` | `name` | TEXT | `scene` | legacy string token | legacy string token | `String` | typed:scene |
| `scene.save` | `markerMode` | TEXT | `region` | legacy string token | legacy string token | `String` | typed:region |
| `scene.save` | `region` | REGION | `` | legacy string token | legacy string token | `emscript.region` | typed: |
| `scene.save` | `asset` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `system.datastorePut` | `key` | TEXT | `key` | legacy string token | legacy string token | `String` | typed:key |
| `system.datastorePut` | `value` | TEXT | `value` | legacy string token | legacy string token | `String` | typed:value |
| `system.datastoreGet` | `key` | TEXT | `key` | legacy string token | legacy string token | `String` | typed:key |
| `feedback.beep` | `frequency` | FREQUENCY_HZ | `1000` | numeric literal | numeric literal | `Number` | typed:1000 |
| `feedback.beep` | `durationMs` | DURATION_MS | `200` | numeric literal | numeric literal | `Number` | typed:200 |
| `feedback.beep` | `volume` | PERCENT | `100` | numeric literal | numeric literal | `Number` | typed:100 |
| `debug.log` | `value` | ANY | `"debug"` | expression/raw token | expression rendered to text | `Any` | typed:debug |
| `file.readText` | `path` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `file.writeText` | `path` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `file.writeText` | `text` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `clipboard.set` | `text` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `system.env` | `name` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `chromeTab.open` | `url` | TEXT | `https://` | legacy string token | legacy string token | `String` | typed:https:// |
| `chromeTab.open` | `options` | ANY | `` | expression/raw token | command-specific raw argument parsing | `Any` | typed: |
| `tasker.runTask` | `name` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `tasker.runTask` | `parameters` | ANY | `` | expression/raw token | command-specific raw argument parsing | `Any` | typed: |
| `tasker.setVariable` | `name` | VARIABLE_REF | `%var` | legacy string token | legacy string token | `emscript.variableRef` | typed:%var |
| `tasker.setVariable` | `value` | ANY | `` | expression/raw token | command-specific raw argument parsing | `Any` | typed: |
| `tasker.emitEvent` | `name` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `shizuku.exec` | `command` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `shizuku.exec` | `args` | ANY | `` | expression/raw token | command-specific raw argument parsing | `Any` | typed: |
| `shizuku.shell` | `commandLine` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `termux.run` | `path` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `termux.run` | `args` | ANY | `` | expression/raw token | command-specific raw argument parsing | `Any` | typed: |
| `termux.shell` | `commandLine` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `termux.api` | `command` | TEXT | `battery-status` | legacy string token | legacy string token | `String` | typed:battery-status |
| `scrcpy.start` | `device` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `scrcpy.stop` | `session` | ANY | `` | expression/raw token | command-specific raw argument parsing | `Any` | typed: |
| `scrcpy.touch` | `session` | ANY | `` | expression/raw token | command-specific raw argument parsing | `Any` | typed: |
| `scrcpy.touch` | `action` | TEXT | `tap` | legacy string token | legacy string token | `String` | typed:tap |
| `scrcpy.touch` | `point` | ANY | `` | expression/raw token | command-specific raw argument parsing | `Any` | typed: |
| `chart.create` | `type` | TEXT | `line` | legacy string token | legacy string token | `String` | typed:line |
| `chart.create` | `data` | ANY | `` | expression/raw token | command-specific raw argument parsing | `Any` | typed: |
| `chart.show` | `chart` | ANY | `` | expression/raw token | command-specific raw argument parsing | `Any` | typed: |
| `chart.export` | `chart` | ANY | `` | expression/raw token | command-specific raw argument parsing | `Any` | typed: |
| `chart.export` | `path` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `control.repeat` | `times` | NUMBER | `3` | numeric literal | numeric literal | `Number` | typed:3 |
| `logic.screenContains` | `text` | TEXT | `OK` | legacy string token | legacy string token | `String` | typed:OK |
| `logic.boolean` | `value` | BOOLEAN | `true` | boolean literal | boolean literal | `Bool` | typed:true |
| `logic.operate` | `operator` | TEXT | `add` | legacy string token | legacy string token | `String` | typed:add |
| `logic.compare` | `operator` | TEXT | `GREATER_OR_EQUAL` | legacy string token | legacy string token | `String` | typed:GREATER_OR_EQUAL |
| `literal.number` | `value` | NUMBER | `0` | numeric literal | numeric literal | `Number` | typed:0 |
| `literal.string` | `value` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `literal.boolean` | `value` | BOOLEAN | `false` | boolean literal | boolean literal | `Bool` | typed:false |
| `rem.region` | `name` | TEXT | `region` | legacy string token | legacy string token | `String` | typed:region |
| `rem.region` | `mode` | TEXT | `facet` | legacy string token | legacy string token | `String` | typed:facet |
| `rem.region` | `color` | TEXT | `auto` | legacy string token | legacy string token | `String` | typed:auto |
| `rem.variableBulk` | `name` | TEXT | `variables` | legacy string token | legacy string token | `String` | typed:variables |
| `rem.variableBulk` | `layout` | TEXT | `stack` | legacy string token | legacy string token | `String` | typed:stack |
| `rem.variableBulk` | `variables` | ANY | `{}` | expression/raw token | command-specific raw argument parsing | `Any` | typed:{} |
| `rem.expressionCapsule` | `name` | TEXT | `expression` | legacy string token | legacy string token | `String` | typed:expression |
| `rem.expressionCapsule` | `strategy` | TEXT | `collapse` | legacy string token | legacy string token | `String` | typed:collapse |
| `rem.expressionCapsule` | `nodes` | ANY | `{}` | expression/raw token | command-specific raw argument parsing | `Any` | typed:{} |
| `rem.flowBreak` | `label` | TEXT | `break` | legacy string token | legacy string token | `String` | typed:break |
| `rem.flowBreak` | `direction` | TEXT | `right` | legacy string token | legacy string token | `String` | typed:right |
| `rem.offPageOut` | `connector` | TEXT | `A` | legacy string token | legacy string token | `String` | typed:A |
| `rem.offPageIn` | `connector` | TEXT | `A` | legacy string token | legacy string token | `String` | typed:A |
| `rem.group` | `name` | TEXT | `group` | legacy string token | legacy string token | `String` | typed:group |
| `rem.group` | `active` | BOOLEAN | `true` | boolean literal | boolean literal | `Bool` | typed:true |
| `rem.layoutHint` | `mode` | TEXT | `vertical` | legacy string token | legacy string token | `String` | typed:vertical |
| `rem.layoutHint` | `scope` | TEXT | `next` | legacy string token | legacy string token | `String` | typed:next |
| `chromeTab.bind` | `packageName` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `chromeTab.create` | `url` | TEXT | `https://` | legacy string token | legacy string token | `String` | typed:https:// |
| `chromeTab.create` | `options` | ANY | `{}` | expression/raw token | command-specific raw argument parsing | `Any` | typed:{} |
| `chromeTab.mayLaunchUrl` | `url` | TEXT | `https://` | legacy string token | legacy string token | `String` | typed:https:// |
| `chromeTab.requestPostMessageChannel` | `origin` | TEXT | `https://` | legacy string token | legacy string token | `String` | typed:https:// |
| `chromeTab.postMessage` | `message` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `chromeTab.validateRelationship` | `origin` | TEXT | `https://` | legacy string token | legacy string token | `String` | typed:https:// |
| `chromeTab.validateRelationship` | `relation` | TEXT | `delegate_permission/common.handle_all_urls` | legacy string token | legacy string token | `String` | typed:delegate_permission/common.handle_all_urls |
| `tasker.cancel` | `name` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `tasker.getVariable` | `name` | VARIABLE_REF | `%var` | legacy string token | legacy string token | `emscript.variableRef` | typed:%var |
| `tasker.clearVariable` | `name` | VARIABLE_REF | `%var` | legacy string token | legacy string token | `emscript.variableRef` | typed:%var |
| `tasker.getVariables` | `pattern` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `tasker.lastResult` | `runId` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `tasker.error` | `runId` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `tasker.action` | `action` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `tasker.action` | `args` | ANY | `{}` | expression/raw token | command-specific raw argument parsing | `Any` | typed:{} |
| `tasker.pluginAction` | `plugin` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `tasker.pluginAction` | `action` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `tasker.pluginAction` | `args` | ANY | `{}` | expression/raw token | command-specific raw argument parsing | `Any` | typed:{} |
| `tasker.profileEnable` | `profile` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `tasker.profileDisable` | `profile` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `tasker.profileToggle` | `profile` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `tasker.profileState` | `profile` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `shizuku.bindUserService` | `component` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `shizuku.unbindUserService` | `component` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `shizuku.systemService` | `name` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `shizuku.call` | `service` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `shizuku.call` | `method` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `shizuku.call` | `args` | ANY | `{}` | expression/raw token | command-specific raw argument parsing | `Any` | typed:{} |
| `termux.writeStdin` | `sessionId` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `termux.writeStdin` | `text` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `termux.cancel` | `sessionId` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `termux.get` | `key` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `scrcpy.connect` | `serial` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `scrcpy.disconnect` | `serial` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `scrcpy.isRunning` | `serial` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `scrcpy.get` | `key` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `scrcpy.key` | `key` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `scrcpy.key` | `repeat` | NUMBER | `1` | numeric literal | numeric literal | `Number` | typed:1 |
| `scrcpy.text` | `text` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `scrcpy.scroll` | `x` | NUMBER | `0` | numeric literal | numeric literal | `Number` | typed:0 |
| `scrcpy.scroll` | `y` | NUMBER | `0` | numeric literal | numeric literal | `Number` | typed:0 |
| `scrcpy.scroll` | `deltaY` | NUMBER | `1` | numeric literal | numeric literal | `Number` | typed:1 |
| `scrcpy.scroll` | `deltaX` | NUMBER | `0` | numeric literal | numeric literal | `Number` | typed:0 |
| `scrcpy.setClipboard` | `text` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `scrcpy.setScreenPower` | `on` | BOOLEAN | `true` | boolean literal | boolean literal | `Bool` | typed:true |
| `scrcpy.rotate` | `direction` | TEXT | `right` | legacy string token | legacy string token | `String` | typed:right |
| `chart.hide` | `id` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `chart.remove` | `id` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `chart.exists` | `id` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `chart.setData` | `id` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `chart.setData` | `data` | ANY | `{}` | expression/raw token | command-specific raw argument parsing | `Any` | typed:{} |
| `chart.setOptions` | `id` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `chart.setOptions` | `options` | ANY | `{}` | expression/raw token | command-specific raw argument parsing | `Any` | typed:{} |
| `chart.add` | `id` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `chart.add` | `data` | ANY | `{}` | expression/raw token | command-specific raw argument parsing | `Any` | typed:{} |
| `chart.update` | `id` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `chart.update` | `patch` | ANY | `{}` | expression/raw token | command-specific raw argument parsing | `Any` | typed:{} |
| `chart.removeData` | `id` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `chart.removeData` | `selector` | ANY | `{}` | expression/raw token | command-specific raw argument parsing | `Any` | typed:{} |
| `chart.clear` | `id` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `chart.get` | `id` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `chart.get` | `key` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `chart.capture` | `id` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |
| `chart.capture` | `path` | TEXT | `` | legacy string token | legacy string token | `String` | typed: |

`ANY` defaults are typed only as a bridge guess and remain marked `UNTYPED_DEFAULT`; they are not promoted to V1 truth.

## Signature Conflicts

### click / clickText

`action.clickText` currently exposes `click(text)`. V1 requires `click` for point/coordinate semantics and `clickText` for text lookup. Legacy `click("text")` therefore needs signature-aware import normalization, not a simple alias.

### vibrate

Catalog: one `DURATION_MS` parameter with default `80`. Parser/generator/runtime: multiple integer durations interpreted as an alternating vibration/pause pattern. Runtime consumes a comma-separated `Long` list and falls back to `[80]`. Result: CLEAN; V1 signature remains undecided.

### log

Catalog declares `TEXT`; parser accepts an expression and runtime renders the resulting value to text. This supports `log(Any)` plus deterministic string conversion, but M1B-2A records the conflict rather than changing either side. Result: CLEAN.

## Provider / Capability / Owner Audit

| Command | Capability | pluginOwner | Observed adapter | Provider candidate | Conflict |
| --- | --- | --- | --- | --- | --- |
| `action.clickText` | A11Y | `visualtasker.core` | A concrete evaluator or WorkspaceBasicRuntime branch exists for action.clickText. | `provider.accessibility` | none |
| `action.findTemplate` | VISION | `visualtasker.vision` | A concrete evaluator or WorkspaceBasicRuntime branch exists for action.findTemplate. | `provider.vision` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `action.swipe` | A11Y | `visualtasker.core` | A concrete evaluator or WorkspaceBasicRuntime branch exists for action.swipe. | `provider.accessibility` | none |
| `input.clickPoint` | A11Y | `visualtasker.core` | A concrete evaluator or WorkspaceBasicRuntime branch exists for input.clickPoint. | `provider.accessibility` | none |
| `input.touch` | A11Y | `visualtasker.core` | Catalog explicitly marks liveImplemented=false. | `provider.accessibility` | none |
| `vision.screenshot` | SCREEN_CAPTURE | `visualtasker.core` | A concrete evaluator or WorkspaceBasicRuntime branch exists for vision.screenshot. | `provider.screenCapture` | none |
| `vision.ocr` | VISION | `visualtasker.core` | Catalog declares runtime metadata, but no explicit live dispatch branch was confirmed. | `provider.vision` | none |
| `vision.findText` | VISION | `visualtasker.vision` | A concrete evaluator or WorkspaceBasicRuntime branch exists for vision.findText. | `provider.vision` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `vision.highlight` | VISION | `visualtasker.core` | Catalog declares runtime metadata, but no explicit live dispatch branch was confirmed. | `provider.vision` | none |
| `vision.markerSave` | VISION | `visualtasker.vision` | A concrete evaluator or WorkspaceBasicRuntime branch exists for vision.markerSave. | `provider.vision` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `vision.markerLoad` | VISION | `visualtasker.vision` | A concrete evaluator or WorkspaceBasicRuntime branch exists for vision.markerLoad. | `provider.vision` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `vision.markerDelete` | VISION | `visualtasker.vision` | A concrete evaluator or WorkspaceBasicRuntime branch exists for vision.markerDelete. | `provider.vision` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `vision.templateDefine` | VISION | `visualtasker.vision` | A concrete evaluator or WorkspaceBasicRuntime branch exists for vision.templateDefine. | `provider.vision` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `vision.templateCompare` | VISION | `visualtasker.vision` | A concrete evaluator or WorkspaceBasicRuntime branch exists for vision.templateCompare. | `provider.vision` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `chromeTab.open` | CUSTOM_TAB | `visualtasker.customtabs` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.customTabs` | PLUGIN_OWNER_NOT_PROVIDER |
| `chromeTab.close` | CUSTOM_TAB | `visualtasker.customtabs` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.customTabs` | PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.runTask` | TASKER | `visualtasker.tasker` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.tasker` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.setVariable` | TASKER | `visualtasker.tasker` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.tasker` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.emitEvent` | TASKER | `visualtasker.tasker` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.tasker` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.exec` | SHIZUKU | `visualtasker.shizuku` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.shizuku` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.shell` | SHIZUKU | `visualtasker.shizuku` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.shizuku` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `termux.run` | TERMUX | `visualtasker.termux` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.termux` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `termux.shell` | TERMUX | `visualtasker.termux` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.termux` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `termux.api` | TERMUX | `visualtasker.termux` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.termux` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.start` | SCRCPY | `visualtasker.scrcpy` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.scrcpy` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.stop` | SCRCPY | `visualtasker.scrcpy` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.scrcpy` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.touch` | SCRCPY | `visualtasker.scrcpy` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.scrcpy` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `chart.create` | CHARTS | `visualtasker.charts` | No Chart namespace branch exists in WorkspaceBasicRuntime. | `provider.charts` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `chart.show` | CHARTS | `visualtasker.charts` | No Chart namespace branch exists in WorkspaceBasicRuntime. | `provider.charts` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `chart.export` | CHARTS | `visualtasker.charts` | No Chart namespace branch exists in WorkspaceBasicRuntime. | `provider.charts` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `logic.screenContains` | A11Y | `visualtasker.core` | A concrete evaluator or WorkspaceBasicRuntime branch exists for logic.screenContains. | `provider.accessibility` | none |
| `rem.region` | CORE | `visualtasker.flowchart` | REM entries are consumed as visual metadata and have no WorkspaceBasicRuntime dispatch branch. | `none` | PLUGIN_OWNER_NOT_PROVIDER |
| `rem.variableBulk` | CORE | `visualtasker.flowchart` | REM entries are consumed as visual metadata and have no WorkspaceBasicRuntime dispatch branch. | `none` | PLUGIN_OWNER_NOT_PROVIDER |
| `rem.expressionCapsule` | CORE | `visualtasker.flowchart` | REM entries are consumed as visual metadata and have no WorkspaceBasicRuntime dispatch branch. | `none` | PLUGIN_OWNER_NOT_PROVIDER |
| `rem.flowBreak` | CORE | `visualtasker.flowchart` | REM entries are consumed as visual metadata and have no WorkspaceBasicRuntime dispatch branch. | `none` | PLUGIN_OWNER_NOT_PROVIDER |
| `rem.offPageOut` | CORE | `visualtasker.flowchart` | REM entries are consumed as visual metadata and have no WorkspaceBasicRuntime dispatch branch. | `none` | PLUGIN_OWNER_NOT_PROVIDER |
| `rem.offPageIn` | CORE | `visualtasker.flowchart` | REM entries are consumed as visual metadata and have no WorkspaceBasicRuntime dispatch branch. | `none` | PLUGIN_OWNER_NOT_PROVIDER |
| `rem.group` | CORE | `visualtasker.flowchart` | REM entries are consumed as visual metadata and have no WorkspaceBasicRuntime dispatch branch. | `none` | PLUGIN_OWNER_NOT_PROVIDER |
| `rem.layoutHint` | CORE | `visualtasker.flowchart` | REM entries are consumed as visual metadata and have no WorkspaceBasicRuntime dispatch branch. | `none` | PLUGIN_OWNER_NOT_PROVIDER |
| `chromeTab.isSupported` | CUSTOM_TAB | `visualtasker.customtabs` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.customTabs` | PLUGIN_OWNER_NOT_PROVIDER |
| `chromeTab.bind` | CUSTOM_TAB | `visualtasker.customtabs` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.customTabs` | PLUGIN_OWNER_NOT_PROVIDER |
| `chromeTab.create` | CUSTOM_TAB | `visualtasker.customtabs` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.customTabs` | PLUGIN_OWNER_NOT_PROVIDER |
| `chromeTab.mayLaunchUrl` | CUSTOM_TAB | `visualtasker.customtabs` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.customTabs` | PLUGIN_OWNER_NOT_PROVIDER |
| `chromeTab.requestPostMessageChannel` | CUSTOM_TAB | `visualtasker.customtabs` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.customTabs` | PLUGIN_OWNER_NOT_PROVIDER |
| `chromeTab.postMessage` | CUSTOM_TAB | `visualtasker.customtabs` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.customTabs` | PLUGIN_OWNER_NOT_PROVIDER |
| `chromeTab.validateRelationship` | CUSTOM_TAB | `visualtasker.customtabs` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.customTabs` | PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.isInstalled` | TASKER | `visualtasker.tasker` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.tasker` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.isEnabled` | TASKER | `visualtasker.tasker` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.tasker` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.cancel` | TASKER | `visualtasker.tasker` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.tasker` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.getVariable` | TASKER | `visualtasker.tasker` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.tasker` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.clearVariable` | TASKER | `visualtasker.tasker` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.tasker` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.getVariables` | TASKER | `visualtasker.tasker` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.tasker` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.lastResult` | TASKER | `visualtasker.tasker` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.tasker` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.error` | TASKER | `visualtasker.tasker` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.tasker` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.action` | TASKER | `visualtasker.tasker` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.tasker` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.pluginAction` | TASKER | `visualtasker.tasker` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.tasker` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.profileEnable` | TASKER | `visualtasker.tasker` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.tasker` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.profileDisable` | TASKER | `visualtasker.tasker` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.tasker` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.profileToggle` | TASKER | `visualtasker.tasker` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.tasker` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `tasker.profileState` | TASKER | `visualtasker.tasker` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.tasker` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.isInstalled` | SHIZUKU | `visualtasker.shizuku` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.shizuku` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.isAvailable` | SHIZUKU | `visualtasker.shizuku` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.shizuku` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.getUid` | SHIZUKU | `visualtasker.shizuku` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.shizuku` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.permissionState` | SHIZUKU | `visualtasker.shizuku` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.shizuku` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.requestPermission` | SHIZUKU | `visualtasker.shizuku` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.shizuku` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.bindUserService` | SHIZUKU | `visualtasker.shizuku` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.shizuku` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.unbindUserService` | SHIZUKU | `visualtasker.shizuku` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.shizuku` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.systemService` | SHIZUKU | `visualtasker.shizuku` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.shizuku` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `shizuku.call` | SHIZUKU | `visualtasker.shizuku` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.shizuku` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `termux.isInstalled` | TERMUX | `visualtasker.termux` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.termux` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `termux.canRunCommands` | TERMUX | `visualtasker.termux` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.termux` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `termux.writeStdin` | TERMUX | `visualtasker.termux` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.termux` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `termux.cancel` | TERMUX | `visualtasker.termux` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.termux` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `termux.get` | TERMUX | `visualtasker.termux` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.termux` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.hostAvailable` | SCRCPY | `visualtasker.scrcpy` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.scrcpy` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.devices` | SCRCPY | `visualtasker.scrcpy` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.scrcpy` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.connect` | SCRCPY | `visualtasker.scrcpy` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.scrcpy` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.disconnect` | SCRCPY | `visualtasker.scrcpy` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.scrcpy` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.isRunning` | SCRCPY | `visualtasker.scrcpy` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.scrcpy` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.get` | SCRCPY | `visualtasker.scrcpy` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.scrcpy` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.key` | SCRCPY | `visualtasker.scrcpy` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.scrcpy` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.text` | SCRCPY | `visualtasker.scrcpy` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.scrcpy` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.scroll` | SCRCPY | `visualtasker.scrcpy` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.scrcpy` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.setClipboard` | SCRCPY | `visualtasker.scrcpy` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.scrcpy` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.setScreenPower` | SCRCPY | `visualtasker.scrcpy` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.scrcpy` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `scrcpy.rotate` | SCRCPY | `visualtasker.scrcpy` | WorkspaceBasicRuntime dispatches the namespace to an environment adapter; availability is external. | `provider.scrcpy` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `chart.hide` | CHARTS | `visualtasker.charts` | No Chart namespace branch exists in WorkspaceBasicRuntime. | `provider.charts` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `chart.remove` | CHARTS | `visualtasker.charts` | No Chart namespace branch exists in WorkspaceBasicRuntime. | `provider.charts` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `chart.exists` | CHARTS | `visualtasker.charts` | A concrete evaluator or WorkspaceBasicRuntime branch exists for chart.exists. | `provider.charts` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `chart.setData` | CHARTS | `visualtasker.charts` | No Chart namespace branch exists in WorkspaceBasicRuntime. | `provider.charts` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `chart.setOptions` | CHARTS | `visualtasker.charts` | No Chart namespace branch exists in WorkspaceBasicRuntime. | `provider.charts` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `chart.add` | CHARTS | `visualtasker.charts` | No Chart namespace branch exists in WorkspaceBasicRuntime. | `provider.charts` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `chart.update` | CHARTS | `visualtasker.charts` | No Chart namespace branch exists in WorkspaceBasicRuntime. | `provider.charts` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `chart.removeData` | CHARTS | `visualtasker.charts` | No Chart namespace branch exists in WorkspaceBasicRuntime. | `provider.charts` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `chart.clear` | CHARTS | `visualtasker.charts` | No Chart namespace branch exists in WorkspaceBasicRuntime. | `provider.charts` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `chart.get` | CHARTS | `visualtasker.charts` | A concrete evaluator or WorkspaceBasicRuntime branch exists for chart.get. | `provider.charts` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |
| `chart.capture` | CHARTS | `visualtasker.charts` | No Chart namespace branch exists in WorkspaceBasicRuntime. | `provider.charts` | CAPABILITY_PROVIDER_MIXED, PLUGIN_OWNER_NOT_PROVIDER |

## Live Dispatch Audit

`liveImplemented=true` is treated only as a legacy claim. Confirmation requires resolver/gate, runtime branch, adapter and result/error path.

### LIVE_FLAG_WITHOUT_DISPATCH

| Command | Catalog flag | Classification | Evidence |
| --- | --- | --- | --- |
| `vision.ocr` | true | CATALOG_ONLY | Catalog declares runtime metadata, but no explicit live dispatch branch was confirmed. |
| `vision.highlight` | true | CATALOG_ONLY | Catalog declares runtime metadata, but no explicit live dispatch branch was confirmed. |
| `scene.save` | true | CATALOG_ONLY | Catalog declares runtime metadata, but no explicit live dispatch branch was confirmed. |
| `chart.create` | true | NO_DISPATCH | No Chart namespace branch exists in WorkspaceBasicRuntime. |
| `chart.show` | true | NO_DISPATCH | No Chart namespace branch exists in WorkspaceBasicRuntime. |
| `chart.export` | true | NO_DISPATCH | No Chart namespace branch exists in WorkspaceBasicRuntime. |
| `rem.region` | true | NO_DISPATCH | REM entries are consumed as visual metadata and have no WorkspaceBasicRuntime dispatch branch. |
| `rem.variableBulk` | true | NO_DISPATCH | REM entries are consumed as visual metadata and have no WorkspaceBasicRuntime dispatch branch. |
| `rem.expressionCapsule` | true | NO_DISPATCH | REM entries are consumed as visual metadata and have no WorkspaceBasicRuntime dispatch branch. |
| `rem.flowBreak` | true | NO_DISPATCH | REM entries are consumed as visual metadata and have no WorkspaceBasicRuntime dispatch branch. |
| `rem.offPageOut` | true | NO_DISPATCH | REM entries are consumed as visual metadata and have no WorkspaceBasicRuntime dispatch branch. |
| `rem.offPageIn` | true | NO_DISPATCH | REM entries are consumed as visual metadata and have no WorkspaceBasicRuntime dispatch branch. |
| `rem.group` | true | NO_DISPATCH | REM entries are consumed as visual metadata and have no WorkspaceBasicRuntime dispatch branch. |
| `rem.layoutHint` | true | NO_DISPATCH | REM entries are consumed as visual metadata and have no WorkspaceBasicRuntime dispatch branch. |
| `chart.hide` | true | NO_DISPATCH | No Chart namespace branch exists in WorkspaceBasicRuntime. |
| `chart.remove` | true | NO_DISPATCH | No Chart namespace branch exists in WorkspaceBasicRuntime. |
| `chart.setData` | true | NO_DISPATCH | No Chart namespace branch exists in WorkspaceBasicRuntime. |
| `chart.setOptions` | true | NO_DISPATCH | No Chart namespace branch exists in WorkspaceBasicRuntime. |
| `chart.add` | true | NO_DISPATCH | No Chart namespace branch exists in WorkspaceBasicRuntime. |
| `chart.update` | true | NO_DISPATCH | No Chart namespace branch exists in WorkspaceBasicRuntime. |
| `chart.removeData` | true | NO_DISPATCH | No Chart namespace branch exists in WorkspaceBasicRuntime. |
| `chart.clear` | true | NO_DISPATCH | No Chart namespace branch exists in WorkspaceBasicRuntime. |
| `chart.capture` | true | NO_DISPATCH | No Chart namespace branch exists in WorkspaceBasicRuntime. |

## Projection Commands

| ID | Kind | Side effect | liveImplemented | Bridge result |
| --- | --- | --- | --- | --- |
| `rem.region` | STATEMENT | CONTROL_FLOW | true | PROJECTION_MODELED_AS_COMMAND |
| `rem.variableBulk` | STATEMENT | CONTROL_FLOW | true | PROJECTION_MODELED_AS_COMMAND |
| `rem.expressionCapsule` | STATEMENT | CONTROL_FLOW | true | PROJECTION_MODELED_AS_COMMAND |
| `rem.flowBreak` | STATEMENT | CONTROL_FLOW | true | PROJECTION_MODELED_AS_COMMAND |
| `rem.offPageOut` | STATEMENT | CONTROL_FLOW | true | PROJECTION_MODELED_AS_COMMAND |
| `rem.offPageIn` | STATEMENT | CONTROL_FLOW | true | PROJECTION_MODELED_AS_COMMAND |
| `rem.group` | STATEMENT | CONTROL_FLOW | true | PROJECTION_MODELED_AS_COMMAND |
| `rem.layoutHint` | STATEMENT | CONTROL_FLOW | true | PROJECTION_MODELED_AS_COMMAND |

All `rem.*` entries must later move to `ProjectionDefinition`; none is migrated here.

## M1B-2D Preflight Decisions

| Candidate | Classification | Decision | Reason |
| --- | --- | --- | --- |
| `event.start` | SAFE | NATIVE_V1 | Existing implicit source entrypoint maps losslessly to the event block, IR root and runtime entrypoint. |
| `variable.get` | EXPRESSION_PROJECTION | LEGACY_ALIAS | Source variables use dynamic reporter identity and serialize as references, not `get(...)`. |
| `logic.and` | CONFLICT | DEFER | Existing M1B-1 `OperatorDefinition` owns `&&`; a command would duplicate semantic truth. |
| `logic.or` | CONFLICT | DEFER | Existing M1B-1 `OperatorDefinition` owns `||`; a command would duplicate semantic truth. |
| `literal.number` | CONFLICT | DEFER | Number literals belong to grammar and expression IR, not callable command syntax. |

## Native V1 Migration

`event.start`, `action.wait` and `feedback.beep` are the M1B-2D native commands. Typed defaults project to the existing legacy scalar strings, and shared dual-read parity covers identity, signature, return, side effect, capability, provider and dispatchability.

## Migration Boundary

M1B-2D changes only the source of truth for `event.start`. It does not make the implicit entrypoint source-visible and does not change canonical names, aliases, command kinds, return values, parser acceptance, block definitions, runtime gates, dispatch, providers or projection behavior. The four rejected candidates remain explicit contract conflicts; all other proposed definitions remain read-only analysis output.

## M1B-3A Naming Normalization

Command identity and source spelling are independent. The catalog keeps each stable ID and writes the canonical V1 spelling; explicit aliases retain V1.x reads and have no removal before V2.

| Stable ID | Legacy spelling | Canonical V1 | Alias lifecycle | Naming | Overall |
| --- | --- | --- | --- | --- | --- |
| `file.writeText` | `File.writeText` | `file.writeText` | `File.writeText`: read V1.x, remove >= V2 | RESOLVED_WITH_LEGACY_ALIAS | CLEAN / NONE |
| `clipboard.set` | `Clipboard.set` | `clipboard.set` | `Clipboard.set`: read V1.x, remove >= V2 | RESOLVED_WITH_LEGACY_ALIAS | CLEAN / NONE |
| `cache.clear` | `Cache.clear` | `cache.clear` | `Cache.clear`: read V1.x, remove >= V2 | RESOLVED_WITH_LEGACY_ALIAS | CLEAN / NONE |

`action.findTemplate` remains `NEEDS_NORMALIZATION` toward `templateFind` for naming/default-source reasons. Its query-return contract was independently resolved by M1B-3Y; M1B-3A itself does not change runtime semantics.

## M1B-3 Remaining-Entry Groups

The groups are exclusive and ordered by the first architectural blocker: projection, operator/literal model, control structure, query return, provider ownership, runtime dispatch, type contract, naming, then lossless generic candidates.

| Group | Count |
| --- | ---: |
| A_SAFE_GENERIC | 8 |
| B_NAMING_NORMALIZATION | 4 |
| C_TYPE_CONFLICT | 1 |
| D_QUERY_RETURN | 0 |
| E_CONTROL_STRUCTURE | 5 |
| F_OPERATOR_OR_LITERAL_MODEL | 6 |
| G_PROVIDER_DEPENDENT | 86 |
| H_PROJECTION | 11 |
| I_RUNTIME_DISPATCH_CONFLICT | 3 |
| TOTAL_REMAINING | 124 |

## M1B-3B Type-Conflict Decisions

`TypeStatus` is orthogonal to bridge and migration status. `CONTRACT_DECIDED` means the normative treatment is known, not that the legacy representation has already been migrated.

| Stable ID | Legacy type | Normative V1 type/model | Classification | Type status | Implemented | Remaining blocker |
| --- | --- | --- | --- | --- | --- | --- |
| `system.datastorePut` | key: TEXT, value: ANY = String literal | datastorePut(key: String, value: String) | TYPE_MAPPING | RESOLVED | yes | None for static type checking and expression transport; NATIVE_V1 migration remains a separate decision. |
| `debug.log` | message: TEXT | log(value: Any): Void with deterministic scalar text rendering | TYPE_MAPPING | RESOLVED | yes | None for expression transport; NATIVE_V1 migration remains a separate decision. |
| `feedback.vibrate` | pattern: DURATION_MS = 80 | vibrate(patternMs: Number...): Void; one value is a duration, multiple values alternate delay/vibration phases | SIGNATURE_DECISION | RESOLVED | yes | None for the V1 signature or expression transport; the single text field remains a legacy visual fallback until a later block mutator slice. |
| `variable.set` | variable: VARIABLE_REF, value: ANY | SET value must be assignable to the referenced variable's declared or inferred type; Any only accepts unrestricted values when the variable itself is Any | TYPECHECKER_RULE | RESOLVED | yes | None for assignment type checking; NATIVE_V1 migration remains a separate decision. |
| `variable.get` | VARIABLE_REF reporter returning Any | VariableReference expression whose result type is the referenced variable's declared or inferred type | EXPRESSION_MODEL | RESOLVED | yes | None for expression ownership or V1.x legacy workspace migration. |
| `logic.boolean` | BOOL reporter command boolean(value) | Bool literal expression owned by grammar/AST/IR; logic.boolean and literal.boolean are historical visual projections | EXPRESSION_MODEL | RESOLVED | yes | None for expression ownership or V1.x legacy workspace migration. |
| `literal.boolean` | BOOL reporter catalog entry | Canonical visual projection of the Bool literal expression owned by grammar/AST/IR | EXPRESSION_MODEL | RESOLVED | yes | None; it remains a visual projection rather than a V1 command. |
| `input.touch` | sequence: ANY raw payload | Legacy structural migration into typed touch primitives, PointerPath, MultiPath or GestureSequence according to recoverable payload semantics | STRUCTURAL_MIGRATION | CONTRACT_DECIDED | no | No fixture proves timing, pointer identity, coordinate space, multi-pointer structure or complete gesture boundaries; no single typed replacement is lossless. |

M1B-3C implements `debug.log` expression transport, M1B-3D typed `variable.set`, M1B-3E `datastorePut(String, String)`, M1B-3F separates VariableReference and Bool literal projections from language commands, and M1B-3G converges `feedback.vibrate` on one variadic Number signature. The other 1 decision still requires structural migration work. These bounded implementations do not imply native migration; `NATIVE_V1` remains 3.

## M1B-3C Lossless Command-Argument Expressions

The first conformance probe is `log(value: Any)`. Its parameter is a semantic ValueInput and carries the existing literal, VariableReference or operator reporter into `IrExpression`; conversion to display text occurs only after DryRun/runtime evaluation.

Invariants:

- `Command Argument Expression != Serialized String`
- `Any != String`
- `VariableReference != variable.get Command`
- Config/presentation fields remain fields; only semantic INPUT properties use reporter connections.
- Legacy stored `message` fields remain read-compatible but are not the canonical projection.

`debug.log`: TypeStatus `RESOLVED`, ExpressionStatus `LOSSLESS`, bridge `CLEAN`, migration `NONE`. The last value must remain non-native until a separate migration slice.

## M1B-3D Typed Variable Assignment

`LET` stores the declared or initializer-inferred type in `WorkspaceDocument.variables[variableId]`. `SET` resolves the target by `variableId`, types its connected expression, and applies `LanguageTypeCompatibility` before IR generation.

`variable.set`: TypeStatus `RESOLVED`, ExpressionStatus `LOSSLESS`, bridge `CLEAN`, migration `NONE`. Legacy catalog `Any` remains a bridge issue, so the command is intentionally not `NATIVE_V1`.

## M1B-3E datastorePut String Contract

`datastorePut` has two required semantic ValueInputs: `key: String` and `value: String`. Parser expressions are preserved through Workspace and IR; `WorkspaceValueTypeSystem` delegates compatibility to `LanguageTypeCompatibility`. Number, Bool and Any do not implicitly convert to String and fail during pre-apply validation.

The same catalog-driven ValueInput mechanism validates command parameters that explicitly declare `acceptedTypes`. `system.datastoreGet` is now a separate nullable `String?` reporter; no unrelated command was migrated by that change.

`system.datastorePut`: TypeStatus `RESOLVED`, ExpressionStatus `LOSSLESS`, bridge `CLEAN`, migration `NONE`. It remains non-native by design.

## M1B-3F Expression Model Cleanup

`VariableReference != Command`, `Bool Literal != Command`, `Visual Projection != Language Command`, and `Legacy Catalog Entry != V1 Command`.

| Catalog entry | Catalog role | Canonical semantic model | Bridge | Migration |
| --- | --- | --- | --- | --- |
| `variable.get` | LEGACY_EXPRESSION_ALIAS | VariableReference expression whose result type is the referenced variable's declared or inferred type | CLEAN | LEGACY_ALIAS |
| `logic.boolean` | LEGACY_EXPRESSION_ALIAS | Bool literal expression owned by grammar/AST/IR; logic.boolean and literal.boolean are historical visual projections | CLEAN | LEGACY_ALIAS |
| `literal.boolean` | CANONICAL_EXPRESSION_PROJECTION | Canonical visual projection of the Bool literal expression owned by grammar/AST/IR | CLEAN | NONE |

The compatibility catalog still contains 127 heterogeneous entries. That inventory includes language commands, expressions, literals, visual projections, structural constructs and legacy aliases; it is not the EMScript V1 language-command count. `NATIVE_V1` remains 3.

## M1B-3G feedback.vibrate Signature Convergence

The normative V1 contract is `vibrate(patternMs: Number...): Void`. The variadic parameter is required, which generically enforces a minimum of one argument; there is no maximum, no language default and no repeat parameter.

Every pattern member follows the shared command-expression path and must be Number-compatible. One value is dispatched as a one-shot duration. Multiple values retain their order as alternating delay/vibration phases and are dispatched as a non-repeating waveform. Even and odd lengths are both accepted.

Zero and negative values remain explicit through source, Workspace, IR and DryRun. The existing Android adapter clamps them to zero and removes non-positive phases before one-shot/waveform dispatch. If no positive phase remains, it performs no vibration. This is adapter behavior, not a source-language default or validation rule.

Legacy workspaces with an explicit scalar `pattern` field remain readable and serialize as one explicit argument, including an explicit `80`. The existing empty-field fallback to `80` is classified strictly as `LEGACY READ NORMALIZATION`; it is not part of the V1 CommandDefinition. Source `vibrate()` is rejected and never becomes `vibrate(80)`. A dynamic block mutator is intentionally deferred.

`feedback.vibrate`: TypeStatus `RESOLVED`, ExpressionStatus `LOSSLESS`, bridge `CLEAN`, migration `NONE`. It remains non-native by design.

## M1B-3H-A Legacy input.touch Structural Classification

The classifier is analysis-only. It preserves exact raw argument text, records only observed evidence and does not choose a V1 target type. No current repository fixture proves timing, pointer identity, multi-pointer structure, complete gesture boundaries or coordinate space.

| Alias | Payload | Classification | Evidence | Migration readiness |
| --- | --- | --- | --- | --- |
| `touch` | `[540, 1100]` | PARTIAL | HAS_COORDINATES | REQUIRES_STRUCTURAL_MIGRATION |
| `touch` | `["down", 120, 240, "up"]` | PARTIAL | HAS_DOWN<br>HAS_UP<br>HAS_COORDINATES | REQUIRES_STRUCTURAL_MIGRATION |
| `touch` | `"down(10,20);move(20,30);up(20,30)"` | PARTIAL | HAS_DOWN<br>HAS_MOVE<br>HAS_UP<br>HAS_COORDINATES | REQUIRES_STRUCTURAL_MIGRATION |

`input.touch`: TypeStatus `CONTRACT_DECIDED`, bridge `NORMALIZABLE`, migration `STRUCTURAL_MIGRATION`. It remains the sole `C_TYPE_CONFLICT`, is not `NATIVE_V1`, and live dispatch remains disabled.
