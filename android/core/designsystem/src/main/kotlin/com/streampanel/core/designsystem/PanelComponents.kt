package com.streampanel.core.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardTab
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Undo
import com.streampanel.core.model.QuickActionItem
import com.streampanel.core.model.QuickActionKeys
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Speaker
import com.streampanel.core.model.QuickLaunchApp
import com.streampanel.core.model.QuickOpenFolder
import com.streampanel.core.model.QuickOpenUrlGroup
import com.streampanel.core.model.PcStorageDrive

@Composable
fun SidebarNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) {
        Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            ),
        )
    } else {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
fun ConnectionPill(
    label: String,
    connected: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val pulse by rememberInfiniteTransition(label = "conn-pulse").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulse",
    )
    val iconTint = panelIconTint()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(
                if (connected) Color(0xFF10B981).copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (connected) (7.dp + 3.dp * pulse) else 8.dp)
                .clip(CircleShape)
                .background(if (connected) Color(0xFF34D399) else MaterialTheme.colorScheme.error),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (connected) Color(0xFF6EE7B7) else MaterialTheme.colorScheme.error,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaControlPanel(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onVolumeSet: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMute: () -> Unit,
    onMicMute: () -> Unit = {},
    onSpeakers: () -> Unit = {},
    onHeadphones: () -> Unit = {},
    isMuted: Boolean = false,
    isMicMuted: Boolean = false,
    isPlaying: Boolean = false,
    nowPlaying: String? = null,
    modifier: Modifier = Modifier,
) {
    val iconTint = panelIconTint()
    GlassSurface(modifier = modifier.fillMaxWidth(), elevated = true) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val s = strings()
            SectionHeader(s.systemAudio, s.systemAudioSub)
            if (!nowPlaying.isNullOrBlank()) {
                Text(
                    text = nowPlaying,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MediaCircleButton(onClick = onMute) {
                    Icon(
                        if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeMute,
                        contentDescription = "Mute",
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    onValueChangeFinished = { onVolumeSet(volume.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
                Text(
                    text = "${volume.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediaCircleButton(onClick = {
                    val next = (volume - 5f).coerceAtLeast(0f)
                    onVolumeChange(next)
                    onVolumeSet(next.toInt())
                }) {
                    Icon(Icons.Default.VolumeDown, contentDescription = "Volume down", tint = iconTint, modifier = Modifier.size(20.dp))
                }
                MediaCircleButton(onClick = {
                    val next = (volume + 5f).coerceAtMost(100f)
                    onVolumeChange(next)
                    onVolumeSet(next.toInt())
                }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Volume up", tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ToolChip(
                    label = if (isMicMuted) s.micOff else s.micMute,
                    icon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    onClick = onMicMute,
                )
                ToolChip(s.speakers, Icons.Default.Speaker, onSpeakers, Modifier.weight(1f))
                ToolChip(s.headphones, Icons.Default.Headphones, onHeadphones, Modifier.weight(1f))
            }

            SectionHeader(s.media, if (isPlaying) s.playing else s.paused)
            val playPulse by rememberInfiniteTransition(label = "play-pulse").animateFloat(
                initialValue = 1f,
                targetValue = if (isPlaying) 1.08f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "play-scale",
            )
            val playElevation by animateFloatAsState(
                targetValue = if (isPlaying) 16f else 10f,
                label = "play-elevation",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediaCircleButton(onClick = onPrevious, size = 52.dp) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = iconTint, modifier = Modifier.size(24.dp))
                }
                Surface(
                    onClick = onPlayPause,
                    shape = CircleShape,
                    color = if (isPlaying) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    shadowElevation = playElevation.dp,
                    modifier = Modifier
                        .size((68 * playPulse).dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                (fadeIn(tween(200)) + scaleIn(initialScale = 0.8f))
                                    .togetherWith(fadeOut(tween(150)) + scaleOut(targetScale = 0.8f))
                            },
                            label = "play-pause-icon",
                        ) { playing ->
                            Icon(
                                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                }
                MediaCircleButton(onClick = onNext, size = 52.dp) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = iconTint, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaCircleButton(
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
fun QuickLaunchGrid(
    apps: List<QuickLaunchApp>,
    onLaunch: (QuickLaunchApp) -> Unit,
    onConfigure: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeader(strings().quickLaunch, strings().quickLaunchSub)
                if (onConfigure != null) {
                    Text(
                        text = strings().edit,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onConfigure)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            apps.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { app ->
                        QuickLaunchTile(
                            app = app,
                            modifier = Modifier.weight(1f),
                            onClick = { onLaunch(app) },
                        )
                    }
                    if (row.size == 1) {
                        Box(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLaunchTile(
    app: QuickLaunchApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = parseHexColor(app.accent, MaterialTheme.colorScheme.primary)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconForName(app.iconName),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                app.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                lineHeight = MaterialTheme.typography.labelLarge.lineHeight,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SidebarAction(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon()
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun QuickActionsStrip(
    items: List<QuickActionItem>,
    onAction: (String) -> Unit,
    onConfigure: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val enabled = items.filter { it.enabled }.sortedWith(compareBy({ it.row }, { it.sortOrder }))
    val rows = enabled.groupBy { it.row }.toSortedMap().values
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeader(strings().quickActions, strings().quickActionsSub)
                if (onConfigure != null) {
                    Text(
                        text = strings().edit,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onConfigure)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            rows.forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { item ->
                        val spec = quickActionSpec(item.actionKey) ?: return@forEach
                        QuickActionTile(spec.label, spec.icon, { onAction(item.actionKey) }, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class QuickActionSpec(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun quickActionSpec(key: String): QuickActionSpec? {
    val s = strings()
    return when (key) {
        QuickActionKeys.LOCK -> QuickActionSpec(s.lock, Icons.Default.Lock)
        QuickActionKeys.PASTE -> QuickActionSpec(s.paste, Icons.Default.ContentPaste)
        QuickActionKeys.DESKTOP -> QuickActionSpec(s.desktop, Icons.Default.DesktopWindows)
        QuickActionKeys.SYNC_VOLUME -> QuickActionSpec(s.sync, Icons.Default.Refresh)
        QuickActionKeys.COPY -> QuickActionSpec(s.copy, Icons.Default.ContentCopy)
        QuickActionKeys.ALT_TAB -> QuickActionSpec(s.altTab, Icons.Default.SwapHoriz)
        QuickActionKeys.CLOSE_WINDOW -> QuickActionSpec(s.closeWin, Icons.Default.Close)
        QuickActionKeys.FULLSCREEN -> QuickActionSpec(s.fullscreen, Icons.Default.Fullscreen)
        QuickActionKeys.REFRESH -> QuickActionSpec(s.refresh, Icons.Default.Refresh)
        QuickActionKeys.UNDO -> QuickActionSpec(s.undo, Icons.Default.Undo)
        QuickActionKeys.CUT -> QuickActionSpec(s.cut, Icons.Default.ContentCut)
        QuickActionKeys.REDO -> QuickActionSpec(s.redo, Icons.Default.Redo)
        QuickActionKeys.SAVE -> QuickActionSpec(s.save, Icons.Default.Save)
        QuickActionKeys.SEARCH -> QuickActionSpec(s.search, Icons.Default.Search)
        QuickActionKeys.NEW_TAB -> QuickActionSpec(s.newTab, Icons.Default.KeyboardTab)
        QuickActionKeys.SCREENSHOT -> QuickActionSpec(s.screenshot, Icons.Default.CameraAlt)
        QuickActionKeys.TASK_MANAGER -> QuickActionSpec(s.taskManager, Icons.Default.Computer)
        QuickActionKeys.SNAP_LEFT -> QuickActionSpec(s.snapLeft, Icons.Default.DesktopWindows)
        QuickActionKeys.SNAP_RIGHT -> QuickActionSpec(s.snapRight, Icons.Default.DesktopWindows)
        QuickActionKeys.PLAY_PAUSE -> QuickActionSpec(s.media, Icons.Default.PlayArrow)
        QuickActionKeys.PREVIOUS_TRACK -> QuickActionSpec(s.previousTrack, Icons.Default.SkipPrevious)
        QuickActionKeys.NEXT_TRACK -> QuickActionSpec(s.nextTrack, Icons.Default.SkipNext)
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = LocalStreamPanelExtras.current
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = label, tint = panelIconTint(), modifier = Modifier.size(20.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
fun SidebarIconActions(
    onOpenObs: () -> Unit,
    onOpenConnections: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val tint = panelIconTint()
        val s = strings()
        SidebarAction(s.obsStudio, { Icon(Icons.Default.LiveTv, null, tint = tint) }, onOpenObs)
        SidebarAction(s.pcServer, { Icon(Icons.Default.Computer, null, tint = tint) }, onOpenConnections)
        SidebarAction(s.settings, { Icon(Icons.Default.Settings, null, tint = tint) }, onOpenSettings)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemToolsPanel(
    onMinimizeAll: () -> Unit,
    onMaximize: () -> Unit,
    onMinimizeActive: () -> Unit,
    onSnapLeft: () -> Unit,
    onSnapRight: () -> Unit,
    onMoveToMonitor: (Int) -> Unit,
    onScreenshot: () -> Unit,
    onTaskManager: () -> Unit,
    onKillProcess: () -> Unit = {},
    onLock: () -> Unit = {},
    onAltTab: () -> Unit = {},
    onCloseActive: () -> Unit = {},
    onFullscreen: () -> Unit = {},
    onSleep: () -> Unit,
    onSleepLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val s = strings()
            SectionHeader(s.windowsSystem, s.windowsSystemSub)
            ToolButtonRow(
                ToolButtonSpec(s.minimizeAll, onMinimizeAll),
                ToolButtonSpec(s.maximize, onMaximize),
            )
            ToolButtonRow(
                ToolButtonSpec(s.snapLeft, onSnapLeft),
                ToolButtonSpec(s.snapRight, onSnapRight),
            )
            ToolButtonRow(
                ToolButtonSpec(s.minimizeWin, onMinimizeActive),
                ToolButtonSpec(s.closeWin, onCloseActive),
            )
            ToolButtonRow(
                ToolButtonSpec(s.altTab, onAltTab),
                ToolButtonSpec(s.fullscreen, onFullscreen),
            )
            ToolButtonRow(
                ToolButtonSpec(s.screenshot, onScreenshot),
                ToolButtonSpec(s.lock, onLock),
            )
            SectionHeader(s.moveToMonitor, s.moveToMonitorSub)
            ToolButtonRow(
                ToolButtonSpec(s.monitor(1), { onMoveToMonitor(1) }),
                ToolButtonSpec(s.monitor(2), { onMoveToMonitor(2) }),
                ToolButtonSpec(s.monitor(3), { onMoveToMonitor(3) }),
            )
            ToolButtonRow(
                ToolButtonSpec(s.taskManager, onTaskManager),
                ToolButtonSpec(s.killProcess, onKillProcess),
            )
            ToolButtonRow(
                ToolButtonSpec(s.sleep, onSleep, onLongPress = onSleepLongPress),
            )
        }
    }
}

@Composable
fun NavigationShortcutsPanel(
    folders: List<QuickOpenFolder>,
    urlGroups: List<QuickOpenUrlGroup>,
    onOpenFolder: (QuickOpenFolder) -> Unit,
    onOpenUrlGroup: (QuickOpenUrlGroup) -> Unit,
    onConfigure: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeader(strings().navigation, strings().navigationSub)
                if (onConfigure != null) {
                    Text(
                        text = strings().edit,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onConfigure)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            folders.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { folder ->
                        ToolChip(
                            label = folder.name,
                            icon = Icons.Default.Folder,
                            onClick = { onOpenFolder(folder) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }
            urlGroups.forEach { group ->
                ToolChip(
                    label = group.name,
                    icon = Icons.Default.Public,
                    onClick = { onOpenUrlGroup(group) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun HardwareMonitorPanel(
    cpuPercent: Float?,
    ramPercent: Float?,
    foregroundProcess: String?,
    downloadMbps: Float? = null,
    uploadMbps: Float? = null,
    diskFreePercent: Float? = null,
    storageDrives: List<PcStorageDrive> = emptyList(),
    networkInterface: String? = null,
    modifier: Modifier = Modifier,
) {
    val s = strings()
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(s.hwMonitor, s.hwMonitorSub)
            if (!foregroundProcess.isNullOrBlank()) {
                Text(
                    s.foregroundApp(foregroundProcess),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatBar(s.cpu, cpuPercent, Modifier.weight(1f))
                StatBar(s.ram, ramPercent, Modifier.weight(1f))
            }
            if (downloadMbps != null || uploadMbps != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        s.networkDown(downloadMbps?.let { "%.1f".format(it) } ?: "—"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        s.networkUp(uploadMbps?.let { "%.1f".format(it) } ?: "—"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                networkInterface?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            if (storageDrives.isNotEmpty()) {
                Text(s.storage, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                storageDrives.take(5).forEach { drive ->
                    val usedPercent = (100.0 - drive.freePercent).toFloat()
                    val label = buildString {
                        append(drive.name)
                        if (!drive.label.isNullOrBlank()) append(" · ").append(drive.label)
                        append(" · ").append("%.1f GB".format(drive.freeGb)).append(" ").append(s.storageFree)
                    }
                    StatBar(label, usedPercent, Modifier.fillMaxWidth())
                }
            } else {
                diskFreePercent?.let { free ->
                    StatBar(s.disk, 100f - free, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun StatBar(label: String, percent: Float?, modifier: Modifier = Modifier) {
    val value = percent?.coerceIn(0f, 100f) ?: 0f
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                if (percent != null) "${value.toInt()}%" else "—",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(value / 100f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
fun ClipboardSharePanel(
    preview: String?,
    onPasteFromPc: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = strings()
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(s.clipboard, s.clipboardSub)
            if (!preview.isNullOrBlank()) {
                Text(
                    preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            ToolChip(s.pasteFromPc, Icons.Default.ContentPaste, onPasteFromPc, Modifier.fillMaxWidth())
        }
    }
}

private data class ToolButtonSpec(
    val label: String,
    val onClick: () -> Unit,
    val onLongPress: (() -> Unit)? = null,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolButtonRow(vararg buttons: ToolButtonSpec) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        buttons.forEach { spec ->
            val shape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            ),
                        ),
                    )
                    .combinedClickable(
                        onClick = spec.onClick,
                        onLongClick = spec.onLongPress,
                    )
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            ) {
                Text(
                    text = spec.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = panelIconTint(), modifier = Modifier.size(16.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}
