package com.streampanel.core.designsystem

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HeadsetOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.streampanel.core.model.GameOverlaySettings
import com.streampanel.core.model.GameTelemetryInfo
import com.streampanel.core.model.PcProcessInfo
import com.streampanel.core.model.StreamChatSettings
import com.streampanel.core.model.TimeTrackerProject

@Composable
fun DiscordPanel(
    onOpenDiscord: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleDeafen: () -> Unit,
    onPushToTalk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = strings()
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(s.discordTitle, s.discordSub)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenDiscord, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.OpenInNew, null, tint = panelIconTint())
                    Text(" ${s.discordOpen}", color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolChip(s.discordMute, Icons.Default.MicOff, onToggleMute, Modifier.weight(1f))
                ToolChip(s.discordDeafen, Icons.Default.HeadsetOff, onToggleDeafen, Modifier.weight(1f))
                ToolChip(s.discordPtt, Icons.Default.Mic, onPushToTalk, Modifier.weight(1f))
            }
        }
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
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(s.pomodoroTitle, if (running) s.pomodoroFocus else s.pomodoroIdle)
            val progress = if (totalSeconds > 0) remainingSeconds / totalSeconds.toFloat() else 0f
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(
                text = formatTimer(remainingSeconds),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 15, 25, 45, 60).forEach { minutes ->
                    FilterChip(
                        selected = focusMinutes == minutes,
                        enabled = !running,
                        onClick = { onFocusMinutesChange(minutes) },
                        label = { Text("${minutes}m") },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart, enabled = !running, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Timer, null)
                    Text(" ${s.pomodoroStart}")
                }
                OutlinedButton(onClick = onStop, enabled = running, modifier = Modifier.weight(1f)) {
                    Text(s.pomodoroStop, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(s.timeTrackerTitle, if (activeProjectId != null) formatTimer(elapsedSeconds) else s.timeTrackerIdle)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                projects.filter { it.enabled }.forEach { project ->
                    val color = parseHexColor(project.color, MaterialTheme.colorScheme.primary)
                    FilterChip(
                        selected = project.id == activeProjectId,
                        onClick = {
                            if (project.id == activeProjectId) onStop() else onStart(project.id)
                        },
                        label = { Text(project.name, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.28f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            }
            if (activeProjectId != null) {
                OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                    Text(s.timeTrackerStopLog, color = MaterialTheme.colorScheme.onSurface)
                }
            }
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
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(s.processTitle, s.processSub)
            processes.forEach { proc ->
                OutlinedButton(
                    onClick = { onKillProcess(proc.pid) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "${proc.name} · ${proc.memoryMb} MB",
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
            OutlinedButton(onClick = onCleanTemp, modifier = Modifier.fillMaxWidth()) {
                Text(s.cleanTemp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(s.devToolsTitle, s.devToolsSub)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    s.gitPull to onGitPull,
                    s.gitStatus to onGitStatus,
                    s.dockerPs to onDockerPs,
                    s.dockerUp to onDockerUp,
                    s.vscodeTest to onVscodeTest,
                    s.vscodeFormat to onVscodeFormat,
                    s.vscodeTerminal to onVscodeTerminal,
                    s.wifiRestart to onWifiRestart,
                    s.flushDns to onFlushDns,
                    s.emptyRecycleBin to onEmptyRecycleBin,
                ).forEach { (label, action) ->
                    OutlinedButton(onClick = action) { Text(label, maxLines = 1) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(s.streamToolsTitle, s.streamToolsSub)
            Text(s.streamToolsHint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenObs) { Text("OBS") }
                OutlinedButton(onClick = onOpenTwitch) { Text(s.twitchDashboard) }
                OutlinedButton(onClick = onOpenYoutube) { Text(s.youtubeStudio) }
                OutlinedButton(onClick = onOpenChat) { Text(s.openChat) }
                OutlinedButton(onClick = onSaveReplay) { Text(s.replayClip) }
            }
        }
    }
}

@Composable
fun StreamChatPanel(
    settings: StreamChatSettings,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = strings()
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(s.streamChatTitle, s.streamChatSub)
            if (settings.twitchChannel.isBlank() && settings.youtubeVideoId.isBlank()) {
                Text(s.streamChatHint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text(s.settings, color = MaterialTheme.colorScheme.onSurface)
                }
                return@Column
            }

            if (!settings.embedChatInDashboard) {
                Text(s.streamChatHint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text(s.streamChatSettings, color = MaterialTheme.colorScheme.onSurface)
                }
                return@Column
            }

            if (settings.showTwitchChat && settings.twitchChannel.isNotBlank()) {
                Text("Twitch · ${settings.twitchChannel}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                EmbeddedChatWebView(twitchChatUrl(settings.twitchChannel))
            }
            if (settings.showYoutubeChat && settings.youtubeVideoId.isNotBlank()) {
                Text("YouTube · ${settings.youtubeVideoId}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
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
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(s.gameStatusTitle, s.gameStatusSub)
            if (!settings.enabled) {
                Text(s.gameStatusHint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text(s.settings, color = MaterialTheme.colorScheme.onSurface)
                }
                return@Column
            }

            Text(
                gameInfo.provider ?: gameInfo.processName ?: s.gameNotDetected,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
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
                gameInfo.processName?.let { InfoChip("EXE", it) }
                if (settings.showMap) {
                    InfoChip(s.map, gameInfo.mapName ?: "—")
                    InfoChip(s.phase, gameInfo.phase ?: "—")
                }
                if (settings.showHealth) {
                    InfoChip(s.health, gameInfo.health?.toString() ?: "—")
                    InfoChip(s.armor, gameInfo.armor?.toString() ?: "—")
                }
                if (settings.showAmmo) {
                    val ammo = listOfNotNull(gameInfo.ammoClip, gameInfo.ammoReserve).joinToString("/")
                    InfoChip(s.ammo, ammo.ifBlank { "—" })
                }
                if (settings.showScore) {
                    val score = if (gameInfo.teamScore != null || gameInfo.enemyScore != null) {
                        "${gameInfo.teamScore ?: 0}:${gameInfo.enemyScore ?: 0}"
                    } else {
                        "—"
                    }
                    InfoChip(s.score, score)
                }
            }
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(s.gameOverlaySettings, color = MaterialTheme.colorScheme.onSurface)
            }
        }
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
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(s.pcConfiguratorTitle, s.pcConfiguratorSub)
            Text(s.pcConfiguratorHint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text(url ?: "http://<pc-ip>:17820", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenConfigurator, enabled = !url.isNullOrBlank(), modifier = Modifier.weight(1f)) {
                    Text(s.openPcConfigurator)
                }
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                    Text(s.settings, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyModePanel(
    onStartFocus: () -> Unit,
    onOpenStudyPack: () -> Unit,
    onOpenNotes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = strings()
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(s.studyModeTitle, s.studyModeSub)
            Text(s.studyModeHint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onStartFocus) { Text(s.startFocus) }
                OutlinedButton(onClick = onOpenStudyPack) { Text(s.studyPack) }
                OutlinedButton(onClick = onOpenNotes) { Text(s.notes) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MeetingModePanel(
    onMicMute: () -> Unit,
    onOpenDiscord: () -> Unit,
    onOpenTeams: () -> Unit,
    onOpenZoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = strings()
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(s.meetingModeTitle, s.meetingModeSub)
            Text(s.meetingModeHint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onMicMute) { Text(s.micMute) }
                OutlinedButton(onClick = onOpenDiscord) { Text("Discord") }
                OutlinedButton(onClick = onOpenTeams) { Text(s.teams) }
                OutlinedButton(onClick = onOpenZoom) { Text(s.zoom) }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbeddedChatWebView(
    url: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.fillMaxWidth().height(320.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = panelIconTint())
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}
