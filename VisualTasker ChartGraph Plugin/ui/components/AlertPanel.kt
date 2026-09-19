package com.visualtasker.chartgraph.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.visualtasker.chartgraph.demo.indicators.Alert
import com.visualtasker.chartgraph.demo.indicators.AlertSeverity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visualtasker.chartgraph.demo.viewmodel.DashboardViewModel

@Composable
fun AlertPanel(viewModel: DashboardViewModel = viewModel()) {
    val alerts by viewModel.activeAlerts.collectAsState()

    if (alerts.isEmpty()) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp),
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    " Alerts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "${alerts.size} aktiv",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(alerts) { alert ->
                    AlertItem(alert)
                }
            }
        }
    }
}

@Composable
private fun AlertItem(alert: Alert) {
    val (bgColor, icon, severityText) = when (alert.severity) {
        AlertSeverity.INFO -> Triple(Color(0xFF00E5FF).copy(alpha = 0.1f), Icons.Filled.Info, "INFO")
        AlertSeverity.WARNING -> Triple(Color(0xFFFFB300).copy(alpha = 0.1f), Icons.Filled.Warning, "WARN")
        AlertSeverity.CRITICAL -> Triple(Color(0xFFFF007F).copy(alpha = 0.1f), Icons.Filled.Warning, "CRIT")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when (alert.severity) {
                AlertSeverity.INFO -> Color(0xFF00E5FF)
                AlertSeverity.WARNING -> Color(0xFFFFB300)
                AlertSeverity.CRITICAL -> Color(0xFFFF007F)
            },
            modifier = Modifier.size(20.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                alert.message,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            Text(
                alert.timestamp.toString().takeLast(12),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }

        Text(
            severityText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = when (alert.severity) {
                AlertSeverity.INFO -> Color(0xFF00E5FF)
                AlertSeverity.WARNING -> Color(0xFFFFB300)
                AlertSeverity.CRITICAL -> Color(0xFFFF007F)
            }
        )
    }
}