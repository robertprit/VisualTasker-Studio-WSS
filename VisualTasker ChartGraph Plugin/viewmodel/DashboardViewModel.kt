package com.visualtasker.chartgraph.demo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visualtasker.chartgraph.demo.data.CandleDataPoint
import com.visualtasker.chartgraph.demo.data.ChartDataset
import com.visualtasker.chartgraph.demo.data.ChartType
import com.visualtasker.chartgraph.demo.data.DataPoint
import com.visualtasker.chartgraph.demo.data.MarketDataMode
import com.visualtasker.chartgraph.demo.data.MarketInterval
import com.visualtasker.chartgraph.demo.data.MarketRangePreset
import com.visualtasker.chartgraph.demo.data.MarketTimeRange
import com.visualtasker.chartgraph.demo.indicators.Alert
import com.visualtasker.chartgraph.demo.indicators.IndicatorContext
import com.visualtasker.chartgraph.demo.indicators.IndicatorResult
import com.visualtasker.chartgraph.demo.indicators.ScriptManager
import com.visualtasker.chartgraph.demo.indicators.Series
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URL
import kotlin.random.Random

class DashboardViewModel : ViewModel() {

    private val _chartType = MutableStateFlow(ChartType.LINE)
    val chartType: StateFlow<ChartType> = _chartType.asStateFlow()
    private val _marketInterval = MutableStateFlow(MarketInterval.SEC_5)
    val marketInterval: StateFlow<MarketInterval> = _marketInterval.asStateFlow()
    private val _rangePreset = MutableStateFlow(MarketRangePreset.HOUR_1)
    val rangePreset: StateFlow<MarketRangePreset> = _rangePreset.asStateFlow()
    private val _customFromInput = MutableStateFlow("")
    val customFromInput: StateFlow<String> = _customFromInput.asStateFlow()
    private val _customToInput = MutableStateFlow("")
    val customToInput: StateFlow<String> = _customToInput.asStateFlow()
    private val _activeRange = MutableStateFlow(currentDefaultRange())
    val activeRange: StateFlow<MarketTimeRange> = _activeRange.asStateFlow()
    private val _marketDataMode = MutableStateFlow(MarketDataMode.SYNTHETIC)
    val marketDataMode: StateFlow<MarketDataMode> = _marketDataMode.asStateFlow()
    private val _providerStatus = MutableStateFlow("SYNTHETIC DEMO")
    val providerStatus: StateFlow<String> = _providerStatus.asStateFlow()
    private val _rangeError = MutableStateFlow<String?>(null)
    val rangeError: StateFlow<String?> = _rangeError.asStateFlow()

    private val _datasets = MutableStateFlow(
        listOf(
            ChartDataset(id = "btc", label = "BTC/USD", color = 0xFF00E5FF, points = emptyList()),
            ChartDataset(id = "eth", label = "ETH/USD", color = 0xFFFF007F, points = emptyList())
        )
    )
    val datasets: StateFlow<List<ChartDataset>> = _datasets.asStateFlow()
    private val _candleData = MutableStateFlow<List<CandleDataPoint>>(emptyList())
    val candleData: StateFlow<List<CandleDataPoint>> = _candleData.asStateFlow()
    private val scriptManager = ScriptManager()
    val scriptResults: StateFlow<Map<String, IndicatorResult>> = scriptManager.results
    val activeAlerts: StateFlow<List<Alert>> = scriptResults
        .map { results -> results.values.flatMap { it.alerts } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        applyPresetRange()
        regenerateSyntheticHistory()
        startUnifiedStream()
    }

    fun setChartType(type: ChartType) {
        _chartType.value = type
    }

    fun setMarketInterval(interval: MarketInterval) {
        _marketInterval.value = interval
        regenerateSyntheticHistory()
    }

    fun setRangePreset(preset: MarketRangePreset) {
        _rangePreset.value = preset
        _rangeError.value = null
        if (preset != MarketRangePreset.CUSTOM) {
            applyPresetRange()
            regenerateSyntheticHistory()
        }
    }

    fun updateCustomFromInput(value: String) {
        _customFromInput.value = value
    }

    fun updateCustomToInput(value: String) {
        _customToInput.value = value
    }

    fun applyCustomRange() {
        val fromSec = _customFromInput.value.toLongOrNull()
        val toSec = _customToInput.value.toLongOrNull()
        if (fromSec == null || toSec == null) {
            _rangeError.value = "From/To muss als Unix-Zeit in Sekunden angegeben werden."
            return
        }
        val fromMs = fromSec * 1_000L
        val toMs = toSec * 1_000L
        if (fromMs >= toMs) {
            _rangeError.value = "From muss kleiner als To sein."
            return
        }
        _rangePreset.value = MarketRangePreset.CUSTOM
        _activeRange.value = MarketTimeRange(fromMs, toMs)
        _rangeError.value = null
        regenerateSyntheticHistory()
    }

    fun setMarketDataMode(mode: MarketDataMode) {
        _marketDataMode.value = mode
        _providerStatus.value = if (mode == MarketDataMode.SYNTHETIC) "SYNTHETIC DEMO" else "LIVE (Coinbase)"
        if (mode == MarketDataMode.SYNTHETIC) {
            regenerateSyntheticHistory()
        }
    }

    private fun startUnifiedStream() {
        viewModelScope.launch(Dispatchers.Default) {
            var syntheticBtcPrice = 50f
            var syntheticEthPrice = 30f
            while (true) {
                val currentTime = System.currentTimeMillis()
                val intervalMillis = _marketInterval.value.millis

                if (_rangePreset.value != MarketRangePreset.CUSTOM) {
                    applyPresetRange(now = currentTime)
                }

                val currentRange = _activeRange.value
                val pointLimit = pointLimitFor(currentRange, _marketInterval.value)

                if (_marketDataMode.value == MarketDataMode.SYNTHETIC) {
                    syntheticBtcPrice += Random.nextFloat() * 6f - 3f
                    syntheticEthPrice += Random.nextFloat() * 4f - 2f
                    syntheticBtcPrice = syntheticBtcPrice.coerceAtLeast(1f)
                    syntheticEthPrice = syntheticEthPrice.coerceAtLeast(1f)

                    appendMarketTick(currentTime, syntheticBtcPrice, syntheticEthPrice, pointLimit, currentRange)
                    appendCandleTick(currentTime, syntheticBtcPrice, pointLimit, currentRange)
                    _providerStatus.value = "SYNTHETIC DEMO"
                } else {
                    val btcLive = fetchSpotPrice("BTC-USD")
                    val ethLive = fetchSpotPrice("ETH-USD")
                    if (btcLive == null || ethLive == null) {
                        _providerStatus.value = "Live data unavailable"
                    } else {
                        _providerStatus.value = "LIVE (Coinbase)"
                        appendMarketTick(currentTime, btcLive, ethLive, pointLimit, currentRange)
                        appendCandleTick(currentTime, btcLive, pointLimit, currentRange)
                    }
                }
                scriptManager.executeAllScripts(createIndicatorContext())
                delay(intervalMillis)
            }
        }
    }

    private fun regenerateSyntheticHistory() {
        val now = System.currentTimeMillis()
        if (_rangePreset.value != MarketRangePreset.CUSTOM) {
            applyPresetRange(now)
        }
        val range = _activeRange.value
        val interval = _marketInterval.value
        val pointLimit = pointLimitFor(range, interval)

        val btcSeries = mutableListOf<DataPoint>()
        val ethSeries = mutableListOf<DataPoint>()
        val candles = mutableListOf<CandleDataPoint>()

        var btc = 50f
        var eth = 30f
        val startTime = range.endMillis - ((pointLimit - 1) * interval.millis)

        repeat(pointLimit) { idx ->
            val ts = startTime + (idx * interval.millis)
            val phase = idx / 9.0
            val btcDrift = kotlin.math.sin(phase).toFloat() * 1.1f + (Random.nextFloat() * 1.2f - 0.6f)
            val ethDrift = kotlin.math.cos(phase * 0.8).toFloat() * 0.8f + (Random.nextFloat() * 0.8f - 0.4f)
            btc = (btc + btcDrift).coerceAtLeast(1f)
            eth = (eth + ethDrift).coerceAtLeast(1f)

            btcSeries += DataPoint(ts, btc)
            ethSeries += DataPoint(ts, eth)

            val open = btc
            val close = (btc + (Random.nextFloat() * 1.6f - 0.8f)).coerceAtLeast(1f)
            val high = maxOf(open, close) + Random.nextFloat() * 0.9f
            val low = minOf(open, close) - Random.nextFloat() * 0.9f
            candles += CandleDataPoint(ts, open, high, low, close)
            btc = close
        }

        _datasets.value = listOf(
            ChartDataset(id = "btc", label = "BTC/USD", color = 0xFF00E5FF, points = btcSeries),
            ChartDataset(id = "eth", label = "ETH/USD", color = 0xFFFF007F, points = ethSeries)
        )
        _candleData.value = candles
    }

    private fun appendMarketTick(
        currentTime: Long,
        btcPrice: Float,
        ethPrice: Float,
        pointLimit: Int,
        range: MarketTimeRange
    ) {
        _datasets.update { currentDatasets ->
            currentDatasets.map { dataset ->
                val nextValue = when (dataset.id) {
                    "btc" -> btcPrice
                    "eth" -> ethPrice
                    else -> dataset.points.lastOrNull()?.y ?: 0f
                }
                val bounded = (dataset.points + DataPoint(currentTime, nextValue))
                    .filter { it.x in range.startMillis..range.endMillis }
                    .takeLast(pointLimit)
                dataset.copy(points = bounded)
            }
        }
    }

    private fun appendCandleTick(
        currentTime: Long,
        closePrice: Float,
        pointLimit: Int,
        range: MarketTimeRange
    ) {
        _candleData.update { currentList ->
            val previousClose = currentList.lastOrNull()?.close ?: closePrice
            val open = previousClose
            val close = closePrice
            val high = maxOf(open, close) + Random.nextFloat() * 0.8f
            val low = minOf(open, close) - Random.nextFloat() * 0.8f
            (currentList + CandleDataPoint(currentTime, open, high, low, close))
                .filter { it.x in range.startMillis..range.endMillis }
                .takeLast(pointLimit)
        }
    }

    private fun currentDefaultRange(now: Long = System.currentTimeMillis()): MarketTimeRange {
        return MarketTimeRange(
            startMillis = now - (60 * 60_000L),
            endMillis = now
        )
    }

    private fun applyPresetRange(now: Long = System.currentTimeMillis()) {
        val preset = _rangePreset.value
        if (preset == MarketRangePreset.CUSTOM) return
        val duration = preset.durationMillis
        _activeRange.value = if (duration == null) {
            val oldest = _datasets.value.flatMap { it.points }.minOfOrNull { it.x } ?: now - (365L * 24 * 60 * 60_000L)
            MarketTimeRange(oldest, now)
        } else {
            MarketTimeRange(now - duration, now)
        }
    }

    private fun pointLimitFor(range: MarketTimeRange, interval: MarketInterval): Int {
        val duration = (range.endMillis - range.startMillis).coerceAtLeast(interval.millis)
        return ((duration / interval.millis) + 1L)
            .coerceAtLeast(20L)
            .coerceAtMost(10_000L)
            .toInt()
    }

    private fun fetchSpotPrice(product: String): Float? {
        val endpoint = "https://api.coinbase.com/v2/prices/$product/spot"
        return runCatching {
            val body = URL(endpoint).readText()
            val match = """"amount":"([0-9.]+)"""".toRegex().find(body)
            match?.groupValues?.get(1)?.toFloat()
        }.getOrNull()
    }

    private fun createEmptyContext(): IndicatorContext {
        return IndicatorContext(
            close = Series(),
            open = Series(),
            high = Series(),
            low = Series(),
            volume = Series(),
            time = Series()
        )
    }

    private fun createSyntheticSeries(base: Double): Triple<Double, Double, Double> {
        val open = base * (0.99 + Random.nextDouble(0.02))
        val high = base * (1.0 + Random.nextDouble(0.02))
        val low = base * (0.98 + Random.nextDouble(0.01))
        return Triple(open, high, low)
    }

    private fun createIndicatorContext(): IndicatorContext {
        val baseDataset = _datasets.value.firstOrNull() ?: return createEmptyContext()
        if (baseDataset.points.isEmpty()) return createEmptyContext()

        val closeSeries = Series<Double>()
        val openSeries = Series<Double>()
        val highSeries = Series<Double>()
        val lowSeries = Series<Double>()
        val volumeSeries = Series<Double>()
        val timeSeries = Series<Long>()

        baseDataset.points.forEach { point ->
            val close = point.y.toDouble()
            val (open, high, low) = createSyntheticSeries(close)
            closeSeries + close
            openSeries + open
            highSeries + high
            lowSeries + low
            volumeSeries + Random.nextDouble(1_000.0, 10_000.0)
            timeSeries + point.x
        }

        return IndicatorContext(
            close = closeSeries,
            open = openSeries,
            high = highSeries,
            low = lowSeries,
            volume = volumeSeries,
            time = timeSeries
        )
    }
}