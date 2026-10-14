package com.streampanel.feature.dashboard

/**
 * The deck's confirmations ("Copy sent", "Layer deleted"...) are written in English where the actions happen.
 * This turns the known ones into Russian for the screen; anything it does not know is shown as it is
 * (for example a text coming from the PC).
 */
internal object MessageLocalizer {
    private val exact = mapOf(
        "Copy sent" to "Копирование отправлено",
        "Paste sent" to "Вставка отправлена",
        "Refresh sent" to "Обновление отправлено",
        "Undo sent" to "Отмена отправлена",
        "Cut sent" to "Вырезание отправлено",
        "Redo sent" to "Повтор отправлен",
        "Save sent" to "Сохранение отправлено",
        "Search sent" to "Поиск отправлен",
        "New tab sent" to "Новая вкладка отправлена",
        "Alt+Tab" to "Alt+Tab",
        "Window closed" to "Окно закрыто",
        "Fullscreen toggled" to "Полный экран переключён",
        "Screenshot tool opened" to "Скриншот открыт",
        "Task Manager opened" to "Диспетчер задач открыт",
        "Snapped left" to "Окно слева",
        "Snapped right" to "Окно справа",
        "Desktop shown" to "Рабочий стол показан",
        "Active process terminated" to "Активная программа закрыта",
        "Temp folders cleaned" to "Временные файлы удалены",
        "Discord mute" to "Discord: микрофон",
        "Discord deafen" to "Discord: звук",
        "Discord PTT" to "Discord: рация",
        "Run tests" to "Запуск тестов",
        "Format document" to "Форматирование",
        "Terminal toggled" to "Терминал переключён",
        "Microphone toggled" to "Микрофон переключён",
        "Speakers" to "Колонки",
        "Headphones" to "Наушники",
        "PC is going to sleep" to "ПК засыпает",
        "PC locked" to "ПК заблокирован",
        "Focus mode enabled" to "Режим фокуса включён",
        "Focus mode disabled" to "Режим фокуса выключен",
        "Pomodoro started" to "Таймер запущен",
        "Pomodoro break" to "Перерыв",
        "Pomodoro complete - take a break!" to "Время вышло — сделайте перерыв!",
        "Pomodoro complete — take a break!" to "Время вышло — сделайте перерыв!",
        "Layer deleted" to "Слой удалён",
        "Macro deleted" to "Кнопка удалена",
        "Disconnected" to "Отключено",
        "PC server is not connected" to "ПК не подключён",
        "Setting up Counter-Strike…" to "Настраиваю Counter-Strike…",
        "Counter-Strike integration installed — restart the game once" to "Интеграция установлена — перезапустите игру один раз",
        "Counter-Strike 2 was not found on the PC" to "Counter-Strike 2 не найден на ПК",
        "Wrong PIN — check Settings → My PC" to "Неверный PIN — проверьте «Настройки → Мой ПК»",
        "git status" to "git status",
        "git pull" to "git pull",
        "docker ps" to "docker ps",
        "docker compose up" to "docker compose up",
        "Wi-Fi restart" to "Перезапуск Wi-Fi",
        "DNS cache flush" to "Очистка DNS",
        "Recycle bin" to "Корзина",
    )

    private val patterns: List<Pair<Regex, (MatchResult) -> String>> = listOf(
        Regex("""Volume (\d+)%""") to { m -> "Громкость ${m.groupValues[1]}%" },
        Regex("""Media: (.+)""") to { m -> "Медиа: ${mediaWord(m.groupValues[1])}" },
        Regex("""Layer "(.*)" created""") to { m -> "Слой «${m.groupValues[1]}» создан" },
        Regex("""Layer renamed: (.*)""") to { m -> "Слой переименован: ${m.groupValues[1]}" },
        Regex("""Layer grid: (.+)""") to { m -> "Сетка слоя: ${m.groupValues[1]}" },
        Regex("""Button "(.*)" created.*""") to { m -> "Кнопка «${m.groupValues[1]}» создана — нажмите «Изменить», чтобы настроить" },
        Regex("""Done: (.*)""") to { m -> "Готово: ${m.groupValues[1]}" },
        Regex("""Launched (.*)""") to { m -> "Запущено: ${m.groupValues[1]}" },
        Regex("""Opened (.*)""") to { m -> "Открыто: ${m.groupValues[1]}" },
        Regex("""Moved to monitor (\d+)""") to { m -> "Окно перенесено на монитор ${m.groupValues[1]}" },
        Regex("""Process (\d+) killed""") to { m -> "Процесс ${m.groupValues[1]} завершён" },
        Regex("""(.*): (\d+)m logged""") to { m -> "${m.groupValues[1]}: записано ${m.groupValues[2]} мин" },
        Regex("""Could not set up Counter-Strike: (.*)""") to { m -> "Не получилось настроить Counter-Strike: ${m.groupValues[1]}" },
    )

    private fun mediaWord(action: String) = when (action) {
        "play_pause" -> "пуск / пауза"
        "next" -> "дальше"
        "previous" -> "назад"
        "mute" -> "звук"
        else -> action
    }

    fun localize(message: String, russian: Boolean): String {
        if (!russian) return message
        exact[message]?.let { return it }
        for ((pattern, build) in patterns) {
            pattern.matchEntire(message)?.let { return build(it) }
        }
        return message
    }
}