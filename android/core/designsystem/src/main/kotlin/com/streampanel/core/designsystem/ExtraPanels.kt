package com.streampanel.core.designsystem

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.HeadsetOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.streampanel.core.model.GameOverlaySettings
import com.streampanel.core.model.GameTelemetryInfo
import com.streampanel.core.model.PcProcessInfo
import com.streampanel.core.model.StreamChatSettings
import com.streampanel.core.model.TimeTrackerProject

private val DiscordBlue = Color(0xFF5865F2)
private val TwitchPurple = Color(0xFF9146FF)

@Composable
fun DiscordPanel(
    onOpenDiscord: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleDeafen: () -> Unit,
    onPushToTalk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = strings()
    PanelCard(s.discordTitle, Icons.Rounded.Headset, modifier, s.discordSub, DiscordBlue) {
        BigButton(s.discordOpen, onOpenDiscord, Modifier.fillMaxWidth(), icon = Icons.Rounded.OpenInNew, filled = false)
        TileGrid(
            columns = 3,
            tiles = listOf(
                TileSpec(s.discordMute, Icons.Rounded.MicOff, onToggleMute, accent = DiscordBlue),
                TileSpec(s.discordDeafen, Icons.Rounded.HeadsetOff, onToggleDeafen, accent = DiscordBlue),
                TileSpec(s.discordPtt, Icons.Rounded.Mic, onPushToTalk, accent = DiscordBlue),
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PomodoroPanel(
    remainingSeconds: Int,
    totalSeconds: Int,
    running: Boolean,
    focusMinutes: Int,
    onFocusMinutesChange: (Int) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = strings()
    PanelCard(
        title = s.pomodoroTitle,
        icon = Icons.Rounded.Timer,
        modifier = modifier,
        subtitle = if (running) s.pomodoroFocus else s.pomodoroIdle,
        accent = UiPalette.Orange,
    ) {
        val progress = if (totalSeconds > 0) remainingSeconds / totalSeconds.toFloat() else 0f
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(168.dp)) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(168.dp),
                    strokeWidth = 10.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                )
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.size(168.dp),
                    strokeWidth = 10.dp,
                    color = UiPalette.Orange,
                    trackColor = Color.Transparent,
                )
                Text(
                    text = formatTimer(remainingSeconds),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (!running) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(5, 15, 25, 45, 60).forEach { minutes ->
                    ChoicePill("$minutes ${tr("мин", "min")}", selected = focusMinutes == minutes, onClick = { onFocusMinutesChange(minutes) })
                }
            }
            BigButton(s.pomodoroStart, onStart, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow, color = UiPalette.Orange)
        } else {
            BigButton(s.pomodoroStop, onStop, Modifier.fillMaxWidth(), icon = Icons.Rounded.Stop, filled = false)
        }
    }
}

@Composable
fun TimeTrackerPanel(
    projects: List<TimeTrackerProject>,
    activeProjectId: String?,
    elapsedSeconds: Int,
    onStart: (String) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = strings()
    PanelCard(
        title = s.timeTrackerTitle,
        icon = Icons.Rounded.Schedule,
        modifier = modifier,
        subtitle = if (activeProjectId != null) null else s.timeTrackerIdle,
        accent = UiPalette.Amber,
        trailing = if (activeProjectId != null) {
            {
                Text(
                    formatTimer(elapsedSeconds),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = UiPalette.Amber,
                )
            }
        } else null,
    ) {
        TileGrid(
            columns = 3,
            tiles = projects.filter { it.enabled }.map { project ->
                val active = project.id == activeProjectId
                TileSpec(
                    label = project.name,
                    icon = if (active) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    onClick = { if (active) onStop() else onStart(project.id) },
                    active = active,
                    accent = parseHexColor(project.color, UiPalette.Amber),
                )
            },
        )
        if (activeProjectId != null) {
            BigButton(s.timeTrackerStopLog, onStop, Modifier.fillMaxWidth(), icon = Icons.Rounded.Stop, filled = false)
        }
    }
}

@Composable
fun ProcessMonitorPanel(
    processes: List<PcProcessInfo>,
    onKillProcess: (Int) -> Unit,
    onCleanTemp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = strings()
    var pending by remember { mutableStateOf<PcProcessInfo?>(null) }
    PanelCard(s.processTitle, Icons.Rounded.QueryStats, modifier, s.processSub, UiPalette.Red) {
        processes.forEach { proc ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        proc.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${proc.memoryMb} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    onClick = { pending = proc },
                    shape = CircleShape,
                    color = UiPalette.Red.copy(alpha = 0.16f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, contentDescription = tr("Завершить", "End"), tint = UiPalette.Red)
                    }
                }
            }
        }
        BigButton(s.cleanTemp, onCleanTemp, Modifier.fillMaxWidth(), icon = Icons.Rounded.CleaningServices, filled = false)
    }

    pending?.let { proc ->
        AlertDialog(
            onDismissRequest = { pending = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(tr("Завершить «${proc.name}»?", "End “${proc.name}”?")) },
            text = { Text(tr("Несохранённые данные в этой программе пропадут.", "Unsaved data in that program will be lost.")) },
            confirmButton = {
                TextButton(onClick = { onKillProcess(proc.pid); pending = null }) {
                    Text(tr("Завершить", "End"), color = UiPalette.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(strings().cancel) } },
        )
    }
}

@Composable
fun DevToolsPanel(
    onGitPull: () -> Unit,
    onGitStatus: () -> Unit,
    onDockerPs: () -> Unit,
    onDockerUp: () -> Unit,
    onVscodeTest: () -> Unit,
    onVscodeFormat: () -> Unit,
    onVscodeTerminal: () -> Unit,
    onWifiRestart: () -> Unit,
    onFlushDns: () -> Unit,
    onEmptyRecycleBin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = strings()
    PanelCard(s.devToolsTitle, Icons.Rounded.Code, modifier, s.devToolsSub, UiPalette.Green) {
        GroupLabel("Git")
        TileGrid(
            columns = 3,
            tiles = listOf(
                TileSpec(s.gitPull, Icons.Rounded.CloudDownload, onGitPull),
                TileSpec(s.gitStatus, Icons.Rounded.Info, onGitStatus),
            ),
        )
        GroupLabel("Docker")
        TileGrid(
            columns = 3,
            tiles = listOf(
                TileSpec(s.dockerPs, Icons.Rounded.Inventory2, onDockerPs),
                TileSpec(s.dockerUp, Icons.Rounded.PlayCircle, onDockerUp),
            ),
        )
        GroupLabel("VS Code")
        TileGrid(
            columns = 3,
            tiles = listOf(
                TileSpec(s.vscodeTest, Icons.Rounded.CheckCircle, onVscodeTest),
                TileSpec(s.vscodeFormat, Icons.Rounded.FormatAlignLeft, onVscodeFormat),
                TileSpec(s.vscodeTerminal, Icons.Rounded.Terminal, onVscodeTerminal),
            ),
        )
        GroupLabel(tr("Сеть и система", "Network and system"))
        TileGrid(
            columns = 3,
            tiles = listOf(
                TileSpec(s.wifiRestart, Icons.Rounded.Wifi, onWifiRestart),
                TileSpec(s.flushDns, Icons.Rounded.Dns, onFlushDns),
                TileSpec(s.emptyRecycleBin, Icons.Rounded.DeleteSweep, onEmptyRecycleBin),
            ),
        )
    }
}

@Composable
fun StreamToolsPanel(
    onOpenObs: () -> Unit,
    onOpenTwitch: () -> Unit,
    onOpenYoutube: () -> Unit,
    onOpenChat: () -> Unit,
    onSaveReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = strings()
    PanelCard(s.streamToolsTitle, Icons.Rounded.LiveTv, modifier, s.streamToolsSub, UiPalette.Pink) {
        Text(s.streamToolsHint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        TileGrid(
            columns = 3,
            tiles = listOf(
                TileSpec("OBS", Icons.Rounded.Videocam, onOpenObs, accent = UiPalette.Pink),
                TileSpec(s.twitchDashboard, Icons.Rounded.LiveTv, onOpenTwitch, accent = TwitchPurple),
                TileSpec(s.youtubeStudio, Icons.Rounded.PlayCircle, onOpenYoutube, accent = UiPalette.Red),
                TileSpec(s.openChat, Icons.Rounded.Chat, onOpenChat),
                TileSpec(s.replayClip, Icons.Rounded.Replay, onSaveReplay),
            ),
        )
    }
}

@Composable
fun StreamChatPanel(
    settings: StreamChatSettings,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = strings()
    PanelCard(s.streamChatTitle, Icons.Rounded.Chat, modifier, s.streamChatSub, TwitchPurple) {
        val nothingSet = settings.twitchChannel.isBlank() && settings.youtubeVideoId.isBlank()
        if (nothingSet || !settings.embedChatInDashboard) {
            Text(s.streamChatHint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            BigButton(
                text = if (nothingSet) s.settings else s.streamChatSettings,
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Tune,
                filled = false,
            )
        } else {
            if (settings.showTwitchChat && settings.twitchChannel.isNotBlank()) {
                GroupLabel("Twitch · ${settings.twitchChannel}")
                EmbeddedChatWebView(twitchChatUrl(settings.twitchChannel))
            }
            if (settings.showYoutubeChat && settings.youtubeVideoId.isNotBlank()) {
                GroupLabel("YouTube · ${settings.youtubeVideoId}")
                EmbeddedChatWebView(youtubeChatUrl(settings.youtubeVideoId))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameStatusPanel(
    settings: GameOverlaySettings,
    gameInfo: GameTelemetryInfo,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = strings()
    PanelCard(s.gameStatusTitle, Icons.Rounded.SportsEsports, modifier, s.gameStatusSub, UiPalette.Red) {
        if (!settings.enabled) {
            Text(s.gameStatusHint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            BigButton(s.settings, onOpenSettings, Modifier.fillMaxWidth(), icon = Icons.Rounded.Tune, filled = false)
            return@PanelCard
        }

        Text(
            gameInfo.provider ?: gameInfo.processName ?: s.gameNotDetected,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!gameInfo.detected) {
            Text(
                "${s.gameAutoProcesses}: ${settings.autoShowProcessPatterns}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        gameInfo.note?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            gameInfo.processName?.let { InfoPill("EXE", it) }
            if (settings.showMap) {
                InfoPill(s.map, gameInfo.mapName ?: "—")
                InfoPill(s.phase, gameInfo.phase ?: "—")
            }
            if (settings.showHealth) {
                InfoPill(s.health, gameInfo.health?.toString() ?: "—")
                InfoPill(s.armor, gameInfo.armor?.toString() ?: "—")
            }
            if (settings.showAmmo) {
                val ammo = listOfNotNull(gameInfo.ammoClip, gameInfo.ammoReserve).joinToString("/")
                InfoPill(s.ammo, ammo.ifBlank { "—" })
            }
            if (settings.showScore) {
                val score = if (gameInfo.teamScore != null || gameInfo.enemyScore != null) {
                    "${gameInfo.teamScore ?: 0}:${gameInfo.enemyScore ?: 0}"
                } else {
                    "—"
                }
                InfoPill(s.score, score)
            }
        }
        BigButton(s.gameOverlaySettings, onOpenSettings, Modifier.fillMaxWidth(), icon = Icons.Rounded.Tune, filled = false)
    }
}

@Composable
fun PcConfiguratorPanel(
    url: String?,
    onOpenConfigurator: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = strings()
    PanelCard(s.pcConfiguratorTitle, Icons.Rounded.Settings, modifier, s.pcConfiguratorSub, UiPalette.Cyan) {
        Text(s.pcConfiguratorHint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(
            url ?: "http://<pc-ip>:17820",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BigButton(s.openPcConfigurator, onOpenConfigurator, Modifier.fillMaxWidth(), enabled = !url.isNullOrBlank())
        BigButton(s.settings, onOpenSettings, Modifier.fillMaxWidth(), filled = false)
    }
}

@Composable
fun StudyModePanel(
    onStartFocus: () -> Unit,
    onOpenStudyPack: () -> Unit,
    onOpenNotes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = strings()
    PanelCard(s.studyModeTitle, Icons.Rounded.School, modifier, s.studyModeSub, UiPalette.Blue) {
        Text(s.studyModeHint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        TileGrid(
            columns = 3,
            tiles = listOf(
                TileSpec(s.startFocus, Icons.Rounded.Timer, onStartFocus),
                TileSpec(s.studyPack, Icons.Rounded.MenuBook, onOpenStudyPack),
                TileSpec(s.notes, Icons.Rounded.EditNote, onOpenNotes),
            ),
        )
    }
}

@Composable
fun MeetingModePanel(
    onMicMute: () -> Unit,
    onOpenDiscord: () -> Unit,
    onOpenTeams: () -> Unit,
    onOpenZoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = strings()
    PanelCard(s.meetingModeTitle, Icons.Rounded.Videocam, modifier, s.meetingModeSub, UiPalette.Green) {
        Text(s.meetingModeHint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        TileGrid(
            columns = 4,
            tiles = listOf(
                TileSpec(s.micMute, Icons.Rounded.Mic, onMicMute),
                TileSpec("Discord", Icons.Rounded.Headset, onOpenDiscord, accent = DiscordBlue),
                TileSpec(s.teams, Icons.Rounded.Groups, onOpenTeams),
                TileSpec(s.zoom, Icons.Rounded.Videocam, onOpenZoom),
            ),
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbeddedChatWebView(
    url: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(16.dp)),
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url) webView.loadUrl(url)
        },
    )
}

fun twitchChatUrl(channel: String): String {
    val clean = channel.trim().removePrefix("@")
    return "https://www.twitch.tv/popout/$clean/chat?popout="
}

fun youtubeChatUrl(videoId: String): String {
    return "https://www.youtube.com/live_chat?v=${videoId.trim()}&embed_domain=www.youtube.com"
}

private fun formatTimer(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
