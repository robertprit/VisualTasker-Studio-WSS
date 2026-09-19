package com.visualtasker.chartgraph.demo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visualtasker.chartgraph.demo.data.ChartType
import com.visualtasker.chartgraph.demo.data.MarketDataMode
import com.visualtasker.chartgraph.demo.data.MarketInterval
import com.visualtasker.chartgraph.demo.data.MarketRangePreset
import com.visualtasker.chartgraph.demo.ui.components.AggrChart
import com.visualtasker.chartgraph.demo.ui.components.AlertPanel
import com.visualtasker.chartgraph.demo.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val chartType by viewModel.chartType.collectAsState()
    val marketInterval by viewModel.marketInterval.collectAsState()
    val rangePreset by viewModel.rangePreset.collectAsState()
    val fromInput by viewModel.customFromInput.collectAsState()
    val toInput by viewModel.customToInput.collectAsState()
    val activeRange by viewModel.activeRange.collectAsState()
    val marketDataMode by viewModel.marketDataMode.collectAsState()
    val providerStatus by viewModel.providerStatus.collectAsState()
    val rangeError by viewModel.rangeError.collectAsState()
    val datasets by viewModel.datasets.collectAsState()
    val candleData by viewModel.candleData.collectAsState()
    val pointCount = datasets.firstOrNull()?.points?.size ?: 0

    Scaffold(
        containerColor = Color(0xFF121212), // Aggr Dark Background
        topBar = {
            TopAppBar(
                title = {
                    Text("AGGR COMPOSE", color = Color.White, fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Chart Type Selector (Segmented Button Style)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1E1E)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ChartType.entries.forEach { type ->
                        FilterChip(
                            selected = chartType == type,
                            onClick = { viewModel.setChartType(type) },
                            label = {
                                Text(
                                    when(type) {
                                        ChartType.LINE -> "Kurve"
                                        ChartType.COLUMN -> "Balken"
                                        ChartType.PIE -> "Torte"
                                        ChartType.DONUT -> "Donut"
                                        ChartType.CANDLE -> "Kerzen"
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // BTC Market Controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1E1E)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("BTC Market Demo", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = marketDataMode == MarketDataMode.SYNTHETIC,
                            onClick = { viewModel.setMarketDataMode(MarketDataMode.SYNTHETIC) },
                            label = { Text("SYNTHETIC") }
                        )
                        FilterChip(
                            selected = marketDataMode == MarketDataMode.LIVE,
                            onClick = { viewModel.setMarketDataMode(MarketDataMode.LIVE) },
                            label = { Text("LIVE") }
                        )
                    }

                    Text("Provider: $providerStatus", color = Color.Gray, style = MaterialTheme.typography.labelSmall)

                    Text("Intervall", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MarketInterval.entries.forEach { interval ->
                            FilterChip(
                                selected = marketInterval == interval,
                                onClick = { viewModel.setMarketInterval(interval) },
                                label = { Text(interval.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Text("Zeitraum", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MarketRangePreset.entries.filter { it != MarketRangePreset.CUSTOM }.forEach { preset ->
                            FilterChip(
                                selected = rangePreset == preset,
                                onClick = { viewModel.setRangePreset(preset) },
                                label = { Text(preset.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Text(
                        "From/To (Unix-Sekunden)",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = fromInput,
                            onValueChange = viewModel::updateCustomFromInput,
                            label = { Text("From") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = toInput,
                            onValueChange = viewModel::updateCustomToInput,
                            label = { Text("To") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Button(onClick = { viewModel.applyCustomRange() }) {
                        Text("Custom Range anwenden")
                    }
                    rangeError?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Chart Container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A1A1A),
                tonalElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AggrChart(
                        type = chartType,
                        datasets = datasets,
                        candleData = candleData,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Live Indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(
                                if (marketDataMode == MarketDataMode.LIVE) Color(0xFF00E5FF).copy(alpha = 0.2f)
                                else Color(0xFFFFB300).copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (marketDataMode == MarketDataMode.LIVE) Color(0xFF00E5FF) else Color(0xFFFFB300),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (marketDataMode == MarketDataMode.LIVE) "LIVE" else "DEMO",
                                color = if (marketDataMode == MarketDataMode.LIVE) Color(0xFF00E5FF) else Color(0xFFFFB300),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            AlertPanel(viewModel = viewModel)

            // Legend / Data Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                datasets.forEach { dataset ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(dataset.color), shape = RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            dataset.label,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Text(
                "Range: ${activeRange.startMillis / 1000} - ${activeRange.endMillis / 1000}   |   Interval: ${marketInterval.label}   |   Points: $pointCount",
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
