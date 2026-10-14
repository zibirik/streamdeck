package com.streampanel.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streampanel.core.designsystem.AppBackdrop
import com.streampanel.core.designsystem.BigButton
import com.streampanel.core.designsystem.HintText
import com.streampanel.core.designsystem.IconBubble
import com.streampanel.core.designsystem.PageHeader
import com.streampanel.core.designsystem.SectionLabel
import com.streampanel.core.designsystem.SoftCard
import com.streampanel.core.designsystem.TileGrid
import com.streampanel.core.designsystem.TileSpec
import com.streampanel.core.designsystem.color
import com.streampanel.core.designsystem.icon
import com.streampanel.core.designsystem.strings
import com.streampanel.core.designsystem.subtitle
import com.streampanel.core.designsystem.title
import com.streampanel.core.designsystem.tr
import com.streampanel.core.model.DashboardLayoutSettings
import com.streampanel.core.model.DashboardPanelId
import com.streampanel.core.model.HubSection

@Composable
fun LayoutCustomizerRoute(
    onBack: () -> Unit,
    viewModel: LayoutCustomizerViewModel = hiltViewModel(),
) {
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    LayoutCustomizerScreen(
        layout = layout,
        onBack = onBack,
        onMovePanel = viewModel::movePanel,
        onPanelVisibleChanged = viewModel::setPanelVisible,
        onAllPanelsVisibleChanged = viewModel::setAllPanelsVisible,
        onPresetSelected = viewModel::applyPreset,
        onRestoreDefaults = viewModel::restoreDefaults,
    )
}

@Composable
fun LayoutCustomizerScreen(
    layout: DashboardLayoutSettings,
    onBack: () -> Unit,
    onMovePanel: (DashboardPanelId, Int) -> Unit,
    onPanelVisibleChanged: (DashboardPanelId, Boolean) -> Unit,
    onAllPanelsVisibleChanged: (Boolean) -> Unit,
    onPresetSelected: (LayoutPreset) -> Unit,
    onRestoreDefaults: () -> Unit,
) {
    val s = strings()
    AppBackdrop {
        Scaffold(containerColor = Color.Transparent) { padding ->
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PageHeader(
                        title = tr("Панели", "Panels"),
                        subtitle = tr("Что показывать на каждом экране", "What each screen shows"),
                        onBack = onBack,
                    )

                    SectionLabel(tr("Готовые наборы", "Ready-made sets"))
                    TileGrid(
                        columns = 2,
                        tiles = listOf(
                            TileSpec(s.layoutPresetStream, Icons.Rounded.LiveTv, { onPresetSelected(LayoutPreset.Stream) }),
                            TileSpec(s.layoutPresetWork, Icons.Rounded.Work, { onPresetSelected(LayoutPreset.Work) }),
                            TileSpec(s.layoutPresetGaming, Icons.Rounded.SportsEsports, { onPresetSelected(LayoutPreset.Gaming) }),
                            TileSpec(s.layoutPresetMinimal, Icons.Rounded.Spa, { onPresetSelected(LayoutPreset.Minimal) }),
                        ),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BigButton(s.showAll, { onAllPanelsVisibleChanged(true) }, Modifier.weight(1f), icon = Icons.Rounded.Visibility, filled = false)
                        BigButton(s.hideAll, { onAllPanelsVisibleChanged(false) }, Modifier.weight(1f), icon = Icons.Rounded.VisibilityOff, filled = false)
                    }

                    HubSection.entries.forEach { section ->
                        SectionCard(section, layout, onMovePanel, onPanelVisibleChanged)
                    }

                    BigButton(s.restoreDefaults, onRestoreDefaults, Modifier.fillMaxWidth(), icon = Icons.Rounded.Restore, filled = false)
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    section: HubSection,
    layout: DashboardLayoutSettings,
    onMovePanel: (DashboardPanelId, Int) -> Unit,
    onPanelVisibleChanged: (DashboardPanelId, Boolean) -> Unit,
) {
    val s = strings()
    // Panels of this screen in the user's order, with their place in the whole list
    // (the whole list is what gets reordered, so a "step" may jump over other screens' panels).
    val indexed = layout.panelSlots.withIndex().filter { it.value.id in section.panels }
    val anyVisible = indexed.any { it.value.visible }
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBubble(section.icon, section.color)
            Column(Modifier.weight(1f)) {
                Text(section.title(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(section.subtitle(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        indexed.forEachIndexed { position, (globalIndex, slot) ->
            val previous = indexed.getOrNull(position - 1)
            val next = indexed.getOrNull(position + 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Switch(checked = slot.visible, onCheckedChange = { onPanelVisibleChanged(slot.id, it) })
                Text(
                    s.layoutPanelLabel(slot.id),
                    modifier = Modifier.weight(1f).padding(start = 10.dp).alpha(if (slot.visible) 1f else 0.5f),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = { previous?.let { onMovePanel(slot.id, it.index - globalIndex) } }, enabled = previous != null) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = s.moveUp, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { next?.let { onMovePanel(slot.id, it.index - globalIndex) } }, enabled = next != null) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = s.moveDown, modifier = Modifier.size(28.dp))
                }
            }
        }
        if (!anyVisible) {
            HintText(tr("Все панели скрыты — этот экран не показывается в меню.", "All panels are hidden — this screen is not shown in the menu."))
        }
    }
}
