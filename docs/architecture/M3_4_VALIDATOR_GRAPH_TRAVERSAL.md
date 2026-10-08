# M3-4 Validator Performance and Graph Traversal

## Scope

M3-4 optimizes validation-local graph analysis only. It does not change workspace mutation,
canonical documents, EMScript syntax, source anchors, runtime behavior, or public graph APIs.

## Baseline Invariants

The following behavior was captured from the pre-M3-4 validator before production changes:

1. Blocks are validated in `WorkspaceDocument.blocks` iteration order.
2. Per-block diagnostics are emitted in this order: unknown type; required inputs and type
   compatibility; missing connection partners; invalid previous linkage; invalid next linkage.
3. An unknown block type emits `UnknownBlockType` and skips all remaining checks for that block.
4. A connected required value input whose target connection cannot be resolved emits
   `DisconnectedChain`; every unresolved endpoint also emits `InvalidConnection` when the block's
   connections are inspected.
5. Reachability starts from `rootBlocks` and follows valid next-to-previous, statement-to-previous,
   and value-input-to-output edges. Blocks absent from that closure emit `OrphanBlock` in block-map
   order.
6. Cycle analysis uses the same directed next, statement, and value-input edges. `CycleDetected` is
   emitted not only for cycle members but for every block from which a cycle can be reached.
7. Cycle diagnostics are emitted in block-map order.
8. The final result is de-duplicated by diagnostic message while preserving the first occurrence.
9. Validation never derives or changes block, connection, semantic entity, or relation identities.
10. Traversal order may be optimized, but externally observable diagnostics, their payloads, and
    their deterministic order must remain unchanged for an unchanged document.

These invariants are pinned by `ValidatorGraphTraversalTest` before replacing the old repeated
suffix traversal and per-start cycle search.

## Planned Optimization Boundary

Each `validate` call may construct one private index of connection IDs and directed semantic graph
edges. Reachability and cycle-reachability then share that index. The index is disposable validation
state and is not a second workspace authority.

Performance results and the final implementation notes will be added after the regression fixtures
and full repository gates pass.

## Implementation

`Validator.validate` now creates one private `ValidationGraphIndex` per invocation. The index:

- resolves every connection ID once, preserving the first connection in document iteration order;
- records deterministic directed edges for next chains, statement inputs, and value reporters;
- supplies all per-block endpoint checks without repeated `WorkspaceGraph.findConnection` scans;
- computes root reachability with one iterative traversal; and
- computes cycle reachability with one shared WHITE/GRAY/BLACK DFS state.

The cycle result deliberately remains broader than strongly connected component membership. A block
whose outgoing graph can reach a cycle still receives `CycleDetected`, matching the pre-M3-4
contract. Diagnostics continue to be emitted in document block order and de-duplicated by message.

Index construction, reachability, and cycle analysis each inspect a connection or graph edge a
constant number of times. Their graph-analysis bound is `O(V + E)` in time and space. Validator code
no longer calls `WorkspaceGraph.findConnection`, `chainFrom`, `statementStack`, or `descendants`.

## Regression Coverage

`ValidatorGraphTraversalTest` covers:

- a valid linear chain;
- IF branch reachability with nested comparison reporters;
- independent roots;
- deterministic orphan order;
- invalid endpoint and disconnected-chain order;
- a self-referential expression cycle;
- a two-node expression cycle with multiple predecessors; and
- a next-connection cycle that terminates deterministically.

Existing validation fixtures continue to cover required/optional inputs, shared type compatibility,
nullable diagnostics, unknown definitions, and sample workspaces.

## Performance Matrix

The raw matrix is retained in `M3_4_VALIDATOR_PERFORMANCE_PROFILE.csv`. Each row uses the real
EMScript import/apply route. Validation is then warmed once and measured five times; the table keeps
the median and maximum. Times are JVM wall-clock milliseconds and should be treated as diagnostic
samples rather than a device UI budget.

At 320 statements, median validation is 1.269-1.403 ms across legacy/anchor and
format-only/structural paths. M3-3 measured 82,561-84,964 ms for the corresponding validation stage.
The previous non-linear explosion is therefore removed, and the 250 ms acceptance target is met by
more than two orders of magnitude.

The full apply path at 320 statements measures 161-296 ms. That remaining time is outside validator
graph traversal and is consistent with the previously documented importer collection copies and
anchor candidate scans. Those costs are not changed in M3-4.

## Ownership and Compatibility

The index is ephemeral validator state. It is neither serialized nor exposed as a graph authority.
Workspace operations, canonical mutation, source-anchor transport, EMScript syntax, runtime, stable
IDs, and public `WorkspaceGraph` behavior are unchanged.
