package com.streampanel.feature.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageLocalizerTest {
    @Test
    fun `english stays english`() {
        assertEquals("Copy sent", MessageLocalizer.localize("Copy sent", russian = false))
    }

    @Test
    fun `known messages are translated`() {
        assertEquals("ПК заблокирован", MessageLocalizer.localize("PC locked", russian = true))
        assertEquals("Слой удалён", MessageLocalizer.localize("Layer deleted", russian = true))
    }

    @Test
    fun `messages with values keep the values`() {
        assertEquals("Громкость 62%", MessageLocalizer.localize("Volume 62%", russian = true))
        assertEquals("Слой «Игры» создан", MessageLocalizer.localize("Layer \"Игры\" created", russian = true))
        assertEquals("Запущено: Chrome", MessageLocalizer.localize("Launched Chrome", russian = true))
        assertEquals("Окно перенесено на монитор 2", MessageLocalizer.localize("Moved to monitor 2", russian = true))
        assertEquals("Учёба: записано 25 мин", MessageLocalizer.localize("Учёба: 25m logged", russian = true))
        assertEquals("Медиа: пуск / пауза", MessageLocalizer.localize("Media: play_pause", russian = true))
        assertEquals(
            "Интеграция установлена — перезапустите игру один раз",
            MessageLocalizer.localize("Counter-Strike integration installed — restart the game once", russian = true),
        )
    }

    @Test
    fun `unknown text, like an answer from the PC, is left alone`() {
        val fromPc = "On branch main\nnothing to commit"
        assertEquals(fromPc, MessageLocalizer.localize(fromPc, russian = true))
    }
}