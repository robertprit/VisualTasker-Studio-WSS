# Next Architecture Audit Briefing — Intelligence, Learning & Adaptive Recording

> **Status:** NEXT AUDIT / DISCUSSION BACKLOG
>
> **Purpose:** Preserve the newer VisualTasker Studio WSS architecture work that is not covered by the legacy Studio reference migration or the first code-vs-contract audit.
>
> **Authority:** This document is a briefing and decision backlog, not a normative contract. `VISUALTASKER_ARCHITECTURE_CONTRACT.md` and accepted ADRs remain authoritative.

## Confirmed terminology and product decisions

The following are already decided and must not be reopened by the audit without concrete contradictory implementation evidence:

- **Junktor** is the canonical term. It is intentionally derived from the syntactically/linguistically appropriate relation term; `Junktionator` is only a historical joke/alias and must not appear as an architectural identifier.
- **ShapeMaker is an integrated VisualTasker Studio tool**, not merely an optional external integration. A visual development Studio should provide a native drawing/shape authoring surface. ShapeMaker may retain modular/service boundaries internally, but from the Studio product architecture it belongs to the integrated authoring toolset.
- ShapeMaker is expected to support reusable visual assets for WSS/VAL, overlays, indicators, Block/Node forms and programmable/animated visual appearances. Its contracts must avoid making drawing state a competing Workflow or Worldview truth.

## Why this exists

The legacy-reference audit mainly covers EMScript round-trip, capabilities/providers, runtime compatibility and Vision → Worldview boundaries. Several newer architecture strands were developed after the old Studio contracts and therefore must be consolidated separately rather than inferred from legacy documentation.

The next audit should inventory current WSS docs, skills/prompts and implementation, distinguish already-decided principles from open design questions, and produce small implementation/contract slices. It must not invent missing product decisions.

## Architecture families to consolidate

### 1. VAL / WSS Visual Semantics + integrated ShapeMaker

Target chain:

`Domain semantics → Projection semantics → VisualSemanticState → VisualPolicy → Material/Compose representation`

Audit:
- semantic axes: role, authority, certainty, activity, validation, focus, availability, freshness;
- shape/icon/label/outline/badge/pattern/motion/opacity/connector/color channels;
- color must support meaning, never be the only carrier;
- projection kinds including Block, Flow, RailTrace, Inspector, World/Scene and AI projections;
- connector semantics such as CONTROL_FLOW, VALUE_FLOW, OBSERVES, RESOLVES_TO, DERIVED_FROM, REFERENCES, EXPECTS, TRANSITIONS_TO, USES_RESOURCE, CONFLICTS_WITH, PROPOSES;
- relationship to Material 3 Expressive shapes;
- **ShapeMaker as the integrated Studio drawing/shape/animation authoring tool**;
- contracts between ShapeMaker assets and VAL/WSS visual policies;
- use of ShapeMaker assets for overlays, system-wide visual indicators, Block/Node forms, reusable visual resources and programmable/animated appearances;
- import/export and stable asset identity;
- whether a Visual Semantics Inspector is useful for debugging policies.

Architectural boundary:

`ShapeMaker creates/manages visual assets and animations; VAL assigns semantic visual meaning; projections consume the resulting visual policy/assets.`

ShapeMaker must not independently decide Workflow intent, Worldview truth or Runtime authority.

### 2. VT Intelligence Runtime

Candidate contracts:
- `AiTask`
- `AiContext`
- `AiWorkEvent`
- `AiResult`
- `AiProposal`

Core rule:

`AI result != canonical mutation`

Expected flow:

`Task → ContextBuilder → ModelRouter → Model → Structured Result Decoder → Validator → Proposal Gateway → WSS Projection → optional approved Domain Command`

The primary machine-facing output should be structured/parseable. Human-readable explanation is an optional projection. Raw private reasoning is not a UI surface.

Candidate task types:
- INTERPRET_INTENT
- ANALYZE_SCENE
- RESOLVE_ENTITY
- DIAGNOSE_WORKFLOW
- PROPOSE_RECOVERY
- GENERATE_WORKFLOW
- GENERATE_EMSCRIPT
- EXPLAIN_SELECTION
- CURATE_DATASET
- ASK_CLARIFICATION

Audit current AI/chat code for direct state mutation, duplicated context construction and unstructured result assumptions.

### 3. Model Routing — large and small local models

Separate model roles instead of treating every model as a generic chatbot.

Candidate classes:
- larger Gemma/Qwen-class model: complex interpretation, diagnosis, planning and workflow generation;
- small local model: low-latency specialist tasks;
- FunctionGemma-class small model: natural-language intent → stable VTS Capability/function + typed parameters;
- classical ML specialists: entity resolution, scene transition, action outcome, recovery/provider reliability;
- YOLO/vision models: perception only.

Open questions:
- exact routing policy;
- fallback rules and latency budgets;
- device capability tiers;
- when a model may delegate to another model/provider;
- what confidence/calibration data the router needs.

### 4. LLM fine-tuning / calibration

Fine-tune stable VTS semantics and task behavior, not transient world state.

Candidate pipeline:

`Base Model → Dataset Version → LoRA/QLoRA → Evaluation → Quantization/Export → Model Candidate → Approval → Model Registry → Android Deployment`

Keep current screenshots, current Scene state, current Entities, user variables and rapidly changing external documentation retrieval-fed rather than baked into weights.

Audit requirements:
- ModelDescriptor/versioning;
- base-model provenance;
- adapter version;
- quantization/runtime compatibility;
- training dataset version;
- evaluation suite/version;
- supported task/capability set;
- rollback.

### 5. Function Learning

Goal:

`user language → stable CapabilityId/function → typed parameters`

This is distinct from general LLM fine-tuning.

Audit:
- canonical function/capability registry dependency;
- aliases and paraphrases;
- ambiguous intent handling;
- negative examples;
- parameter extraction;
- provider independence;
- evaluation by exact capability and typed argument correctness.

### 6. Classical ML / Experience Learning

Candidate first specialist: Entity Resolver.

Later candidates:
- SceneTransition prediction;
- ActionOutcome prediction;
- Recovery selection;
- Provider reliability/ranking.

Rules:
- ML prediction is not Observation or truth;
- false entity merge is high-cost;
- confidence and provenance must remain visible;
- classical ML should be used where structured features and calibrated outputs are preferable to an LLM.

Open decision: exact first feature schema and model family (e.g. logistic model, tree ensemble, gradient boosting) must follow available data/evaluation, not preference alone.

### 7. YOLO / Vision Training

Candidate lifecycle:

`CaptureFrame/Resource → Annotation → Dataset Version → Train → Evaluate → Model Registry → Vision Provider → Observation`

Hard boundary:

`Detection != WorldEntity`

YOLO/OCR/OpenCV/A11y outputs remain provider observations/evidence. Entity resolution and Worldview interpretation happen after perception.

Audit:
- annotation schema;
- class/version identity;
- bounding-box coordinate spaces;
- model provenance;
- evaluation metrics;
- deployment/quantization;
- provider result normalization;
- model update/rollback.

### 8. Dataset Architecture

Do not collapse knowledge, memory, runtime records, perception artifacts and training data into one store.

Required distinctions:

`Knowledge != Memory != Dataset != Worldview != RetrievalContext`

Candidate lifecycle entities:
- Raw Artifact / CaptureFrame;
- Observation;
- Annotation;
- Human Correction;
- Learning Candidate;
- TrainingExample;
- DatasetVersion;
- EvaluationSet;
- ModelVersion / ModelCandidate;
- Deployment record.

Audit provenance, derivation links, deduplication, retention, export/import and reproducibility.

### 9. Ecological Recording / Experience Learning

Working definition: normal use of VTS may generate structured learning candidates without turning normal operation into uncontrolled automatic training.

Target loop:

`Execution + Observation + Interpretation + Outcome + Human Correction`
`→ Record`
`→ Learning Candidate`
`→ Filter / Curation / Perugger`
`→ TrainingExample`
`→ Dataset Version`
`→ Training`
`→ Evaluation`
`→ Model Candidate`
`→ Approval`
`→ Model Registry / Deployment`

Hard rule:

`normal use != automatic model mutation`

Recording is the active capture process. Record is persisted structured history. RailTrace is a projection. Dataset candidates are derived artifacts. These concepts must remain separate.

Open questions:
- candidate-generation thresholds;
- retention and sampling;
- negative/failed examples;
- privacy-sensitive captures;
- duplicate suppression;
- when a correction becomes reusable training material;
- promotion rules from candidate → training example.

### 10. Active Learning / Human-Machine Perception Debugger (Perugger)

Candidate trigger classes:
- low confidence;
- conflicting evidence;
- entity ambiguity;
- scene ambiguity;
- selection ambiguity;
- intent ambiguity;
- branch ambiguity;
- provider disagreement;
- failed prediction/outcome;
- generalization uncertainty.

Human correction must be represented as new evidence/correction, not as retroactive rewriting of the original Observation.

Candidate semantics:
`ACCEPT / REJECT / DEFER / PROMOTE / INSPECT`

Audit how corrections feed Worldview, Record and Dataset pipelines without bypassing validators.

### 11. Prediction vs Reality

Required distinctions:

`Prediction != Observation != Assumption != Outcome`

Candidate loop:

`Workflow/DryRun → Prediction → LiveRun → Observation → Outcome → Difference → Record → optional Learning Candidate`

This should project consistently into RailTrace, Flow, Block, Inspector and Scene views.

### 12. AI/WSS Projection Contract

AI should appear through the domain panel that owns the semantic result rather than forcing every interaction through a chat panel.

Candidate projections:
- ghost/proposed Blocks;
- ghost/proposed Flow nodes/edges;
- EMScript generated diff;
- RailTrace interpretation/prediction track;
- Inspector evidence/confidence/model provenance;
- Scene layers Observed / Predicted / Unknown / Ambiguous / Expected;
- Perugger comparison/correction UI;
- Dataset curation surfaces.

Visible progress should expose structured result construction such as evidence counts, candidates, ambiguity, confidence, proposals and diagnostics — not private chain-of-thought.

### 13. Semantic Translation Layer

Candidate chain:

`Human Language ↔ StudioLexicon ↔ Intent Normalizer ↔ Context Grounder ↔ Semantic Resolver ↔ AI Task Mapper ↔ VTS Domain Contracts ↔ Result Interpreter ↔ WSS Projection Mapper`

Question to answer:
How can a human communicate with complex automation without knowing the internal architecture while VTS still preserves typed, inspectable domain semantics?

This layer should depend on stable Studio vocabulary and Capability IDs rather than free-form strings.

### 14. Model Registry / Deployment

A model must not become active merely because training completed.

Candidate states:

`TRAINED → EVALUATED → CANDIDATE → APPROVED → DEPLOYED → RETIRED/ROLLED_BACK`

Record at minimum:
- model ID/version;
- model family/base;
- task types;
- capability compatibility;
- dataset/eval versions;
- quantization;
- runtime/backend requirements;
- device compatibility;
- calibration metrics;
- deployment status.

### 15. Evaluation Architecture

Separate evaluation suites are needed for different failure modes:
- EMScript parse/generate/round-trip;
- Function/Capability mapping;
- workflow generation;
- Entity Resolution;
- Scene interpretation;
- vision detection;
- action outcome prediction;
- recovery selection;
- provider routing;
- model regression after fine-tuning/quantization.

Evaluation must precede automatic promotion of a model candidate.

### 16. Junktor semantics

`Junktor` is the canonical architectural term for explicit semantic/evidence relations. Candidate kinds currently include:
- CONJUNCTION
- SUBJUNCTION
- DISJUNCTION
- ADJUNCTION
- BIJUNCTION

The audit should locate existing relation/reasoning implementations and determine where Junktor belongs without conflating it with EntityResolver. EntityResolver is a process; Junktor expresses a typed reasoning/evidence relation used for interpretation and explanation. Junktor itself has no mutation authority.

## Cross-cutting invariants

Preserve these through every future contract:

- `WorkflowDocument = Intent truth`
- `Worldview = Reality/Knowledge model`
- `Runtime = coupling between Intent and Reality`
- `Observation != WorldEntity`
- `Prediction != Observation`
- `Resource != Observation != Entity`
- `AI Proposal != Domain Command`
- `Capability != Provider implementation`
- `Record != Recording != RailTrace`
- `Learning Candidate != TrainingExample`
- `Training completion != Deployment approval`
- UI projections own no independent durable domain truth.

## Required next audit outputs

A future Codex/architecture audit should create or propose:

1. `docs/reference/ai/INTELLIGENCE_RUNTIME.md`
2. `docs/reference/ai/SEMANTIC_TRANSLATION_LAYER.md`
3. `docs/reference/ai/MODEL_ROUTING.md`
4. `docs/reference/ai/AI_PROJECTION_CONTRACT.md`
5. `docs/reference/learning/LEARNING_ARCHITECTURE.md`
6. `docs/reference/learning/ECOLOGICAL_RECORDING.md`
7. `docs/reference/learning/ACTIVE_LEARNING.md`
8. `docs/reference/learning/DATASET_LIFECYCLE.md`
9. `docs/reference/learning/MODEL_REGISTRY.md`
10. `docs/reference/learning/EVALUATION.md`
11. `docs/reference/learning/LLM_FINETUNING.md`
12. `docs/reference/learning/FUNCTION_LEARNING.md`
13. `docs/reference/learning/ML_TRAINING.md`
14. `docs/reference/learning/YOLO_TRAINING.md`
15. `docs/reference/visual/VAL_CONTRACT.md`
16. `docs/reference/visual/AI_VISUAL_SEMANTICS.md`
17. `docs/reference/visual/SHAPEMAKER_INTEGRATION.md`
18. `docs/reference/worldview/PREDICTION_VS_OBSERVATION.md`
19. `docs/reference/worldview/ENTITY_RESOLUTION.md`
20. `docs/reference/worldview/JUNKTOR_SEMANTICS.md`

Do not create these as normative contracts merely from this briefing. First inventory existing implementation/docs/prompts, mark each concept `DECIDED`, `PARTIAL`, `PROPOSED`, `UNKNOWN`, or `OWNER_DECISION_REQUIRED`, then promote only confirmed material.

## Owner decisions still expected

The following should remain explicitly open until evidence or product preference resolves them:

1. Exact large-vs-small model routing policy and latency thresholds.
2. Exact local model families/versions; Gemma/Qwen/FunctionGemma are roles/candidates, not permanent architectural dependencies unless deliberately adopted.
3. First classical-ML model family and feature schema.
4. Ecological-recording retention/privacy/sampling policy.
5. Promotion policy from LearningCandidate to TrainingExample.
6. Dataset/version storage and export format.
7. Model approval/deployment authority and rollback UX.
8. Exact Perugger public naming/UI placement versus internal codename.
9. Which AI work events are useful as visible projections without exposing private reasoning.
10. How much prediction state is persisted versus recomputed.
11. Whether Visual Semantics Inspector is a release feature or developer-only tool.
12. ShapeMaker's internal module/service contract, persistence ownership and asset API. **Its integration into VisualTasker Studio is already decided.**
13. Exact Junktor storage/projection representation and whether all five candidate kinds belong in the first implementation. **The canonical term `Junktor` is already decided.**

## Recommended execution order

Do not implement all of this at once.

1. Finish current Code-vs-Contract red-zone audit.
2. Inventory AI/learning/VAL/ShapeMaker/Junktor prompts, skills, docs and code.
3. Freeze vocabulary and authority boundaries.
4. Define Learning Architecture + Dataset Lifecycle + Model Registry.
5. Define Intelligence Runtime + Model Routing + structured results.
6. Define ecological recording / active learning loop.
7. Define individual LLM, Function Learning, ML and YOLO training contracts.
8. Bind them to WSS/VAL projections and integrated ShapeMaker assets.
9. Add evaluation suites before production model promotion.

This ordering keeps the project from accidentally turning "AI integration" into a second runtime, a second database, a second Worldview and a surprisingly confident fifth editor.
