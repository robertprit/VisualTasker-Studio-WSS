package com.visualtasker.wss.emscript.runtime

import de.visualtasker.emscript.contract.CoreTypes
import de.visualtasker.emscript.contract.DomainTypes
import de.visualtasker.emscript.contract.LanguageTypeCompatibility
import de.visualtasker.emscript.contract.LanguageTypeRef
import de.visualtasker.emscript.contract.ProviderTypes

internal object EmscriptRuntimeTypeSafety {
    const val NULLABLE_VALUE_IN_NONNULL_CONTEXT = "NULLABLE_VALUE_IN_NONNULL_CONTEXT"

    fun matches(value: EmscriptValue, expectedType: String): Boolean {
        val expected = LanguageTypeCompatibility.fromWorkspaceName(expectedType) ?: return false
        if (value === EmscriptValue.NullValue) return expected is LanguageTypeRef.Nullable
        return LanguageTypeCompatibility.isAssignable(typeOf(value), expected)
    }

    fun requireMatches(value: EmscriptValue, expectedType: String, context: String) {
        if (matches(value, expectedType)) return
        val code = if (value === EmscriptValue.NullValue) {
            NULLABLE_VALUE_IN_NONNULL_CONTEXT
        } else {
            "RUNTIME_TYPE_CONTRACT_VIOLATION"
        }
        error("$code: $context erwartet $expectedType")
    }

    fun defaultValue(expectedType: String): EmscriptValue {
        val expected = requireNotNull(LanguageTypeCompatibility.fromWorkspaceName(expectedType)) {
            "Unbekannter Runtime-Typ '$expectedType'"
        }
        if (expected is LanguageTypeRef.Nullable) return EmscriptValue.NullValue
        return when (expected) {
            CoreTypes.STRING.ref -> EmscriptValue.StringValue("")
            CoreTypes.NUMBER.ref -> EmscriptValue.NumberValue(0.0)
            CoreTypes.BOOL.ref -> EmscriptValue.BooleanValue(false)
            is LanguageTypeRef.ListOf -> EmscriptValue.ListValue(expected.elementType, emptyList())
            else -> error("Unsupported expression return type '$expectedType'")
        }
    }

    private fun typeOf(value: EmscriptValue): LanguageTypeRef = when (value) {
        is EmscriptValue.StringValue -> CoreTypes.STRING.ref
        is EmscriptValue.NumberValue -> CoreTypes.NUMBER.ref
        is EmscriptValue.BooleanValue -> CoreTypes.BOOL.ref
        is EmscriptValue.TaskerVariableValue -> ProviderTypes.TASKER_VARIABLE.ref
        is EmscriptValue.ListValue -> LanguageTypeRef.ListOf(value.elementType)
        is EmscriptValue.ImageMatchValue -> DomainTypes.IMAGE_MATCH.ref
        is EmscriptValue.TextMatchValue -> DomainTypes.TEXT_MATCH.ref
        is EmscriptValue.MarkerValue -> DomainTypes.MARKER.ref
        is EmscriptValue.ChartSnapshotValue -> DomainTypes.CHART_SNAPSHOT.ref
        EmscriptValue.NullValue -> error("Absent values require an expected nullable type")
    }
}
