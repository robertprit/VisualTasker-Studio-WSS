package com.visualtasker.chartgraph.demo.indicators

import kotlin.math.*

// ══════════════════════════════════════════════════════════════════
// Series<T> - Zeitreihen-Datentyp (wie Pine Script's series)
// ══════════════════════════════════════════════════════════════════
class Series<T>(private val values: MutableList<T> = mutableListOf()) {
    val last: T? get() = values.lastOrNull()
    val size: Int get() = values.size

    operator fun get(index: Int): T = values[index]
    operator fun plus(value: T) = apply { values.add(value) }

    fun tail(n: Int = 1): T? = values.takeLast(n).lastOrNull()
    fun history(offset: Int): T? = if (offset < values.size) values[values.size - 1 - offset] else null

    fun toDoubleList(): List<Double> = values.map {
        when (it) {
            is Double -> it
            is Float -> it.toDouble()
            is Int -> it.toDouble()
            else -> 0.0
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Indicator Context - Wird an jedes Script übergeben
// ═══════════════════════════════════════════════════════════════════
data class IndicatorContext(
    val close: Series<Double>,
    val open: Series<Double>,
    val high: Series<Double>,
    val low: Series<Double>,
    val volume: Series<Double>,
    val time: Series<Long>
)

// ═══════════════════════════════════════════════════════════════════
// Plot Definition
// ═══════════════════════════════════════════════════════════════════
data class Plot(
    val name: String,
    val values: Series<Double>,
    val color: Long,
    val style: PlotStyle = PlotStyle.LINE,
    val lineWidth: Int = 2
)

enum class PlotStyle { LINE, AREA, COLUMN, STEP }

// ═══════════════════════════════════════════════════════════════════
// Indicator Result
// ═══════════════════════════════════════════════════════════════════
data class IndicatorResult(
    val plots: List<Plot>,
    val alerts: List<Alert>,
    val metadata: Map<String, String> = emptyMap()
)

data class Alert(
    val message: String,
    val severity: AlertSeverity = AlertSeverity.INFO,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AlertSeverity { INFO, WARNING, CRITICAL }

// ═══════════════════════════════════════════════════════════════════
// Indicator Script Interface
// ═══════════════════════════════════════════════════════════════════
fun interface IndicatorScript {
    fun execute(ctx: IndicatorContext): IndicatorResult
}

// ═══════════════════════════════════════════════════════════════════
// Helper Functions (Pine Script-ähnlich)
// ═══════════════════════════════════════════════════════════════════

// Moving Averages
fun sma(source: Series<Double>, length: Int): Double {
    if (source.size < length) return Double.NaN
    return source.toDoubleList().takeLast(length).average()
}

fun ema(source: Series<Double>, length: Int): Double {
    if (source.size < length) return Double.NaN
    val k = 2.0 / (length + 1)
    val values = source.toDoubleList().takeLast(length)

    var ema = values.first()
    for (i in 1 until values.size) {
        ema = values[i] * k + ema * (1 - k)
    }
    return ema
}

fun wma(source: Series<Double>, length: Int): Double {
    if (source.size < length) return Double.NaN
    val values = source.toDoubleList().takeLast(length)
    val weightedSum = values.mapIndexed { i, v -> v * (i + 1) }.sum()
    val weightSum = (1..length).sum()
    return weightedSum / weightSum
}

// RSI
fun rsi(source: Series<Double>, length: Int = 14): Double {
    if (source.size < length + 1) return 50.0

    val changes = mutableListOf<Double>()
    val values = source.toDoubleList()

    for (i in 1 until values.size) {
        changes.add(values[i] - values[i - 1])
    }

    val gains = changes.filter { it > 0 }
    val losses = changes.filter { it < 0 }.map { abs(it) }

    val avgGain = if (gains.isNotEmpty()) gains.average() else 0.0
    val avgLoss = if (losses.isNotEmpty()) losses.average() else 0.0

    return if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + avgGain / avgLoss))
}

// MACD
fun macd(source: Series<Double>, fast: Int = 12, slow: Int = 26, signal: Int = 9): Triple<Double, Double, Double> {
    val fastEma = ema(source, fast)
    val slowEma = ema(source, slow)
    val macdLine = fastEma - slowEma

    // Signal Line (EMA des MACD)
    // Vereinfacht: Wir brauchen eine History der MACD-Werte
    val signalLine = macdLine * 0.9 // Platzhalter

    val histogram = macdLine - signalLine

    return Triple(macdLine, signalLine, histogram)
}

// Bollinger Bands
fun bollingerBands(source: Series<Double>, length: Int = 20, mult: Double = 2.0): Triple<Double, Double, Double> {
    val basis = sma(source, length)
    val values = source.toDoubleList().takeLast(length)
    val deviation = sqrt(values.map { (it - basis).pow(2) }.average())

    val upper = basis + mult * deviation
    val lower = basis - mult * deviation

    return Triple(upper, basis, lower)
}

// ATR (Average True Range)
fun atr(high: Series<Double>, low: Series<Double>, close: Series<Double>, length: Int = 14): Double {
    if (high.size < length + 1) return 0.0

    val trValues = mutableListOf<Double>()

    for (i in 0 until minOf(high.size, length)) {
        val h = high[i]
        val l = low[i]
        val c = if (close.size > i) close[i] else l

        val tr1 = h - l
        val tr2 = abs(h - (close.history(i + 1) ?: c))
        val tr3 = abs(l - (close.history(i + 1) ?: c))

        trValues.add(maxOf(tr1, tr2, tr3))
    }

    return trValues.average()
}

// Condition Functions
fun crossover(series1: Series<Double>, series2: Series<Double>): Boolean {
    if (series1.size < 2 || series2.size < 2) return false
    val curr1 = series1.last ?: return false
    val curr2 = series2.last ?: return false
    val prev1 = series1.history(1) ?: return false
    val prev2 = series2.history(1) ?: return false

    return prev1 <= prev2 && curr1 > curr2
}

fun crossunder(series1: Series<Double>, series2: Series<Double>): Boolean {
    if (series1.size < 2 || series2.size < 2) return false
    val curr1 = series1.last ?: return false
    val curr2 = series2.last ?: return false
    val prev1 = series1.history(1) ?: return false
    val prev2 = series2.history(1) ?: return false

    return prev1 >= prev2 && curr1 < curr2
}

// Math Helpers
fun Series<Double>.change(): Double {
    if (size < 2) return 0.0
    return (last ?: 0.0) - (history(1) ?: 0.0)
}

fun Series<Double>.percentChange(): Double {
    if (size < 2) return 0.0
    val curr = last ?: 0.0
    val prev = history(1) ?: curr
    return if (prev == 0.0) 0.0 else ((curr - prev) / prev) * 100
}