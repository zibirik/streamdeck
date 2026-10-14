package com.streampanel.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Work
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.streampanel.core.model.HubSection

/** The first destination: the grid of the user's own buttons. It is not a [HubSection]. */
object DeckDestination {
    val icon: ImageVector get() = Icons.Rounded.GridView
    val color: Color get() = UiPalette.Blue

    @Composable
    fun title(): String = tr("Кнопки", "Deck")

    @Composable
    fun subtitle(): String = tr("Ваши кнопки и слои", "Your buttons and layers")
}

val HubSection.icon: ImageVector
    get() = when (this) {
        HubSection.Control -> Icons.Rounded.Bolt
        HubSection.Stream -> Icons.Rounded.LiveTv
        HubSection.Pc -> Icons.Rounded.Memory
        HubSection.Work -> Icons.Rounded.Work
    }

val HubSection.color: Color
    get() = when (this) {
        HubSection.Control -> UiPalette.Violet
        HubSection.Stream -> UiPalette.Pink
        HubSection.Pc -> UiPalette.Cyan
        HubSection.Work -> UiPalette.Amber
    }

@Composable
fun HubSection.title(): String = when (this) {
    HubSection.Control -> tr("Пульт", "Control")
    HubSection.Stream -> tr("Стрим", "Stream")
    HubSection.Pc -> tr("Мой ПК", "My PC")
    HubSection.Work -> tr("Работа", "Work")
}

@Composable
fun HubSection.subtitle(): String = when (this) {
    HubSection.Control -> tr("Звук, окна, быстрые команды", "Sound, windows, quick commands")
    HubSection.Stream -> tr("Эфир, чат, Discord и игры", "Live, chat, Discord and games")
    HubSection.Pc -> tr("Нагрузка и процессы компьютера", "Computer load and processes")
    HubSection.Work -> tr("Фокус, время и разработка", "Focus, time and development")
}
