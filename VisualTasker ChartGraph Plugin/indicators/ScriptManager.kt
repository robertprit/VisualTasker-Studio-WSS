package com.visualtasker.chartgraph.demo.indicators

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScriptManager {
    private val _activeScripts = MutableStateFlow<Map<String, IndicatorScript>>(emptyMap())
    val activeScripts: StateFlow<Map<String, IndicatorScript>> = _activeScripts.asStateFlow()

    private val _results = MutableStateFlow<Map<String, IndicatorResult>>(emptyMap())
    val results: StateFlow<Map<String, IndicatorResult>> = _results.asStateFlow()

    // Builtin Scripts registrieren
    init {
        registerBuiltinScripts()
    }

    private fun registerBuiltinScripts() {
        _activeScripts.value = mapOf(
            "RSI" to RSIScript,
            "MACD" to MACDScript,
            "Bollinger" to BollingerBandsScript,
            "SMA_Cross" to SMACrossoverStrategy
        )
    }

    fun executeAllScripts(context: IndicatorContext) {
        val newResults = mutableMapOf<String, IndicatorResult>()

        _activeScripts.value.forEach { (name, script) ->
            try {
                newResults[name] = script.execute(context)
            } catch (e: Exception) {
                newResults[name] = IndicatorResult(
                    plots = emptyList(),
                    alerts = listOf(Alert("Script Error: ${e.message}", AlertSeverity.CRITICAL))
                )
            }
        }

        _results.value = newResults
    }

    fun registerScript(name: String, script: IndicatorScript) {
        _activeScripts.value = _activeScripts.value + (name to script)
    }

    fun unregisterScript(name: String) {
        _activeScripts.value = _activeScripts.value - name
    }

    fun getAlerts(): List<Alert> {
        return _results.value.values.flatMap { it.alerts }
    }
}