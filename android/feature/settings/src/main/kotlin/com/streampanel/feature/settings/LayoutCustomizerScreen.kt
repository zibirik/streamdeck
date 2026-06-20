package com.streampanel.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.streampanel.core.designsystem.SectionHeader
import com.streampanel.core.designsystem.strings
import com.streampanel.core.model.DashboardLayoutSettings
import com.streampanel.core.model.DashboardPanelId
import com.streampanel.core.model.DashboardZone
import com.streampanel.core.model.ToolsColumnWidth

@Composable
fun LayoutCustomizerRoute(
    onBack: () -> Unit,
    viewModel: LayoutCustomizerViewModel = hiltViewModel(),
) {
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    LayoutCustomizerScreen(
        layout = layout,
        onBack = onBack,
        onMoveZone = viewModel::moveZone,
        onMoveCompactTab = viewModel::moveCompactTab,
        onMovePanel = viewModel::movePanel,
        onMovePanelToTop = viewModel::movePanelToTop,
        onMovePanelToBottom = viewModel::movePanelToBottom,
        onPanelVisibleChanged = viewModel::setPanelVisible,
        onAllPanelsVisibleChanged = viewModel::setAllPanelsVisible,
        onSidebarVisibleChanged = viewModel::setSidebarVisible,
        onToolsWidthChanged = viewModel::setToolsColumnWidth,
        onPresetSelected = viewModel::applyPreset,
        onRestoreDefaults = viewModel::restoreDefaults,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LayoutCustomizerScreen(
    layout: DashboardLayoutSettings,
    onBack: () -> Unit,
    onMoveZone: (DashboardZone, Int) -> Unit,
    onMoveCompactTab: (DashboardZone, Int) -> Unit,
    onMovePanel: (DashboardPanelId, Int) -> Unit,
    onMovePanelToTop: (DashboardPanelId) -> Unit,
    onMovePanelToBottom: (DashboardPanelId) -> Unit,
    onPanelVisibleChanged: (DashboardPanelId, Boolean) -> Unit,
    onAllPanelsVisibleChanged: (Boolean) -> Unit,
    onSidebarVisibleChanged: (Boolean) -> Unit,
    onToolsWidthChanged: (ToolsColumnWidth) -> Unit,
    onPresetSelected: (LayoutPreset) -> Unit,
    onRestoreDefaults: () -> Unit,
) {
    val s = strings()
    AppBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(s.layoutCustomization, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(s.layoutCustomizationSub, color = MaterialTheme.colorScheme.onSurfaceVariant)

                LayoutSection(s.layoutPresets, s.layoutPresetsSub) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = { onPresetSelected(LayoutPreset.Stream) },
                            label = { Text(s.layoutPresetStream) },
                        )
                        FilterChip(
                            selected = false,
                            onClick = { onPresetSelected(LayoutPreset.Work) },
                            label = { Text(s.layoutPresetWork) },
                        )
                        FilterChip(
                            selected = false,
                            onClick = { onPresetSelected(LayoutPreset.Gaming) },
                            label = { Text(s.layoutPresetGaming) },
                        )
                        FilterChip(
                            selected = false,
                            onClick = { onPresetSelected(LayoutPreset.Minimal) },
                            label = { Text(s.layoutPresetMinimal) },
                        )
                    }
                }

                LayoutSection(s.layoutPhoneTabs, s.layoutPhoneTabsSub) {
                    layout.compactTabOrder.forEachIndexed { index, zone ->
                        ReorderRow(
                            title = s.layoutZoneLabel(zone),
                            canMoveUp = index > 0,
                            canMoveDown = index < layout.compactTabOrder.lastIndex,
                            onMoveUp = { onMoveCompactTab(zone, -1) },
                            onMoveDown = { onMoveCompactTab(zone, 1) },
                        )
                    }
                }

                LayoutSection(s.layoutZones, s.layoutZonesSub) {
                    layout.zoneOrder.forEachIndexed { index, zone ->
                        ReorderRow(
                            title = s.layoutZoneLabel(zone),
                            canMoveUp = index > 0,
                            canMoveDown = index < layout.zoneOrder.lastIndex,
                            onMoveUp = { onMoveZone(zone, -1) },
                            onMoveDown = { onMoveZone(zone, 1) },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.layoutSidebarVisible, color = Color.White)
                        Switch(layout.sidebarVisible, onSidebarVisibleChanged)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToolsColumnWidth.entries.forEach { width ->
                            FilterChip(
                                selected = layout.toolsColumnWidth == width,
                                onClick = { onToolsWidthChanged(width) },
                                label = { Text(s.layoutToolsWidthLabel(width)) },
                            )
                        }
                    }
                }

                LayoutSection(s.layoutPanels, s.layoutPanelsSub) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { onAllPanelsVisibleChanged(true) }, modifier = Modifier.weight(1f)) {
                            Text(s.showAll, color = Color.White)
                        }
                        OutlinedButton(onClick = { onAllPanelsVisibleChanged(false) }, modifier = Modifier.weight(1f)) {
                            Text(s.hideAll, color = Color.White)
                        }
                    }
                    layout.panelSlots.forEachIndexed { index, slot ->
                        ReorderRow(
                            title = s.layoutPanelLabel(slot.id),
                            canMoveUp = index > 0,
                            canMoveDown = index < layout.panelSlots.lastIndex,
                            onMoveUp = { onMovePanel(slot.id, -1) },
                            onMoveDown = { onMovePanel(slot.id, 1) },
                            onMoveTop = { onMovePanelToTop(slot.id) },
                            onMoveBottom = { onMovePanelToBottom(slot.id) },
                            trailing = {
                                Switch(
                                    checked = slot.visible,
                                    onCheckedChange = { onPanelVisibleChanged(slot.id, it) },
                                )
                            },
                        )
                    }
                }

                OutlinedButton(onClick = onRestoreDefaults) {
                    Text(s.restoreDefaults, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun LayoutSection(title: String, subtitle: String, content: @Composable () -> Unit) {
    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(title = title, subtitle = subtitle)
            content()
        }
    }
}

@Composable
private fun ReorderRow(
    title: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveTop: (() -> Unit)? = null,
    onMoveBottom: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val s = strings()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Color.White, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onMoveTop != null) {
                OutlinedButton(onClick = onMoveTop, enabled = canMoveUp) {
                    Text(s.moveTop)
                }
            }
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Default.ArrowUpward, contentDescription = s.moveUp, tint = Color.White)
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(Icons.Default.ArrowDownward, contentDescription = s.moveDown, tint = Color.White)
            }
            if (onMoveBottom != null) {
                OutlinedButton(onClick = onMoveBottom, enabled = canMoveDown) {
                    Text(s.moveBottom)
                }
            }
            trailing?.invoke()
        }
    }
}
