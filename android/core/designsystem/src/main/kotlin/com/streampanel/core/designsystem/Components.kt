package com.streampanel.core.designsystem

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState

import androidx.compose.animation.core.FastOutSlowInEasing

import androidx.compose.animation.core.RepeatMode

import androidx.compose.animation.core.animateFloat

import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.animation.core.infiniteRepeatable

import androidx.compose.animation.core.rememberInfiniteTransition

import androidx.compose.animation.core.tween

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.foundation.background

import androidx.compose.foundation.combinedClickable

import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.interaction.collectIsPressedAsState

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.offset

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.AutoAwesome

import androidx.compose.material.icons.filled.Bolt

import androidx.compose.material.icons.filled.Chat

import androidx.compose.material.icons.filled.Folder

import androidx.compose.material.icons.filled.LiveTv

import androidx.compose.material.icons.filled.MicOff

import androidx.compose.material.icons.filled.MusicNote

import androidx.compose.material.icons.filled.Public

import androidx.compose.material.icons.filled.Settings

import androidx.compose.material.icons.filled.SportsEsports

import androidx.compose.material.icons.filled.Terminal

import androidx.compose.material3.Card

import androidx.compose.material3.CardDefaults

import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.Icon

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Surface

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue

import androidx.compose.runtime.remember

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.platform.LocalView

import androidx.compose.ui.draw.alpha

import androidx.compose.ui.draw.blur

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.drawBehind

import androidx.compose.ui.draw.scale

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.layout.ContentScale

import coil.compose.AsyncImage

import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import com.streampanel.core.model.ButtonState

import com.streampanel.core.model.DashboardButton



@Composable

fun AppBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {

    val extras = LocalStreamPanelExtras.current

    val pulse by rememberInfiniteTransition(label = "backdrop-pulse").animateFloat(

        initialValue = 0.88f,

        targetValue = 1f,

        animationSpec = infiniteRepeatable(

            animation = tween(4200, easing = FastOutSlowInEasing),

            repeatMode = RepeatMode.Reverse,

        ),

        label = "pulse",

    )



    Box(

        modifier = modifier

            .fillMaxSize()

            .background(MaterialTheme.colorScheme.background),

    ) {

        if (extras.backgroundImageUrl.isNotBlank()) {

            AsyncImage(

                model = extras.backgroundImageUrl,

                contentDescription = null,

                modifier = Modifier.fillMaxSize().alpha(0.42f),

                contentScale = ContentScale.Crop,

            )

            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.58f)))

        }

        Box(

            modifier = Modifier

                .fillMaxSize()

                .background(

                    Brush.radialGradient(

                        colors = listOf(

                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),

                            MaterialTheme.colorScheme.background,

                        ),

                        center = Offset(0.3f, 0.2f),

                        radius = 1200f,

                    ),

                ),

        )

        Box(

            modifier = Modifier

                .align(Alignment.TopEnd)

                .offset(x = 40.dp, y = (-30).dp)

                .size(340.dp * pulse)

                .blur(110.dp)

                .background(extras.glowColor),

        )

        Box(

            modifier = Modifier

                .align(Alignment.CenterStart)

                .offset(x = (-80).dp)

                .size(280.dp)

                .blur(90.dp)

                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)),

        )

        Box(

            modifier = Modifier

                .align(Alignment.BottomEnd)

                .offset(x = 60.dp, y = 80.dp)

                .size(300.dp * pulse)

                .blur(100.dp)

                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)),

        )

        content()

    }

}



@Composable

fun GlassSurface(

    modifier: Modifier = Modifier,

    elevated: Boolean = false,

    content: @Composable () -> Unit,

) {

    val extras = LocalStreamPanelExtras.current

    val shape = RoundedCornerShape(28.dp)

    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = if (elevated) 0.35f else 0.18f)



    Surface(

        modifier = modifier.drawBehind {

            if (elevated) {

                drawCircle(

                    color = extras.glowColor.copy(alpha = 0.18f),

                    radius = size.maxDimension * 0.55f,

                    center = Offset(size.width * 0.85f, size.height * 0.1f),

                )

            }

        },

        shape = shape,

        color = MaterialTheme.colorScheme.surface.copy(alpha = extras.glassAlpha),

        tonalElevation = if (elevated) 12.dp else 6.dp,

        shadowElevation = if (elevated) 16.dp else 8.dp,

        border = BorderStroke(1.dp, borderColor),

        content = content,

    )

}



@Composable

fun GradientBrandText(

    text: String,

    style: androidx.compose.ui.text.TextStyle,

    modifier: Modifier = Modifier,

) {

    val extras = LocalStreamPanelExtras.current

    Text(

        text = text,

        style = style.copy(brush = extras.accentGradient),

        fontWeight = FontWeight.Bold,

        modifier = modifier,

    )

}



@Composable

fun SectionHeader(

    title: String,

    subtitle: String? = null,

    modifier: Modifier = Modifier,

) {

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {

        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        subtitle?.let {

            Text(

                it,

                style = MaterialTheme.typography.bodySmall,

                color = MaterialTheme.colorScheme.onSurfaceVariant,

            )

        }

    }

}



@OptIn(ExperimentalFoundationApi::class)

@Composable

fun ControlButtonCard(

    button: DashboardButton,

    modifier: Modifier = Modifier,

    executing: Boolean = false,

    onClick: () -> Unit,

    onLongClick: () -> Unit,

) {

    val start = parseHexColor(button.backgroundColor, MaterialTheme.colorScheme.surfaceVariant)

    val end = parseHexColor(button.gradientEndColor, start)

    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(

        targetValue = when {

            executing -> 0.94f

            pressed -> 0.97f

            else -> 1f

        },

        label = "button-scale",

    )

    val overlayColor by animateColorAsState(

        targetValue = when (button.state) {

            ButtonState.Active -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)

            ButtonState.Warning -> Color(0xFFFFD166).copy(alpha = 0.18f)

            ButtonState.Disabled -> Color.Black.copy(alpha = 0.28f)

            ButtonState.Idle -> Color.Transparent

        },

        label = "button-state",

    )

    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(

        initialValue = 0.3f,

        targetValue = 0.7f,

        animationSpec = infiniteRepeatable(

            animation = tween(900),

            repeatMode = RepeatMode.Reverse,

        ),

        label = "shimmer-alpha",

    )



    Card(

        modifier = modifier

            .scale(scale)

            .clip(RoundedCornerShape(26.dp))

            .combinedClickable(

                interactionSource = interactionSource,

                indication = null,

                enabled = button.state != ButtonState.Disabled,

                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                },
                onLongClick = onLongClick,

            ),

        colors = CardDefaults.cardColors(containerColor = Color.Transparent),

        elevation = CardDefaults.cardElevation(defaultElevation = if (pressed) 2.dp else 10.dp),

        border = BorderStroke(

            1.dp,

            Brush.linearGradient(

                listOf(

                    Color.White.copy(alpha = 0.35f),

                    Color.White.copy(alpha = 0.08f),

                ),

            ),

        ),

    ) {

        Box(

            modifier = Modifier

                .fillMaxSize()

                .background(Brush.linearGradient(listOf(start, end)))

                .background(

                    Brush.radialGradient(

                        colors = listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),

                        radius = 480f,

                    ),

                )

                .background(overlayColor)

                .then(

                    if (executing) {

                        Modifier.background(Color.White.copy(alpha = shimmer * 0.15f))

                    } else {

                        Modifier

                    },

                )

                .padding(16.dp),

        ) {

            Column(

                modifier = Modifier.fillMaxSize(),

                verticalArrangement = Arrangement.SpaceBetween,

            ) {

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically,

                ) {

                    Box(

                        modifier = Modifier

                            .size(42.dp)

                            .clip(RoundedCornerShape(14.dp))

                            .background(Color.White.copy(alpha = 0.16f)),

                        contentAlignment = Alignment.Center,

                    ) {

                        Icon(

                            imageVector = iconForName(button.iconName),

                            contentDescription = null,

                            tint = Color.White,

                            modifier = Modifier.size(24.dp),

                        )

                    }

                    if (button.isFolder) {

                        ButtonBadge("Folder")

                    }

                }

                Spacer(Modifier.height(10.dp))

                Column {

                    Text(

                        text = button.title,

                        color = Color.White,

                        style = MaterialTheme.typography.titleMedium,

                        fontWeight = FontWeight.SemiBold,

                        maxLines = 2,

                        overflow = TextOverflow.Ellipsis,

                    )

                    button.subtitle?.let {

                        Text(

                            text = it,

                            color = Color.White.copy(alpha = 0.78f),

                            style = MaterialTheme.typography.bodySmall,

                            maxLines = 1,

                            overflow = TextOverflow.Ellipsis,

                        )

                    }

                }

            }

            if (executing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

        }

    }

}

@Composable
fun ButtonBadge(text: String) {

    Text(

        text = text,

        color = Color.White.copy(alpha = 0.95f),

        style = MaterialTheme.typography.labelSmall,

        modifier = Modifier

            .clip(RoundedCornerShape(999.dp))

            .background(Color.White.copy(alpha = 0.18f))

            .padding(horizontal = 9.dp, vertical = 5.dp),

    )

}



fun iconForName(name: String?): ImageVector =

    when (name?.lowercase()) {

        "public" -> Icons.Default.Public

        "mic_off" -> Icons.Default.MicOff

        "chat" -> Icons.Default.Chat

        "folder" -> Icons.Default.Folder

        "terminal" -> Icons.Default.Terminal

        "bolt" -> Icons.Default.Bolt

        "settings" -> Icons.Default.Settings

        "live_tv" -> Icons.Default.LiveTv

        "music_note" -> Icons.Default.MusicNote

        "sports_esports" -> Icons.Default.SportsEsports

        else -> Icons.Default.AutoAwesome

    }


