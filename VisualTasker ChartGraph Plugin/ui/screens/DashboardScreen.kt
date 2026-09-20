package com.visualtasker.chartgraph.demo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visualtasker.chartgraph.demo.data.ChartType
import com.visualtasker.chartgraph.demo.data.MarketDataMode
import com.visualtasker.chartgraph.demo.data.MarketInterval
import com.visualtasker.chartgraph.demo.data.MarketRangePreset
import com.visualtasker.chartgraph.demo.ui.components.AggrChart
import com.visualtasker.chartgraph.demo.ui.components.AlertPanel
import com.visualtasker.chartgraph.demo.ui.components.label
import com.visualtasker.chartgraph.demo.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
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
    val chartOrder = remember { mutableStateListOf<ChartType>().apply { addAll(ChartType.entries) } }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

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

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(chartOrder, key = ChartType::name) { type ->
                    Card(
                        modifier = Modifier.fillMaxWidth().height(250.dp).animateItem(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = "Diagramm verschieben",
                                modifier = Modifier.size(32.dp).pointerInput(type, chartOrder.toList()) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { dragAccumulator = 0f },
                                        onDragCancel = { dragAccumulator = 0f },
                                        onDragEnd = { dragAccumulator = 0f },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragAccumulator += amount.y
                                            val current = chartOrder.indexOf(type)
                                            val direction = when {
                                                dragAccumulator > 72f -> 1
                                                dragAccumulator < -72f -> -1
                                                else -> 0
                                            }
                                            val target = (current + direction).coerceIn(chartOrder.indices)
                                            if (direction != 0 && target != current) {
                                                chartOrder.removeAt(current)
                                                chartOrder.add(target, type)
                                                dragAccumulator = 0f
                                            }
                                        },
                                    )
                                },
                            )
                            Text(type.label, modifier = Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.titleSmall)
                            Text(if (marketDataMode == MarketDataMode.LIVE) "LIVE" else "DEMO", color = if (marketDataMode == MarketDataMode.LIVE) Color(0xFF00E5FF) else Color(0xFFFFB300), style = MaterialTheme.typography.labelSmall)
                        }
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    AggrChart(
                        type = type,
                        datasets = datasets,
                        candleData = candleData,
                        modifier = Modifier.fillMaxSize()
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
