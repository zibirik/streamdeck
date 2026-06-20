package com.streampanel.feature.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streampanel.core.designsystem.AppBackdrop
import com.streampanel.core.designsystem.GlassSurface
import com.streampanel.core.designsystem.StreamPanelTextField
import com.streampanel.core.network.ConnectionStatus

@Composable
fun ConnectionsRoute(
    onBack: () -> Unit,
    viewModel: ConnectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ConnectionsScreen(
        state = state,
        onBack = onBack,
        onHostChanged = viewModel::updateHost,
        onPortChanged = viewModel::updatePort,
        onPinChanged = viewModel::updatePin,
        onConnect = viewModel::saveAndConnect,
        onConnectNow = viewModel::connectWithoutSave,
        onDisconnect = viewModel::disconnect,
        onScan = viewModel::scanNetwork,
        onSelectDiscovered = viewModel::selectDiscovered,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(
    state: ConnectionsUiState,
    onBack: () -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onPinChanged: (String) -> Unit,
    onConnect: () -> Unit,
    onConnectNow: () -> Unit,
    onDisconnect: () -> Unit,
    onScan: () -> Unit,
    onSelectDiscovered: (com.streampanel.core.network.DiscoveredServer) -> Unit,
) {
    AppBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("PC Companion", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                GlassSurface(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Connection", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text(connectionLabel(state.status), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StreamPanelTextField(
                            value = state.hostDraft,
                            onValueChange = onHostChanged,
                            label = "Server host",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        StreamPanelTextField(
                            value = state.portDraft,
                            onValueChange = onPortChanged,
                            label = "Port",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        StreamPanelTextField(
                            value = state.pinDraft,
                            onValueChange = onPinChanged,
                            label = "PIN (if server requires it)",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "PIN is optional. Set the same value in Windows appsettings.json " +
                                "(StreamPanel:Pin) to require authentication from this tablet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onConnect) { Text("Save & Connect") }
                            OutlinedButton(onClick = onConnectNow) { Text("Connect now", color = Color.White) }
                            OutlinedButton(onClick = onDisconnect) { Text("Disconnect", color = Color.White) }
                        }
                    }
                }

                GlassSurface(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("LAN discovery", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            if (state.scanning) {
                                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                            } else {
                                OutlinedButton(onClick = onScan) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                                    Text(" Scan", color = Color.White)
                                }
                            }
                        }
                        if (state.discoveredServers.isEmpty()) {
                            Text(
                                "Tap Scan to find StreamPanel servers on your Wi‑Fi",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            state.discoveredServers.forEach { server ->
                                AssistChip(
                                    onClick = { onSelectDiscovered(server) },
                                    label = { Text("${server.machineName} (${server.host})") },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun connectionLabel(status: ConnectionStatus): String =
    when (status) {
        ConnectionStatus.Connecting -> "Connecting..."
        ConnectionStatus.Disconnected -> "Disconnected"
        is ConnectionStatus.Connected -> "Connected to ${status.url}"
        is ConnectionStatus.Failed -> "Failed: ${status.reason}"
    }
