# M3-3 Performance Diagnosis

## Scope and Method

This is a diagnostic close-out for M3-3. It changes no parser, identity, source-apply, transaction,
validation, projection, or publication semantics.

The profile used linear EMScript programs containing 40, 80, 160, or 320 distinct `wait(n)`
statements. Each size was measured through four paths after a 20-statement warm-up:

- legacy source, format-only edit;
- legacy source, one appended statement;
- anchored source, format-only edit;
- anchored source, one unanchored appended statement.

Measurements were taken in one Gradle/JUnit JVM on 2026-10-08. They are diagnostic wall-clock
samples, not stable performance budgets. The temporary timing hooks were removed after capture.
The raw values are in `M3_3_PERFORMANCE_PROFILE.csv`.

## Results

Times are milliseconds. `Anchor` includes directive decoding, validation, block binding, and
entity/relation anchor matching. `Identity` contains the remaining reconciliation work. `Publish`
includes compatibility projection, EMScript generation, and workspace serialization.

| Statements | Path | Change | Parse | Anchor | Identity | Plan | Transaction | Validation | Publish |
|---:|---|---|---:|---:|---:|---:|---:|---:|---:|
| 40 | legacy | format | 0.794 | 0.246 | 7.100 | 6.996 | 1.249 | 40.883 | 8.319 |
| 40 | legacy | structural | 0.858 | 0.269 | 8.458 | 5.477 | 2.989 | 49.297 | 9.326 |
| 40 | anchor | format | 3.771 | 3.247 | 6.134 | 3.804 | 0.939 | 27.986 | 7.442 |
| 40 | anchor | structural | 4.411 | 4.965 | 6.790 | 4.651 | 2.621 | 31.190 | 6.513 |
| 80 | legacy | format | 1.511 | 0.694 | 9.850 | 9.403 | 1.786 | 475.430 | 20.122 |
| 80 | legacy | structural | 1.144 | 0.661 | 10.318 | 4.970 | 2.028 | 347.222 | 12.078 |
| 80 | anchor | format | 9.125 | 8.635 | 8.299 | 5.127 | 0.741 | 302.903 | 9.049 |
| 80 | anchor | structural | 4.567 | 6.535 | 4.890 | 5.108 | 2.748 | 365.329 | 7.334 |
| 160 | legacy | format | 0.664 | 0.865 | 9.789 | 16.583 | 1.721 | 4893.193 | 18.805 |
| 160 | legacy | structural | 1.163 | 1.112 | 12.976 | 7.668 | 3.739 | 4988.477 | 11.430 |
| 160 | anchor | format | 7.369 | 18.521 | 10.549 | 7.503 | 0.880 | 4952.000 | 13.695 |
| 160 | anchor | structural | 7.834 | 18.501 | 11.530 | 8.975 | 5.643 | 4953.394 | 10.993 |
| 320 | legacy | format | 3.224 | 1.774 | 18.023 | 19.370 | 1.621 | 84068.800 | 21.462 |
| 320 | legacy | structural | 1.598 | 2.004 | 17.487 | 18.060 | 4.999 | 82561.360 | 21.114 |
| 320 | anchor | format | 10.250 | 17.446 | 15.989 | 17.863 | 1.525 | 83608.290 | 23.132 |
| 320 | anchor | structural | 9.087 | 12.574 | 15.122 | 25.826 | 3.984 | 84964.199 | 20.138 |

The 320-statement validator accounts for more than 99.9 percent of the measured stages. An eightfold
increase from 40 to 320 statements increases validation by roughly 1,675x to 2,988x, corresponding
to an observed exponent of about 3.6 to 3.9. Legacy versus anchor and format-only versus structural
changes do not materially alter that result.

The importer assembler itself remains between about 14 ms and 80 ms over the measured range. It
uses zero reducer calls, constructs one bulk block object per resulting block, and publishes once.

## Calls, Traversals, and Copies

For a linear workspace with `B` blocks, `Validator.collectReachable` first walks the complete chain.
It then calls `WorkspaceGraph.descendants` for every chain member. `descendants` recursively walks
each remaining suffix and also advances over that suffix after recursion returns. This produces
approximately:

`B * (B + 1) * (B + 2) / 6`

suffix traversal calls. The measured block counts and corresponding theoretical suffix calls are:

| Blocks | Suffix traversal calls | Per-start cycle traversals |
|---:|---:|---:|
| 41 | 12,341 | 861 |
| 81 | 91,881 | 3,321 |
| 161 | 708,561 | 13,041 |
| 321 | 5,564,321 | 51,681 |

Each successful `nextChain` lookup calls `WorkspaceGraph.findConnection`, which scans blocks and
their connections linearly. `Validator.hasCycle` then starts a fresh DFS for every block instead of
sharing visited state, adding the triangular per-start traversal shown above. A live thread dump
during the 320-statement case repeatedly located execution in
`Validator.collectReachable -> WorkspaceGraph.descendants -> WorkspaceGraph.nextChain -> findConnection`.

The bulk importer performs, for `N` unchanged wait statements:

- `N + 1` `InstantiateBlock` actions;
- `N` `UpdateField` actions;
- `N` `Connect` actions;
- zero reducer calls;
- one publication.

It still creates immutable copies while assembling. Instantiate rebuilds blocks, roots, and root
positions; source annotation, field update, and source-property marking rebuild block/metadata maps;
connect rebuilds blocks, roots, and positions. This is a lower bound of roughly `12N + 3`
collection rebuilds before incidental temporary collections and per-connection list copies. At 320
statements that is at least 3,843 rebuilds. Their measured cost is secondary, but their growing-map
copies explain why assembly is not strictly linear.

Anchor reconciliation currently performs repeated candidate scans:

- entity anchor matching: `entityAnchors * entities` upper-bound comparisons;
- relation anchor matching: `relationAnchors * relations` upper-bound comparisons.

At 320 anchored statements these are 103,040 and 102,399 comparisons for format-only input. They
remain around 17 ms in this profile and are not the immediate bottleneck. The source-apply planner
also checks unchanged entities against the operations list; this is harmless for the measured zero-
or two-operation plans but can become `entities * operations` for broad edits.

## Root Cause

The nonlinearity is not caused by persistent source anchors. It is caused primarily by repeated
whole-suffix graph traversal inside reachability validation, multiplied by linear connection lookup.
The per-block fresh cycle DFS is a secondary superlinear cost. Importer collection copying and
anchor candidate scans are measurable but two to four orders of magnitude smaller at 320 statements.

## Prioritized Optimization

The smallest high-value follow-up is confined to `Validator`:

1. Build one local connection/adjacency index at the start of `validate`.
2. Compute reachability once with an iterative DFS/BFS over that index.
3. Reuse a color/state map for cycle detection instead of starting an independent DFS from every block.

This should reduce graph analysis to `O(V + E)` after one index build, without changing canonical
semantics, IDs, diagnostics, mutation order, or public contracts. It must be implemented as a
separate bounded slice with golden validation-result tests and the same 40/80/160/320 profile. No
optimization was applied during M3-3 close-out.

After that, and only if profiling still justifies it, relation/entity anchor candidate lists can be
replaced by maps keyed by legacy source identity and relation identity. Importer mutable staging is a
lower-priority optimization because it is not currently dominant.

## Risks

- Wall-clock samples are single-run diagnostics and include JVM/GC noise.
- The profile uses long linear stacks; nested branch-heavy graphs may shift secondary costs.
- A validator rewrite could accidentally change diagnostic ordering or duplicate suppression unless
  result equivalence is tested explicitly.
- Anchor and planner quadratic upper bounds remain for very large or broad-edit workspaces even after
  validator optimization.
