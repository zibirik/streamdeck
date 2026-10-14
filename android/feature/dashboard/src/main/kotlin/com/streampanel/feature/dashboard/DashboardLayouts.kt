package com.streampanel.feature.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.streampanel.core.designsystem.AppStrings
import com.streampanel.core.designsystem.BigButton
import com.streampanel.core.designsystem.ChoicePill
import com.streampanel.core.designsystem.ClipboardSharePanel
import com.streampanel.core.designsystem.ControlButtonCard
import com.streampanel.core.designsystem.DeckDestination
import com.streampanel.core.designsystem.DevToolsPanel
import com.streampanel.core.designsystem.DiscordPanel
import com.streampanel.core.designsystem.EmptyState
import com.streampanel.core.designsystem.GameStatusPanel
import com.streampanel.core.designsystem.HardwareMonitorPanel
import com.streampanel.core.designsystem.LocalStreamPanelExtras
import com.streampanel.core.designsystem.MediaControlPanel
import com.streampanel.core.designsystem.MeetingModePanel
import com.streampanel.core.designsystem.NavigationShortcutsPanel
import com.streampanel.core.designsystem.PcConfiguratorPanel
import com.streampanel.core.designsystem.PomodoroPanel
import com.streampanel.core.designsystem.ProcessMonitorPanel
import com.streampanel.core.designsystem.QuickActionsStrip
import com.streampanel.core.designsystem.QuickLaunchGrid
import com.streampanel.core.designsystem.StatusDot
import com.streampanel.core.designsystem.StreamChatPanel
import com.streampanel.core.designsystem.StreamToolsPanel
import com.streampanel.core.designsystem.StudyModePanel
import com.streampanel.core.designsystem.SystemToolsPanel
import com.streampanel.core.designsystem.TimeTrackerPanel
import com.streampanel.core.designsystem.UiPalette
import com.streampanel.core.designsystem.WindowWidthSizeClass
import com.streampanel.core.designsystem.color
import com.streampanel.core.designsystem.icon
import com.streampanel.core.designsystem.subtitle
import com.streampanel.core.designsystem.title
import com.streampanel.core.designsystem.toWidthSizeClass
import com.streampanel.core.designsystem.tr
import com.streampanel.core.model.DashboardButton
import com.streampanel.core.model.DashboardPanelId
import com.streampanel.core.model.HubSection
import com.streampanel.core.model.QuickLaunchApp
import com.streampanel.core.model.QuickOpenFolder
import com.streampanel.core.model.QuickOpenUrlGroup
import com.streampanel.core.model.visiblePanels
import com.streampanel.core.network.ConnectionStatus
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** The five places the app is split into. "Deck" is the user's own buttons; the rest are tool topics. */
private enum class Tab(val section: HubSection?) {
    Deck(null),
    Control(HubSection.Control),
    Stream(HubSection.Stream),
    Pc(HubSection.Pc),
    Work(HubSection.Work),
}

@Composable
private fun Tab.label(): String = section?.title() ?: DeckDestination.title()

private val Tab.tabIcon: ImageVector get() = section?.icon ?: DeckDestination.icon

private val Tab.tabColor: Color get() = section?.color ?: DeckDestination.color

/**
 * The whole app screen: navigation (a rail on tablets, a bar on phones) and the page it points to.
 * One idea per page instead of one endless column of everything.
 */
@Composable
fun AdaptiveDashboardLayout(
    state: DashboardUiState,
    s: AppStrings,
    connected: Boolean,
    modifier: Modifier = Modifier,
    callbacks: DashboardCallbacks,
) {
    val tabs = Tab.entries.filter { tab -> tab.section == null || state.layout.visiblePanels(tab.section).isNotEmpty() }
    var selectedName by rememberSaveable { mutableStateOf(Tab.Deck.name) }
    val tab = tabs.firstOrNull { it.name == selectedName } ?: Tab.Deck
    var editing by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(tab) { if (tab != Tab.Deck) editing = false }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sizeClass = maxWidth.toWidthSizeClass()
        val compact = sizeClass == WindowWidthSizeClass.Compact
        val page: @Composable (Modifier) -> Unit = { pageModifier ->
            TabPage(
                tab = tab,
                state = state,
                s = s,
                compact = compact,
                connected = connected,
                editing = editing,
                onEditingChange = { editing = it },
                callbacks = callbacks,
                modifier = pageModifier,
            )
        }
        if (compact) {
            Column(Modifier.fillMaxSize()) {
                page(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp))
                BottomBar(tabs, tab, onSelect = { selectedName = it.name })
            }
        } else {
            Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SideRail(
                    tabs = tabs,
                    selected = tab,
                    onSelect = { selectedName = it.name },
                    onOpenSettings = callbacks.onOpenSettings,
                    settingsLabel = s.settings,
                )
                page(Modifier.weight(1f).fillMaxHeight().padding(end = 4.dp))
            }
        }
    }
}

// ----------------------------------------------------------------------- navigation

@Composable
private fun BottomBar(tabs: List<Tab>, selected: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.tabIcon, contentDescription = null) },
                label = { Text(tab.label(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun SideRail(
    tabs: List<Tab>,
    selected: Tab,
    onSelect: (Tab) -> Unit,
    onOpenSettings: () -> Unit,
    settingsLabel: String,
) {
    val extras = LocalStreamPanelExtras.current
    Surface(
        modifier = Modifier.width(92.dp).fillMaxHeight(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = extras.glassAlpha),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(extras.accentGradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                tabs.forEach { tab ->
                    RailItem(tab.tabIcon, tab.label(), selected = tab == selected, onClick = { onSelect(tab) })
                }
            }
            RailItem(Icons.Rounded.Settings, settingsLabel, selected = false, onClick = onOpenSettings)
        }
    }
}

@Composable
private fun RailItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val pill by animateColorAsState(if (selected) primary.copy(alpha = 0.24f) else Color.Transparent, label = "rail-pill")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier.width(56.dp).height(34.dp).clip(RoundedCornerShape(17.dp)).background(pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ----------------------------------------------------------------------- page frame

@Composable
private fun TabPage(
    tab: Tab,
    state: DashboardUiState,
    s: AppStrings,
    compact: Boolean,
    connected: Boolean,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    callbacks: DashboardCallbacks,
    modifier: Modifier = Modifier,
) {
    val title = if (tab == Tab.Deck) state.currentPage?.title ?: s.deckTitle else tab.label()
    val subtitle = if (tab == Tab.Deck) {
        s.controlsCount(state.gridLayout.columns, state.gridLayout.rows, state.buttons.size)
    } else {
        tab.section?.subtitle().orEmpty()
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TopBar(
            title = title,
            subtitle = subtitle,
            status = state.connectionStatus,
            showEdit = tab == Tab.Deck,
            editing = editing,
            onToggleEdit = { onEditingChange(!editing) },
            showSettings = compact,
            callbacks = callbacks,
        )
        if (!connected) OfflineBanner(state.connectionStatus, callbacks.onConnect)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val section = tab.section
            if (section == null) {
                DeckScreen(state, s, compact, editing, callbacks)
            } else {
                HubScreen(section, state, callbacks)
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    subtitle: String,
    status: ConnectionStatus,
    showEdit: Boolean,
    editing: Boolean,
    onToggleEdit: () -> Unit,
    showSettings: Boolean,
    callbacks: DashboardCallbacks,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        ConnectionChip(status, callbacks.onOpenConnections)
        if (showEdit) EditToggle(editing, onToggleEdit)
        if (showSettings) {
            Surface(
                onClick = callbacks.onOpenSettings,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = tr("Настройки", "Settings"),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionChip(status: ConnectionStatus, onClick: () -> Unit) {
    val (color, label, pulsing) = when (status) {
        is ConnectionStatus.Connected -> Triple(UiPalette.Green, tr("Онлайн", "Online"), true)
        ConnectionStatus.Connecting -> Triple(UiPalette.Amber, tr("Подключаюсь", "Connecting"), true)
        is ConnectionStatus.Failed -> Triple(UiPalette.Red, tr("Нет связи", "No link"), false)
        ConnectionStatus.Disconnected -> Triple(UiPalette.Slate, tr("Офлайн", "Offline"), false)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(color, pulsing, size = 8)
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

@Composable
private fun EditToggle(editing: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val bg by animateColorAsState(if (editing) primary else MaterialTheme.colorScheme.surfaceVariant, label = "edit-bg")
    Row(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val tint = if (editing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        Icon(if (editing) Icons.Rounded.Check else Icons.Rounded.Edit, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(
            if (editing) tr("Готово", "Done") else tr("Изменить", "Edit"),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = tint,
        )
    }
}

@Composable
private fun OfflineBanner(status: ConnectionStatus, onConnect: () -> Unit) {
    val connecting = status == ConnectionStatus.Connecting
    val color = if (status is ConnectionStatus.Failed) UiPalette.Red else UiPalette.Amber
    val message = when (status) {
        ConnectionStatus.Connecting -> tr("Подключаюсь к ПК…", "Connecting to the PC…")
        is ConnectionStatus.Failed -> tr("Нет связи с ПК. Проверьте, что сервер запущен.", "No link to the PC. Check that the server is running.")
        else -> tr("ПК не подключён — кнопки пока не сработают.", "PC is not connected — the buttons won't work yet.")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (connecting) {
            CircularProgressIndicator(color = color, strokeWidth = 3.dp, modifier = Modifier.size(22.dp))
        } else {
            Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        Text(
            message,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!connecting) {
            Text(
                tr("Подключить", "Connect"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onConnect)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

// ----------------------------------------------------------------------- deck

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeckScreen(
    state: DashboardUiState,
    s: AppStrings,
    compact: Boolean,
    editing: Boolean,
    callbacks: DashboardCallbacks,
) {
    var confirmDeletePage by remember { mutableStateOf(false) }
    val layerWord = tr("Слой", "Layer")
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.pages.size > 1 || editing) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.pages.forEach { page ->
                    ChoicePill(page.title, selected = page.id == state.currentPage?.id, onClick = { callbacks.onPageSelected(page.id) })
                }
                if (editing) {
                    EditPill(Icons.Rounded.Add, layerWord, onClick = { callbacks.onCreatePage("$layerWord ${state.pages.size + 1}") })
                }
            }
        }
        if (editing) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EditPill(Icons.Rounded.Add, tr("Кнопка", "Button"), onClick = callbacks.onCreateButton, primary = true)
                EditPill(Icons.Rounded.GridView, tr("Сетка и название", "Grid and name"), onClick = callbacks.onOpenLayerGridSettings)
                if (state.pages.size > 1) {
                    EditPill(Icons.Rounded.Delete, tr("Удалить слой", "Delete layer"), onClick = { confirmDeletePage = true }, danger = true)
                }
            }
            Text(
                tr("Нажмите на кнопку, чтобы изменить её. Зажмите и потяните — переместить.", "Tap a button to change it. Hold and drag to move it."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (state.buttons.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.GridView,
                    title = tr("Здесь пока пусто", "Nothing here yet"),
                    body = tr("Добавьте первую кнопку — она появится на этом слое.", "Add your first button — it will appear on this layer."),
                    actionLabel = tr("Добавить кнопку", "Add a button"),
                    onAction = callbacks.onCreateButton,
                )
            } else {
                DeckGrid(state, compact, editing, callbacks)
            }
        }
    }

    if (confirmDeletePage) {
        com.streampanel.core.designsystem.StreamDialog(
            onDismiss = { confirmDeletePage = false },
            title = tr("Удалить слой «${state.currentPage?.title.orEmpty()}»?", "Delete layer “${state.currentPage?.title.orEmpty()}”?"),
            message = tr("Все кнопки на нём тоже будут удалены.", "All the buttons on it will be deleted too."),
        ) {
            BigButton(
                text = tr("Удалить", "Delete"),
                onClick = { confirmDeletePage = false; callbacks.onDeletePage() },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Delete,
                color = UiPalette.Red,
            )
            BigButton(s.cancel, { confirmDeletePage = false }, Modifier.fillMaxWidth(), filled = false)
        }
    }
}

@Composable
private fun EditPill(icon: ImageVector, label: String, onClick: () -> Unit, primary: Boolean = false, danger: Boolean = false) {
    val accent = when {
        danger -> UiPalette.Red
        primary -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (primary || danger) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = accent, maxLines = 1)
    }
}

@Composable
private fun DeckGrid(state: DashboardUiState, compact: Boolean, editing: Boolean, callbacks: DashboardCallbacks) {
    val gap = if (compact) 10.dp else 16.dp
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val configured = state.gridLayout
        // As many columns as the user asked for, but never tiles narrower than ~116dp.
        val columns = min(configured.columns, max(2, floor(maxWidth.value / 116f).toInt()))
        val rows = configured.rows
        // Fill the height of the screen with the configured rows when it fits, otherwise scroll.
        val fitted = (maxHeight - gap * (rows - 1)) / rows
        val cellHeight = fitted.coerceIn(88.dp, 168.dp)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            items(state.buttons, key = { it.id }) { button ->
                val drag = remember(button.id) { floatArrayOf(0f, 0f) }
                ControlButtonCard(
                    button = button,
                    executing = button.id == state.executingButtonId,
                    editing = editing,
                    modifier = Modifier
                        .height(cellHeight)
                        .pointerInput(button.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { drag[0] = 0f; drag[1] = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    drag[0] += dragAmount.x
                                    drag[1] += dragAmount.y
                                },
                                onDragEnd = {
                                    val columnDelta = when { drag[0] > 80f -> 1; drag[0] < -80f -> -1; else -> 0 }
                                    val rowDelta = when { drag[1] > 80f -> 1; drag[1] < -80f -> -1; else -> 0 }
                                    if (columnDelta != 0 || rowDelta != 0) callbacks.onMoveButton(button, rowDelta, columnDelta)
                                },
                            )
                        },
                    onClick = {
                        if (editing) callbacks.onShowButtonActions(button) else callbacks.onButtonPressed(button)
                    },
                    onLongClick = { callbacks.onShowButtonActions(button) },
                )
            }
        }
    }
}

// ----------------------------------------------------------------------- tool topics

@Composable
private fun HubScreen(section: HubSection, state: DashboardUiState, callbacks: DashboardCallbacks) {
    val panels = state.layout.visiblePanels(section).filter { panelHasContent(it, state) }
    if (panels.isEmpty()) {
        EmptyState(
            icon = section.icon,
            title = tr("Здесь пока ничего нет", "Nothing here yet"),
            body = tr("Включите нужные панели в Настройки → Панели.", "Turn the panels you need on in Settings → Panels."),
            actionLabel = tr("Настройки", "Settings"),
            onAction = callbacks.onOpenSettings,
        )
        return
    }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 340.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalItemSpacing = 14.dp,
    ) {
        items(panels, key = { it.name }) { id -> DashboardPanel(id, state, callbacks) }
    }
}

/** Some panels draw nothing until there is data; keep them out of the grid so it has no holes. */
private fun panelHasContent(id: DashboardPanelId, state: DashboardUiState): Boolean = when (id) {
    DashboardPanelId.ProcessMonitor -> state.topProcesses.isNotEmpty()
    DashboardPanelId.TimeTracker -> state.timeTrackerProjects.isNotEmpty()
    else -> true
}

@Composable
private fun DashboardPanel(id: DashboardPanelId, state: DashboardUiState, callbacks: DashboardCallbacks) {
    when (id) {
        DashboardPanelId.HardwareMonitor -> HardwareMonitorPanel(
            cpuPercent = state.cpuPercent,
            ramPercent = state.ramPercent,
            foregroundProcess = state.foregroundProcess,
            downloadMbps = state.downloadMbps,
            uploadMbps = state.uploadMbps,
            diskFreePercent = state.diskFreePercent,
            storageDrives = state.storageDrives,
            networkInterface = state.networkInterface,
        )
        DashboardPanelId.ProcessMonitor -> ProcessMonitorPanel(state.topProcesses, callbacks.onKillProcessByPid, callbacks.onCleanTemp)
        DashboardPanelId.DevTools -> DevToolsPanel(
            onGitPull = callbacks.onGitPull,
            onGitStatus = callbacks.onGitStatus,
            onDockerPs = callbacks.onDockerPs,
            onDockerUp = callbacks.onDockerUp,
            onVscodeTest = callbacks.onVscodeTest,
            onVscodeFormat = callbacks.onVscodeFormat,
            onVscodeTerminal = callbacks.onVscodeTerminal,
            onWifiRestart = callbacks.onWifiRestart,
            onFlushDns = callbacks.onFlushDns,
            onEmptyRecycleBin = callbacks.onEmptyRecycleBin,
        )
        DashboardPanelId.StreamTools -> StreamToolsPanel(
            onOpenObs = callbacks.onOpenObs,
            onOpenTwitch = { callbacks.onOpenUrlGroup(QuickOpenUrlGroup("twitch-tools", "Twitch", "https://dashboard.twitch.tv")) },
            onOpenYoutube = { callbacks.onOpenUrlGroup(QuickOpenUrlGroup("youtube-tools", "YouTube", "https://studio.youtube.com|https://www.youtube.com/live_dashboard")) },
            onOpenChat = {
                callbacks.onOpenUrlGroup(
                    state.quickOpenUrlGroups.firstOrNull { it.id == "stream-kit" }
                        ?: QuickOpenUrlGroup("stream-chat", "Chat", "https://dashboard.twitch.tv|https://studio.youtube.com"),
                )
            },
            onSaveReplay = callbacks.onOpenObs,
        )
        DashboardPanelId.StreamChat -> StreamChatPanel(
            settings = state.streamChatSettings,
            onOpenSettings = callbacks.onOpenSettings,
        )
        DashboardPanelId.GameStatus -> GameStatusPanel(
            settings = state.gameOverlaySettings,
            gameInfo = state.gameInfo,
            onOpenSettings = callbacks.onOpenSettings,
            integration = state.cs2Integration,
            onInstallIntegration = callbacks.onInstallCs2,
            pcConnected = state.connectionStatus is ConnectionStatus.Connected,
        )
        DashboardPanelId.PcConfigurator -> PcConfiguratorPanel(
            url = state.pcConfiguratorUrl,
            onOpenConfigurator = {
                state.pcConfiguratorUrl?.let {
                    callbacks.onOpenUrlGroup(QuickOpenUrlGroup("pc-configurator", "PC Configurator", it))
                }
            },
            onOpenSettings = callbacks.onOpenSettings,
        )
        DashboardPanelId.Discord -> DiscordPanel(
            callbacks.onOpenDiscord,
            callbacks.onDiscordMute,
            callbacks.onDiscordDeafen,
            callbacks.onDiscordPushToTalk,
        )
        DashboardPanelId.StudyMode -> StudyModePanel(
            onStartFocus = callbacks.onStartPomodoro,
            onOpenStudyPack = { callbacks.onOpenUrlGroup(QuickOpenUrlGroup("study-pack", "Study", "https://calendar.google.com|https://docs.google.com|https://chat.openai.com")) },
            onOpenNotes = { callbacks.onOpenUrlGroup(QuickOpenUrlGroup("notes", "Notes", "https://docs.google.com/document/u/0/")) },
        )
        DashboardPanelId.MeetingMode -> MeetingModePanel(
            onMicMute = callbacks.onMicMute,
            onOpenDiscord = callbacks.onOpenDiscord,
            onOpenTeams = { callbacks.onLaunchApp(QuickLaunchApp("teams", "Teams", "msteams:", "chat", "#6264A7")) },
            onOpenZoom = { callbacks.onLaunchApp(QuickLaunchApp("zoom", "Zoom", "zoommtg:", "videocam", "#2D8CFF")) },
        )
        DashboardPanelId.Pomodoro -> PomodoroPanel(
            state.pomodoroRemainingSeconds,
            state.pomodoroFocusMinutes * 60,
            state.pomodoroRunning,
            state.pomodoroFocusMinutes,
            callbacks.onSetPomodoroFocusMinutes,
            callbacks.onStartPomodoro,
            callbacks.onStopPomodoro,
        )
        DashboardPanelId.TimeTracker -> TimeTrackerPanel(
            state.timeTrackerProjects,
            state.activeTimeProjectId,
            state.timeTrackerElapsedSeconds,
            callbacks.onStartTimeTracker,
            callbacks.onStopTimeTracker,
        )
        DashboardPanelId.Clipboard -> ClipboardSharePanel(state.clipboardPreview, callbacks.onPasteFromPc)
        DashboardPanelId.QuickActions -> QuickActionsStrip(state.quickActions, callbacks.onQuickAction, callbacks.onOpenSettings)
        DashboardPanelId.Media -> MediaControlPanel(
            volume = state.volumeLevel,
            onVolumeChange = callbacks.onVolumeChange,
            onVolumeSet = { _ -> callbacks.onVolumeSet() },
            onPlayPause = { callbacks.onMediaAction("play_pause") },
            onPrevious = { callbacks.onMediaAction("previous") },
            onNext = { callbacks.onMediaAction("next") },
            onMute = { callbacks.onMediaAction("mute") },
            onMicMute = callbacks.onMicMute,
            onSpeakers = callbacks.onSpeakers,
            onHeadphones = callbacks.onHeadphones,
            isMuted = state.isMuted,
            isMicMuted = state.isMicMuted,
            isPlaying = state.isPlaying,
            nowPlaying = state.nowPlaying,
        )
        DashboardPanelId.SystemTools -> SystemToolsPanel(
            callbacks.onMinimizeAll,
            callbacks.onMaximize,
            callbacks.onMinimizeActive,
            callbacks.onSnapLeft,
            callbacks.onSnapRight,
            callbacks.onMoveToMonitor,
            callbacks.onScreenshot,
            callbacks.onTaskManager,
            callbacks.onKillProcess,
            callbacks.onLock,
            callbacks.onAltTab,
            callbacks.onCloseWindow,
            callbacks.onFullscreen,
            {},
            callbacks.onSleepLongPress,
        )
        DashboardPanelId.NavigationShortcuts -> NavigationShortcutsPanel(
            state.quickOpenFolders,
            state.quickOpenUrlGroups,
            callbacks.onOpenFolder,
            callbacks.onOpenUrlGroup,
            callbacks.onOpenSettings,
        )
        DashboardPanelId.QuickLaunch -> QuickLaunchGrid(state.quickLaunchApps, callbacks.onLaunchApp, callbacks.onOpenSettings)
    }
}

data class DashboardCallbacks(
    val onPageSelected: (String) -> Unit,
    val onButtonPressed: (DashboardButton) -> Unit,
    val onMoveButton: (DashboardButton, Int, Int) -> Unit,
    val onShowButtonActions: (DashboardButton) -> Unit,
    val onVolumeChange: (Float) -> Unit,
    val onVolumeSet: () -> Unit,
    val onMediaAction: (String) -> Unit,
    val onMicMute: () -> Unit,
    val onSpeakers: () -> Unit,
    val onHeadphones: () -> Unit,
    val onLaunchApp: (QuickLaunchApp) -> Unit,
    val onOpenFolder: (QuickOpenFolder) -> Unit,
    val onOpenUrlGroup: (QuickOpenUrlGroup) -> Unit,
    val onMinimizeAll: () -> Unit,
    val onMaximize: () -> Unit,
    val onSnapLeft: () -> Unit,
    val onSnapRight: () -> Unit,
    val onMoveToMonitor: (Int) -> Unit,
    val onLock: () -> Unit,
    val onAltTab: () -> Unit,
    val onCloseWindow: () -> Unit,
    val onFullscreen: () -> Unit,
    val onMinimizeActive: () -> Unit,
    val onScreenshot: () -> Unit,
    val onTaskManager: () -> Unit,
    val onKillProcess: () -> Unit,
    val onSleepLongPress: () -> Unit,
    val onQuickAction: (String) -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenObs: () -> Unit,
    val onOpenConnections: () -> Unit,
    val onConnect: () -> Unit,
    val onDisconnect: () -> Unit,
    val onCreatePage: (String) -> Unit,
    val onDeletePage: () -> Unit,
    val onCreateButton: () -> Unit,
    val onOpenLayerGridSettings: () -> Unit,
    val onRenamePage: (String) -> Unit,
    val onPasteFromPc: () -> Unit,
    val onStartPomodoro: () -> Unit,
    val onSetPomodoroFocusMinutes: (Int) -> Unit,
    val onStopPomodoro: () -> Unit,
    val onStartTimeTracker: (String) -> Unit,
    val onStopTimeTracker: () -> Unit,
    val onKillProcessByPid: (Int) -> Unit,
    val onCleanTemp: () -> Unit,
    val onOpenDiscord: () -> Unit,
    val onDiscordMute: () -> Unit,
    val onDiscordDeafen: () -> Unit,
    val onDiscordPushToTalk: () -> Unit,
    val onGitPull: () -> Unit,
    val onGitStatus: () -> Unit,
    val onDockerPs: () -> Unit,
    val onDockerUp: () -> Unit,
    val onVscodeTest: () -> Unit,
    val onVscodeFormat: () -> Unit,
    val onVscodeTerminal: () -> Unit,
    val onWifiRestart: () -> Unit,
    val onFlushDns: () -> Unit,
    val onEmptyRecycleBin: () -> Unit,
    val onInstallCs2: () -> Unit,
)
