package com.streampanel.feature.obs

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import com.streampanel.core.designsystem.WindowWidthSizeClass
import com.streampanel.core.designsystem.toWidthSizeClass
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streampanel.core.designsystem.AppBackdrop
import com.streampanel.core.designsystem.EmbeddedChatWebView
import com.streampanel.core.designsystem.GlassSurface
import com.streampanel.core.designsystem.SectionHeader
import com.streampanel.core.designsystem.StreamPanelTextField
import com.streampanel.core.designsystem.strings
import com.streampanel.core.designsystem.twitchChatUrl
import com.streampanel.core.designsystem.youtubeChatUrl
import com.streampanel.core.model.StreamChatSettings

@Composable
fun ObsRoute(
    onBack: () -> Unit,
    viewModel: ObsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DisposableEffect(Unit) {
        viewModel.startStatusPolling()
        onDispose { viewModel.stopStatusPolling() }
    }
    ObsScreen(
        state = state,
        onBack = onBack,
        onUrlChange = viewModel::updateUrl,
        onPasswordChange = viewModel::updatePassword,
        onRefresh = viewModel::refreshAll,
        onSwitchScene = viewModel::switchScene,
        onPreviewScene = viewModel::previewScene,
        onToggleStream = viewModel::toggleStream,
        onStartStream = viewModel::startStream,
        onStopStream = viewModel::stopStream,
        onToggleRecord = viewModel::toggleRecord,
        onStartRecord = viewModel::startRecord,
        onStopRecord = viewModel::stopRecord,
        onPauseRecord = viewModel::pauseRecord,
        onToggleReplay = viewModel::toggleReplayBuffer,
        onSaveReplay = viewModel::saveReplayBuffer,
        onToggleVirtualCam = viewModel::toggleVirtualCam,
        onToggleStudio = viewModel::toggleStudioMode,
        onTransitionStudio = viewModel::transitionStudio,
        onToggleInputMute = viewModel::toggleInputMute,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ObsScreen(
    state: ObsUiState,
    onBack: () -> Unit,
    onUrlChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSwitchScene: (String) -> Unit,
    onPreviewScene: (String) -> Unit,
    onToggleStream: () -> Unit,
    onStartStream: () -> Unit,
    onStopStream: () -> Unit,
    onToggleRecord: () -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onPauseRecord: () -> Unit,
    onToggleReplay: () -> Unit,
    onSaveReplay: () -> Unit,
    onToggleVirtualCam: () -> Unit,
    onToggleStudio: () -> Unit,
    onTransitionStudio: () -> Unit,
    onToggleInputMute: (String) -> Unit,
) {
    val s = strings()
    val configuration = LocalConfiguration.current
    val widthClass = configuration.screenWidthDp.dp.toWidthSizeClass()
    val contentPadding = when (widthClass) {
        WindowWidthSizeClass.Compact -> 12.dp
        WindowWidthSizeClass.Medium -> 18.dp
        WindowWidthSizeClass.Expanded -> 24.dp
    }
    AppBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(s.obsStudio, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                        }
                    },
                    actions = {
                        if (state.working) {
                            CircularProgressIndicator(Modifier.padding(end = 16.dp))
                        } else {
                            IconButton(onClick = onRefresh) {
                                Icon(Icons.Default.Refresh, contentDescription = s.obsRefresh, tint = Color.White)
                            }
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ObsHeroSection(
                    state = state,
                    widthClass = widthClass,
                    onToggleStream = onToggleStream,
                    onStartStream = onStartStream,
                    onStopStream = onStopStream,
                    onToggleRecord = onToggleRecord,
                    onStartRecord = onStartRecord,
                    onStopRecord = onStopRecord,
                    onPauseRecord = onPauseRecord,
                    onToggleReplay = onToggleReplay,
                    onSaveReplay = onSaveReplay,
                    onToggleVirtualCam = onToggleVirtualCam,
                    onToggleStudio = onToggleStudio,
                    onTransitionStudio = onTransitionStudio,
                )

                if (state.scenes.isNotEmpty()) {
                    ObsScenesSection(
                        state = state,
                        widthClass = widthClass,
                        onSwitchScene = onSwitchScene,
                        onPreviewScene = onPreviewScene,
                    )
                }

                ObsConnectionSection(
                    state = state,
                    onUrlChange = onUrlChange,
                    onPasswordChange = onPasswordChange,
                    onRefresh = onRefresh,
                )

                ObsSourcesSection(state.inputs, onToggleInputMute)

                ObsStreamChatSection(state.streamChatSettings)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ObsHeroSection(
    state: ObsUiState,
    widthClass: WindowWidthSizeClass,
    onToggleStream: () -> Unit,
    onStartStream: () -> Unit,
    onStopStream: () -> Unit,
    onToggleRecord: () -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onPauseRecord: () -> Unit,
    onToggleReplay: () -> Unit,
    onSaveReplay: () -> Unit,
    onToggleVirtualCam: () -> Unit,
    onToggleStudio: () -> Unit,
    onTransitionStudio: () -> Unit,
) {
    val programScene = state.sceneByName(state.currentScene)
    val previewScene = state.sceneByName(state.currentPreviewScene)
    val sectionPadding = when (widthClass) {
        WindowWidthSizeClass.Compact -> 14.dp
        WindowWidthSizeClass.Medium -> 18.dp
        WindowWidthSizeClass.Expanded -> 20.dp
    }
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(sectionPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "OBS Studio Control Room",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                StatusBadge(
                    label = if (state.streaming) "LIVE" else "OFF AIR",
                    color = if (state.streaming) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                )
                StatusBadge(
                    label = recordStatusText(state),
                    color = when {
                        state.recordingPaused -> MaterialTheme.colorScheme.tertiary
                        state.recording -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                )
            }

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stacked = maxWidth < 720.dp
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ObsMonitorPanel(
                            title = "PROGRAM",
                            scene = programScene,
                            fallbackSceneName = state.currentScene ?: "No scene",
                            accent = if (state.streaming) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ObsMonitorPanel(
                            title = "PREVIEW",
                            scene = previewScene,
                            fallbackSceneName = state.currentPreviewScene ?: "Select scene",
                            accent = MaterialTheme.colorScheme.tertiary,
                            dimmed = !state.studioModeEnabled,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ObsControlPanel(
                            state = state,
                            widthClass = widthClass,
                            onToggleStream = onToggleStream,
                            onStartStream = onStartStream,
                            onStopStream = onStopStream,
                            onToggleRecord = onToggleRecord,
                            onStartRecord = onStartRecord,
                            onStopRecord = onStopRecord,
                            onPauseRecord = onPauseRecord,
                            onToggleReplay = onToggleReplay,
                            onSaveReplay = onSaveReplay,
                            onToggleVirtualCam = onToggleVirtualCam,
                            onToggleStudio = onToggleStudio,
                            onTransitionStudio = onTransitionStudio,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        ObsMonitorPanel(
                            title = "PROGRAM",
                            scene = programScene,
                            fallbackSceneName = state.currentScene ?: "No scene",
                            accent = if (state.streaming) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        ObsMonitorPanel(
                            title = "PREVIEW",
                            scene = previewScene,
                            fallbackSceneName = state.currentPreviewScene ?: "Select scene",
                            accent = MaterialTheme.colorScheme.tertiary,
                            dimmed = !state.studioModeEnabled,
                            modifier = Modifier.weight(1f),
                        )
                        ObsControlPanel(
                            state = state,
                            widthClass = widthClass,
                            onToggleStream = onToggleStream,
                            onStartStream = onStartStream,
                            onStopStream = onStopStream,
                            onToggleRecord = onToggleRecord,
                            onStartRecord = onStartRecord,
                            onStopRecord = onStopRecord,
                            onPauseRecord = onPauseRecord,
                            onToggleReplay = onToggleReplay,
                            onSaveReplay = onSaveReplay,
                            onToggleVirtualCam = onToggleVirtualCam,
                            onToggleStudio = onToggleStudio,
                            onTransitionStudio = onTransitionStudio,
                            modifier = Modifier.width(320.dp),
                        )
                    }
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HealthChip("FPS", state.activeFps?.let { "%.0f".format(it) } ?: "—")
                HealthChip("Dropped", droppedFramesText(state))
                HealthChip("OBS CPU", state.obsCpuUsage?.let { "%.1f%%".format(it) } ?: "—")
                HealthChip("OBS RAM", state.obsMemoryMb?.let { "%.0f MB".format(it) } ?: "—")
                HealthChip("Service", state.streamServiceName ?: state.streamServiceType ?: "OBS")
                HealthChip("Server", state.streamServer ?: "—")
                HealthChip("Key", state.streamKeyPreview ?: "—")
            }
        }
    }
}

@Composable
private fun ObsMonitorPanel(
    title: String,
    scene: ObsSceneUi?,
    fallbackSceneName: String,
    accent: Color,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    val bitmap = remember(scene?.previewImageData) { decodeObsImageData(scene?.previewImageData) }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = Color.Black.copy(alpha = if (dimmed) 0.30f else 0.48f),
        border = BorderStroke(1.dp, accent.copy(alpha = if (dimmed) 0.18f else 0.65f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusDot(accent.copy(alpha = if (dimmed) 0.35f else 1f))
                Text(title, color = accent.copy(alpha = if (dimmed) 0.55f else 1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    scene?.name ?: fallbackSceneName,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.22f), Color.Black))),
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        "Preview will appear here",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@Composable
private fun ObsControlPanel(
    state: ObsUiState,
    widthClass: WindowWidthSizeClass,
    onToggleStream: () -> Unit,
    onStartStream: () -> Unit,
    onStopStream: () -> Unit,
    onToggleRecord: () -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onPauseRecord: () -> Unit,
    onToggleReplay: () -> Unit,
    onSaveReplay: () -> Unit,
    onToggleVirtualCam: () -> Unit,
    onToggleStudio: () -> Unit,
    onTransitionStudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Управление", streamStatusHint(state))

            if (state.streaming) {
                Button(
                    onClick = onStopStream,
                    enabled = !state.working,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Default.LiveTv, contentDescription = null)
                    Text(" Остановить трансляцию", maxLines = 1)
                }
            } else {
                Button(
                    onClick = onStartStream,
                    enabled = !state.working,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.LiveTv, contentDescription = null)
                    Text(" Начать трансляцию", maxLines = 1)
                }
            }
            OutlinedButton(onClick = onToggleStream, enabled = !state.working, modifier = Modifier.fillMaxWidth()) {
                Text("Переключить эфир", color = Color.White)
            }

            if (state.recording) {
                val recordLabel = when {
                    state.recordingPaused -> "Продолжить запись"
                    else -> "Остановить запись"
                }
                val recordAction = if (state.recordingPaused) onPauseRecord else onStopRecord
                Button(
                    onClick = recordAction,
                    enabled = !state.working,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.recordingPaused) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    ),
                ) {
                    Icon(Icons.Default.FiberManualRecord, contentDescription = null)
                    Text(" $recordLabel", maxLines = 1)
                }
            } else {
                Button(
                    onClick = onStartRecord,
                    enabled = !state.working,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.FiberManualRecord, contentDescription = null)
                    Text(" Начать запись", maxLines = 1)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPauseRecord,
                    enabled = !state.working && state.recording && !state.recordingPaused,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Pause", maxLines = 1)
                }
                OutlinedButton(onClick = onToggleRecord, enabled = !state.working, modifier = Modifier.weight(1f)) {
                    Text("Toggle", maxLines = 1)
                }
            }
            Button(
                onClick = onTransitionStudio,
                enabled = !state.working && state.studioModeEnabled && !state.currentPreviewScene.isNullOrBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Preview → Program")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onToggleStudio, enabled = !state.working, modifier = Modifier.weight(1f)) {
                    Text(if (state.studioModeEnabled) "Studio ON" else "Studio", color = Color.White, maxLines = 1)
                }
                OutlinedButton(onClick = onToggleVirtualCam, enabled = !state.working, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Videocam, null, tint = Color.White)
                    Text(" Cam", color = Color.White, maxLines = 1)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onToggleReplay, enabled = !state.working, modifier = Modifier.weight(1f)) {
                    Text("Replay", color = Color.White, maxLines = 1)
                }
                OutlinedButton(onClick = onSaveReplay, enabled = !state.working, modifier = Modifier.weight(1f)) {
                    Text("Clip", maxLines = 1)
                }
            }
        }
    }
}

private fun streamStatusHint(state: ObsUiState): String = buildString {
    append(if (state.streaming) "Эфир идёт" else "Эфир выключен")
    append(" · ")
    append(
        when {
            state.recordingPaused -> "Запись на паузе"
            state.recording -> "Идёт запись"
            else -> "Запись выключена"
        },
    )
}

@Composable
private fun StatusBadge(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.90f)) {
        Text(
            label,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(color),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ObsScenesSection(
    state: ObsUiState,
    widthClass: WindowWidthSizeClass,
    onSwitchScene: (String) -> Unit,
    onPreviewScene: (String) -> Unit,
) {
    val s = strings()
    val cardWidth = when (widthClass) {
        WindowWidthSizeClass.Compact -> 168.dp
        WindowWidthSizeClass.Medium -> 196.dp
        WindowWidthSizeClass.Expanded -> 220.dp
    }
    val previewHeight = when (widthClass) {
        WindowWidthSizeClass.Compact -> 96.dp
        WindowWidthSizeClass.Medium -> 112.dp
        WindowWidthSizeClass.Expanded -> 128.dp
    }
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(s.obsScenes, if (state.studioModeEnabled) "Нажатие выбирает Preview" else "Нажатие сразу выводит в эфир")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.scenes.forEach { scene ->
                    ObsSceneCard(
                        scene = scene,
                        live = scene.name == state.currentScene,
                        preview = scene.name == state.currentPreviewScene,
                        studioMode = state.studioModeEnabled,
                        cardWidth = cardWidth,
                        previewHeight = previewHeight,
                        onClick = {
                            if (state.studioModeEnabled) {
                                onPreviewScene(scene.name)
                            } else {
                                onSwitchScene(scene.name)
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ObsConnectionSection(
    state: ObsUiState,
    onUrlChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val s = strings()
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Подключение OBS", "редко нужно трогать во время стрима")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HealthChip("Service", state.streamServiceName ?: state.streamServiceType ?: "OBS")
                HealthChip("Server", state.streamServer ?: "—")
                HealthChip("Key", state.streamKeyPreview ?: "—")
            }
            StreamPanelTextField(state.url, onUrlChange, s.obsUrl, Modifier.fillMaxWidth())
            StreamPanelTextField(state.password, onPasswordChange, s.obsPassword, Modifier.fillMaxWidth())
            Button(onClick = onRefresh, enabled = !state.working, modifier = Modifier.fillMaxWidth()) {
                Text(s.obsConnect)
            }
            Text(
                "Если StreamPanel запускает эфир, но OBS показывает ошибку ключа/канала, проверь OBS: Settings -> Stream.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ObsSourcesSection(
    inputs: List<ObsInputUi>,
    onToggleInputMute: (String) -> Unit,
) {
    if (inputs.isEmpty()) return
    val s = strings()
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(s.obsSources, "${inputs.size} inputs")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                inputs.forEach { input ->
                    FilterChip(
                        selected = input.muted,
                        onClick = { onToggleInputMute(input.name) },
                        label = { Text(input.name, maxLines = 1) },
                        leadingIcon = {
                            Icon(
                                if (input.muted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ObsStreamChatSection(settings: StreamChatSettings) {
    val hasTwitch = settings.showTwitchChat && settings.twitchChannel.isNotBlank()
    val hasYoutube = settings.showYoutubeChat && settings.youtubeVideoId.isNotBlank()
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Чат стрима", "Twitch / YouTube")
            if (!hasTwitch && !hasYoutube) {
                Text(
                    "Укажи Twitch-канал или YouTube video id в настройках чата, и чат появится прямо здесь.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                return@Column
            }
            if (hasTwitch) {
                Text("Twitch · ${settings.twitchChannel}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                EmbeddedChatWebView(twitchChatUrl(settings.twitchChannel), modifier = Modifier.height(280.dp))
            }
            if (hasYoutube) {
                Text("YouTube · ${settings.youtubeVideoId}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                EmbeddedChatWebView(youtubeChatUrl(settings.youtubeVideoId), modifier = Modifier.height(280.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ObsSceneCard(
    scene: ObsSceneUi,
    live: Boolean,
    preview: Boolean,
    studioMode: Boolean,
    cardWidth: Dp,
    previewHeight: Dp,
    onClick: () -> Unit,
) {
    val bitmap = remember(scene.previewImageData) { decodeObsImageData(scene.previewImageData) }
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier
            .width(cardWidth)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = when {
            live -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            preview -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.20f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewHeight)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.26f)),
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        "No preview",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                FlowRow(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (live) SceneBadge("LIVE", MaterialTheme.colorScheme.error)
                    if (preview) SceneBadge("PREVIEW", MaterialTheme.colorScheme.tertiary)
                }
            }
            Text(scene.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                if (studioMode) "Нажми для Preview" else "Нажми для эфира",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SceneBadge(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.88f)) {
        Text(
            label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun HealthChip(label: String, value: String) {
    FilterChip(
        selected = false,
        onClick = {},
        label = { Text("$label: $value", maxLines = 1) },
    )
}

private fun droppedFramesText(state: ObsUiState): String {
    val skipped = state.outputSkippedFrames ?: return "—"
    val total = state.outputTotalFrames ?: 0
    val percent = if (total > 0) skipped * 100.0 / total else 0.0
    return "$skipped (${String.format("%.1f", percent)}%)"
}

private fun recordStatusText(state: ObsUiState): String =
    when {
        state.recordingPaused -> "PAUSED"
        state.recording -> state.recordTimecode ?: "REC"
        else -> "REC OFF"
    }

private fun ObsUiState.sceneByName(name: String?): ObsSceneUi? =
    scenes.firstOrNull { it.name == name }

private fun decodeObsImageData(value: String?): ImageBitmap? {
    if (value.isNullOrBlank()) return null
    return runCatching {
        val base64 = value.substringAfter("base64,", value)
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}
