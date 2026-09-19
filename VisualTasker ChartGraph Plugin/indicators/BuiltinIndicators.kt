package com.visualtasker.chartgraph.demo.indicators

// ══════════════════════════════════════════════════════════════════
// RSI Indicator
// ═══════════════════════════════════════════════════════════════════
val RSIScript: IndicatorScript = IndicatorScript { ctx ->
    val rsiValues = Series<Double>()
    val length = 14

    for (i in 0 until ctx.close.size) {
        val subset = Series<Double>()
        for (j in 0..i) {
            subset + ctx.close[j]
        }
        rsiValues + rsi(subset, length)
    }

    val alerts = mutableListOf<Alert>()
    val lastRsi = rsiValues.last ?: 50.0

    if (lastRsi > 70) {
        alerts.add(Alert("RSI überkauft: %.1f".format(lastRsi), AlertSeverity.WARNING))
    } else if (lastRsi < 30) {
        alerts.add(Alert("RSI überverkauft: %.1f".format(lastRsi), AlertSeverity.INFO))
    }

    IndicatorResult(
        plots = listOf(
            Plot("RSI", rsiValues, 0xFF00E5FF),
            Plot("Overbought", Series<Double>().apply { repeat(rsiValues.size) { this + 70.0 } }, 0xFFFF007F, PlotStyle.STEP),
            Plot("Oversold", Series<Double>().apply { repeat(rsiValues.size) { this + 30.0 } }, 0xFF00FF9D, PlotStyle.STEP)
        ),
        alerts = alerts,
        metadata = mapOf("rsi" to lastRsi.toString())
    )
}

// ═══════════════════════════════════════════════════════════════════
// MACD Indicator
// ═══════════════════════════════════════════════════════════════════
val MACDScript: IndicatorScript = IndicatorScript { ctx ->
    val macdValues = Series<Double>()
    val signalValues = Series<Double>()
    val histValues = Series<Double>()

    for (i in 0 until ctx.close.size) {
        val subset = Series<Double>()
        for (j in 0..i) {
            subset + ctx.close[j]
        }

        val (macd, signal, hist) = macd(subset)
        macdValues + macd
        signalValues + signal
        histValues + hist
    }

    val alerts = mutableListOf<Alert>()

    if (crossover(macdValues, signalValues)) {
        alerts.add(Alert("MACD Bullish Crossover", AlertSeverity.INFO))
    } else if (crossunder(macdValues, signalValues)) {
        alerts.add(Alert("MACD Bearish Crossover", AlertSeverity.WARNING))
    }

    IndicatorResult(
        plots = listOf(
            Plot("MACD", macdValues, 0xFF00E5FF),
            Plot("Signal", signalValues, 0xFFFF007F),
            Plot("Histogram", histValues, 0xFF00FF9D, PlotStyle.COLUMN)
        ),
        alerts = alerts
    )
}

// ═══════════════════════════════════════════════════════════════════
// Bollinger Bands
// ═══════════════════════════════════════════════════════════════════
val BollingerBandsScript: IndicatorScript = IndicatorScript { ctx ->
    val upper = Series<Double>()
    val middle = Series<Double>()
    val lower = Series<Double>()

    for (i in 0 until ctx.close.size) {
        val subset = Series<Double>()
        for (j in 0..i) {
            subset + ctx.close[j]
        }

        val (u, m, l) = bollingerBands(subset)
        upper + u
        middle + m
        lower + l
    }

    IndicatorResult(
        plots = listOf(
            Plot("Upper", upper, 0xFFFF007F, PlotStyle.LINE, 1),
            Plot("Middle", middle, 0xFF00E5FF),
            Plot("Lower", lower, 0xFF00FF9D, PlotStyle.LINE, 1)
        ),
        alerts = emptyList()
    )
}

// ═══════════════════════════════════════════════════════════════════
// Custom Strategy: SMA Crossover
// ═══════════════════════════════════════════════════════════════════
val SMACrossoverStrategy: IndicatorScript = IndicatorScript { ctx ->
    val fastLength = 9
    val slowLength = 21

    val fastSMA = Series<Double>()
    val slowSMA = Series<Double>()

    for (i in 0 until ctx.close.size) {
        val subset = Series<Double>()
        for (j in 0..i) {
            subset + ctx.close[j]
        }

        fastSMA + (if (subset.size >= fastLength) sma(subset, fastLength) else Double.NaN)
        slowSMA + (if (subset.size >= slowLength) sma(subset, slowLength) else Double.NaN)
    }

    val alerts = mutableListOf<Alert>()

    if (crossover(fastSMA, slowSMA)) {
        alerts.add(Alert("BUY Signal: SMA $fastLength crossed above SMA $slowLength", AlertSeverity.INFO))
    } else if (crossunder(fastSMA, slowSMA)) {
        alerts.add(Alert("SELL Signal: SMA $fastLength crossed below SMA $slowLength", AlertSeverity.WARNING))
    }

    IndicatorResult(
        plots = listOf(
            Plot("Close", ctx.close, 0xFFFFFFFF),
            Plot("SMA Fast ($fastLength)", fastSMA, 0xFF00E5FF),
            Plot("SMA Slow ($slowLength)", slowSMA, 0xFFFF007F)
        ),
        alerts = alerts
    )
}