package com.streampanel.core.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardTab
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Minimize
import androidx.compose.material.icons.rounded.Monitor
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.streampanel.core.model.PcStorageDrive
import com.streampanel.core.model.QuickActionItem
import com.streampanel.core.model.QuickActionKeys
import com.streampanel.core.model.QuickLaunchApp
import com.streampanel.core.model.QuickOpenFolder
import com.streampanel.core.model.QuickOpenUrlGroup

/** The small "Edit" pill in the corner of a panel that has its own settings. */
@Composable
internal fun PanelEditChip(onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(primary.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Rounded.Tune, contentDescription = null, tint = primary, modifier = Modifier.size(16.dp))
        Text(strings().edit, style = MaterialTheme.typography.labelLarge, color = primary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun GroupLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

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
    val s = strings()
    PanelCard(
        title = s.systemAudio,
        subtitle = s.systemAudioSub,
        icon = Icons.Rounded.VolumeUp,
        accent = UiPalette.Violet,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RoundIconButton(onClick = onMute, active = isMuted) {
                Icon(
                    if (isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                    contentDescription = tr("Выключить звук", "Mute"),
                    tint = if (isMuted) UiPalette.Red else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
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
                modifier = Modifier.width(52.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        TileGrid(
            columns = 3,
            tiles = listOf(
                TileSpec(
                    label = if (isMicMuted) s.micOff else s.micMute,
                    icon = if (isMicMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    onClick = onMicMute,
                    active = isMicMuted,
                    accent = if (isMicMuted) UiPalette.Red else null,
                ),
                TileSpec(s.speakers, Icons.Rounded.Speaker, onSpeakers),
                TileSpec(s.headphones, Icons.Rounded.Headphones, onHeadphones),
            ),
        )

        GroupLabel(if (isPlaying) "${s.media} · ${s.playing}" else "${s.media} · ${s.paused}")
        if (!nowPlaying.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(
                    nowPlaying,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(onClick = onPrevious, size = 56.dp) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = tr("Назад", "Previous"), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
            }
            val playScale by animateFloatAsState(if (isPlaying) 1.06f else 1f, label = "play-scale")
            Surface(
                onClick = onPlayPause,
                shape = CircleShape,
                color = if (isPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size((72 * playScale).dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.8f)).togetherWith(fadeOut() + scaleOut(targetScale = 0.8f)) },
                        label = "play-pause-icon",
                    ) { playing ->
                        Icon(
                            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playing) tr("Пауза", "Pause") else tr("Играть", "Play"),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(38.dp),
                        )
                    }
                }
            }
            RoundIconButton(onClick = onNext, size = 56.dp) {
                Icon(Icons.Rounded.SkipNext, contentDescription = tr("Дальше", "Next"), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun RoundIconButton(
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    active: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (active) UiPalette.Red.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
fun QuickLaunchGrid(
    apps: List<QuickLaunchApp>,
    onLaunch: (QuickLaunchApp) -> Unit,
    onConfigure: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    PanelCard(
        title = strings().quickLaunch,
        subtitle = strings().quickLaunchSub,
        icon = Icons.Rounded.RocketLaunch,
        accent = UiPalette.Orange,
        modifier = modifier,
        trailing = onConfigure?.let { configure -> { PanelEditChip(configure) } },
    ) {
        TileGrid(
            columns = 3,
            tiles = apps.map { app ->
                TileSpec(
                    label = app.name,
                    icon = iconForName(app.iconName),
                    onClick = { onLaunch(app) },
                    accent = parseHexColor(app.accent, UiPalette.Orange),
                )
            },
        )
    }
}

@Composable
fun QuickActionsStrip(
    items: List<QuickActionItem>,
    onAction: (String) -> Unit,
    onConfigure: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // The user's rows decide the order; the grid itself wraps to whatever width the panel has.
    val enabled = items.filter { it.enabled }.sortedWith(compareBy({ it.row }, { it.sortOrder }))
    PanelCard(
        title = strings().quickActions,
        subtitle = strings().quickActionsSub,
        icon = Icons.Rounded.Bolt,
        accent = UiPalette.Amber,
        modifier = modifier,
        trailing = onConfigure?.let { configure -> { PanelEditChip(configure) } },
    ) {
        val tiles = enabled.mapNotNull { item ->
            val spec = quickActionSpec(item.actionKey) ?: return@mapNotNull null
            TileSpec(spec.label, spec.icon, { onAction(item.actionKey) })
        }
        if (tiles.isNotEmpty()) TileGrid(tiles, columns = 4)
    }
}

private data class QuickActionSpec(val label: String, val icon: ImageVector)

@Composable
private fun quickActionSpec(key: String): QuickActionSpec? {
    val s = strings()
    return when (key) {
        QuickActionKeys.LOCK -> QuickActionSpec(s.lock, Icons.Rounded.Lock)
        QuickActionKeys.PASTE -> QuickActionSpec(s.paste, Icons.Rounded.ContentPaste)
        QuickActionKeys.DESKTOP -> QuickActionSpec(s.desktop, Icons.Rounded.DesktopWindows)
        QuickActionKeys.SYNC_VOLUME -> QuickActionSpec(s.sync, Icons.Rounded.Refresh)
        QuickActionKeys.COPY -> QuickActionSpec(s.copy, Icons.Rounded.ContentCopy)
        QuickActionKeys.ALT_TAB -> QuickActionSpec(s.altTab, Icons.Rounded.SwapHoriz)
        QuickActionKeys.CLOSE_WINDOW -> QuickActionSpec(s.closeWin, Icons.Rounded.Close)
        QuickActionKeys.FULLSCREEN -> QuickActionSpec(s.fullscreen, Icons.Rounded.Fullscreen)
        QuickActionKeys.REFRESH -> QuickActionSpec(s.refresh, Icons.Rounded.Refresh)
        QuickActionKeys.UNDO -> QuickActionSpec(s.undo, Icons.Rounded.Undo)
        QuickActionKeys.CUT -> QuickActionSpec(s.cut, Icons.Rounded.ContentCut)
        QuickActionKeys.REDO -> QuickActionSpec(s.redo, Icons.Rounded.Redo)
        QuickActionKeys.SAVE -> QuickActionSpec(s.save, Icons.Rounded.Save)
        QuickActionKeys.SEARCH -> QuickActionSpec(s.search, Icons.Rounded.Search)
        QuickActionKeys.NEW_TAB -> QuickActionSpec(s.newTab, Icons.Rounded.KeyboardTab)
        QuickActionKeys.SCREENSHOT -> QuickActionSpec(s.screenshot, Icons.Rounded.CameraAlt)
        QuickActionKeys.TASK_MANAGER -> QuickActionSpec(s.taskManager, Icons.Rounded.QueryStats)
        QuickActionKeys.SNAP_LEFT -> QuickActionSpec(s.snapLeft, Icons.AutoMirrored.Rounded.ArrowBack)
        QuickActionKeys.SNAP_RIGHT -> QuickActionSpec(s.snapRight, Icons.AutoMirrored.Rounded.ArrowForward)
        QuickActionKeys.PLAY_PAUSE -> QuickActionSpec(s.media, Icons.Rounded.PlayArrow)
        QuickActionKeys.PREVIOUS_TRACK -> QuickActionSpec(s.previousTrack, Icons.Rounded.SkipPrevious)
        QuickActionKeys.NEXT_TRACK -> QuickActionSpec(s.nextTrack, Icons.Rounded.SkipNext)
        else -> null
    }
}

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
    val s = strings()
    PanelCard(
        title = s.windowsSystem,
        subtitle = null,
        icon = Icons.Rounded.DesktopWindows,
        accent = UiPalette.Blue,
        modifier = modifier,
    ) {
        GroupLabel(tr("Окна", "Windows"))
        TileGrid(
            columns = 4,
            tiles = listOf(
                TileSpec(s.snapLeft, Icons.AutoMirrored.Rounded.ArrowBack, onSnapLeft),
                TileSpec(s.snapRight, Icons.AutoMirrored.Rounded.ArrowForward, onSnapRight),
                TileSpec(s.maximize, Icons.Rounded.OpenInFull, onMaximize),
                TileSpec(s.minimizeWin, Icons.Rounded.Minimize, onMinimizeActive),
                TileSpec(s.minimizeAll, Icons.Rounded.CloseFullscreen, onMinimizeAll),
                TileSpec(s.altTab, Icons.Rounded.SwapHoriz, onAltTab),
                TileSpec(s.fullscreen, Icons.Rounded.Fullscreen, onFullscreen),
                TileSpec(s.closeWin, Icons.Rounded.Close, onCloseActive, accent = UiPalette.Red),
            ),
        )

        GroupLabel(s.moveToMonitor)
        TileGrid(
            columns = 3,
            tiles = (1..3).map { n -> TileSpec(s.monitor(n), Icons.Rounded.Monitor, { onMoveToMonitor(n) }) },
        )

        GroupLabel(tr("Система", "System"))
        TileGrid(
            columns = 4,
            tiles = listOf(
                TileSpec(s.screenshot, Icons.Rounded.CameraAlt, onScreenshot),
                TileSpec(s.lock, Icons.Rounded.Lock, onLock),
                TileSpec(s.taskManager, Icons.Rounded.QueryStats, onTaskManager),
                TileSpec(s.killProcess, Icons.Rounded.Close, onKillProcess, accent = UiPalette.Red),
                TileSpec(s.sleep, Icons.Rounded.Bedtime, onSleep, onLongClick = onSleepLongPress, accent = UiPalette.Violet),
            ),
        )
        Text(
            s.windowsSystemSub,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    PanelCard(
        title = strings().navigation,
        subtitle = strings().navigationSub,
        icon = Icons.Rounded.Folder,
        accent = UiPalette.Lime,
        modifier = modifier,
        trailing = onConfigure?.let { configure -> { PanelEditChip(configure) } },
    ) {
        if (folders.isNotEmpty()) {
            GroupLabel(tr("Папки", "Folders"))
            TileGrid(
                columns = 3,
                tiles = folders.map { folder -> TileSpec(folder.name, Icons.Rounded.Folder, { onOpenFolder(folder) }, accent = UiPalette.Lime) },
            )
        }
        if (urlGroups.isNotEmpty()) {
            GroupLabel(tr("Сайты", "Sites"))
            TileGrid(
                columns = 3,
                tiles = urlGroups.map { group -> TileSpec(group.name, Icons.Rounded.Public, { onOpenUrlGroup(group) }, accent = UiPalette.Blue) },
            )
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
    PanelCard(
        title = s.hwMonitor,
        subtitle = if (!foregroundProcess.isNullOrBlank()) s.foregroundApp(foregroundProcess) else s.hwMonitorSub,
        icon = Icons.Rounded.Memory,
        accent = UiPalette.Cyan,
        modifier = modifier,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatMeter(s.cpu, cpuPercent, Modifier.weight(1f), accent = UiPalette.Cyan)
            StatMeter(s.ram, ramPercent, Modifier.weight(1f), accent = UiPalette.Violet)
        }
        if (downloadMbps != null || uploadMbps != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoPill(
                    label = networkInterface ?: tr("Сеть", "Network"),
                    value = s.networkDown(downloadMbps?.let { "%.1f".format(it) } ?: "—"),
                    modifier = Modifier.weight(1f),
                )
                InfoPill(
                    label = tr("Отдача", "Upload"),
                    value = s.networkUp(uploadMbps?.let { "%.1f".format(it) } ?: "—"),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (storageDrives.isNotEmpty()) {
            GroupLabel(s.storage)
            storageDrives.take(5).forEach { drive ->
                val usedPercent = (100.0 - drive.freePercent).toFloat()
                val label = buildString {
                    append(drive.name)
                    if (!drive.label.isNullOrBlank()) append(" · ").append(drive.label)
                    append(" · ").append("%.1f GB".format(drive.freeGb)).append(" ").append(s.storageFree)
                }
                StatMeter(label, usedPercent, Modifier.fillMaxWidth(), accent = UiPalette.Green)
            }
        } else {
            diskFreePercent?.let { free ->
                StatMeter(s.disk, 100f - free, Modifier.fillMaxWidth(), accent = UiPalette.Green)
            }
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
    PanelCard(
        title = s.clipboard,
        subtitle = s.clipboardSub,
        icon = Icons.Rounded.ContentPaste,
        accent = UiPalette.Blue,
        modifier = modifier,
    ) {
        if (!preview.isNullOrBlank()) {
            Text(
                preview,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BigButton(s.pasteFromPc, onPasteFromPc, Modifier.fillMaxWidth(), icon = Icons.Rounded.ContentPaste, filled = false)
    }
}
