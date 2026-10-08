# M3-1 Semantic Property Schema

## Status

M3-1 adds catalog-backed property discovery and validation to the M3-0 operation boundary. It does not add a generic inspector, a text diff engine, VT2VT transport, or another mutation engine.

## Ownership

`SemanticPropertyDefinition` and `SemanticPropertySchema` are neutral contracts in `workflow-core`. The concrete command schema lives in `workflow-semantics` as `VisualTaskerSemanticPropertySchema` because that module already owns `VisualTaskerCommandCatalog` and depends on `workflow-core`. This preserves dependency direction and avoids moving editor or catalog types into the core.

Command properties are derived from `CommandCatalogEntry.arguments`. There is no second command-property registry. Variable and compatibility-payload properties are supplied by `CanonicalSemanticPropertySchema`, then command definitions override matching fallback definitions by stable `SemanticPropertyId`.

## Property Definition

Each definition exposes:

- stable `SemanticPropertyId`
- owning `SemanticEntityKind`
- existing EMScript `LanguageTypeRef`
- nullability
- writability
- scalar or structural semantics
- validation constraints
- optional owning command ID

Property IDs are independent of labels, field indexes, connectors, geometry, editors, and rendering order.

## Type Reuse

The schema reuses `LanguageTypeRef`, `CoreTypes`, and `LanguageTypeCompatibility`. `WorkspacePropertyValue.Text`, `Number`, and `Bool` map centrally to the corresponding language types. `Null` is handled through the property's nullability contract. No third type system was introduced.

Legacy `CommandArgumentType` values are adapted once in `SemanticPropertyCatalog.kt`. Duration, frequency, and percent remain numeric; optional arguments without defaults become nullable. Parameter source selectors are derived companion properties named `<argument>.source`.

## Variables

Variable `name`, `type`, `scope`, `defaultValue`, and `initialValue` are discoverable. Name and type are non-blank. Scope is constrained to `VariableScope`. Initial/default values may be null and accept the existing scalar value envelope.

`variableId` is read-only. Variable identity cannot be changed through `SetProperty`. A block's `variableId` reference may be set or cleared because that changes a reference relation, not the variable entity identity.

## Structural Boundary

`SetProperty` is limited to scalar semantic payload. Entity identity is read-only. Relation order and branch structure are explicitly classified as structural and rejected with `STRUCTURAL_PROPERTY_REQUIRED`.

Branches, expressions, sequences, relations, root order, and entity identity continue to use their structural `WorkspaceOperation` variants.

## Validation and Atomicity

`WorkspaceOperationExecutor` validates every `SetProperty` before invoking the M2 canonical mutator. Validation covers:

- target existence
- property discovery
- owner compatibility through entity-specific discovery
- writability
- structural classification
- scalar type compatibility
- nullability
- non-blank, allowed-text, and numeric-range constraints

Failure returns a typed diagnostic and the original document. Revision, history publication, projection publication, and canonical state remain unchanged.

## Discovery

`SemanticPropertySchema.propertiesFor(document, entityId)` provides a deterministic, Compose-free discovery API. A future Block inspector, Flow inspector, text tool, component designer, or AI tool can combine these definitions with current canonical payload values without owning semantic metadata.

## Editor Convergence

BlockEditor controller and view-model mutations pass `VisualTaskerSemanticPropertySchema` into the existing reducer. Text, number, boolean, display-label, note, and parameter-source changes continue through `WorkspaceAction.UpdateField`, then `WorkspaceOperation.SetProperty`.

FlowEditor inspector field changes use the same catalog schema through `WorkspaceFlowchartMutations`. Structural FlowEditor actions remain structural reducer operations.

Text Apply remains assessment-only. Its later implementation should parse and diff source into one `WorkspaceTransaction`; M3-1 does not add that diff engine.

## Undo, Persistence, and Replay

The existing reducer publishes one resulting document for each accepted property operation, so current snapshot undo/redo remains compatible. Transactions remain atomic and deterministic. Existing workspace serialization stores the resulting canonical and compatibility state; serialized operations remain suitable for deterministic local replay and later VT2VT transport.

## Remaining M3 Work

The largest remaining gap is transaction-based Text Apply and broader migration of importer/editor mutation call sites. M3-2 should define a semantic source-apply plan that emits one validated transaction while preserving identity and diagnostics. It must not introduce remote synchronization or a generic inspector.
