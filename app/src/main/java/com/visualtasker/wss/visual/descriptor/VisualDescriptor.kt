package com.visualtasker.wss.visual.descriptor

enum class ShapeRole {
    Capsule,
    RoundedRect,
    Diamond,
    Hexagon,
    NotchedValue,
    GroupRegion,
    Connector,
    Row,
}

enum class SurfaceRole {
    Event,
    Action,
    Condition,
    Loop,
    Value,
    WorldEntity,
    Observation,
    Resource,
    Ambiguity,
    Runtime,
    Group,
    Neutral,
}

enum class OutlineRole {
    None,
    Focused,
    Selected,
    Proposal,
    Warning,
    Invalid,
    RuntimeActive,
}

enum class MotionRole {
    None,
    RuntimePulse,
    RuntimeFlow,
    AttentionPulse,
}

enum class OpacityRole {
    Normal,
    Muted,
    Disabled,
    Historical,
}

enum class Emphasis {
    Low,
    Normal,
    High,
    Critical,
}

enum class ConnectorRole {
    ControlFlow,
    ValueFlow,
    Branch,
    LoopBack,
    Relation,
    Conflict,
}

data class VisualBadge(
    val role: BadgeRole,
    val label: String? = null,
)

enum class BadgeRole {
    AiProposal,
    Ambiguous,
    Warning,
    Invalid,
    Running,
    Blocked,
    Stale,
}

data class VisualDescriptor(
    val shapeRole: ShapeRole,
    val surfaceRole: SurfaceRole,
    val outlineRole: OutlineRole = OutlineRole.None,
    val motionRole: MotionRole = MotionRole.None,
    val opacityRole: OpacityRole = OpacityRole.Normal,
    val emphasis: Emphasis = Emphasis.Normal,
    val badges: List<VisualBadge> = emptyList(),
)

data class EdgeVisualDescriptor(
    val connectorRole: ConnectorRole,
    val outlineRole: OutlineRole = OutlineRole.None,
    val motionRole: MotionRole = MotionRole.None,
    val emphasis: Emphasis = Emphasis.Normal,
    val badges: List<VisualBadge> = emptyList(),
)
