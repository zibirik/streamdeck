package com.streampanel.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthSizeClass {
    Compact,
    Medium,
    Expanded,
}

fun Dp.toWidthSizeClass(): WindowWidthSizeClass = when {
    this < 600.dp -> WindowWidthSizeClass.Compact
    this < 900.dp -> WindowWidthSizeClass.Medium
    else -> WindowWidthSizeClass.Expanded
}

fun gridColumnsForWidth(sizeClass: WindowWidthSizeClass, configuredColumns: Int): Int = when (sizeClass) {
    WindowWidthSizeClass.Compact -> minOf(configuredColumns, 3)
    WindowWidthSizeClass.Medium -> minOf(configuredColumns, 4)
    WindowWidthSizeClass.Expanded -> minOf(configuredColumns, 5)
}

fun buttonHeightForWidth(sizeClass: WindowWidthSizeClass): Dp = when (sizeClass) {
    WindowWidthSizeClass.Compact -> 96.dp
    WindowWidthSizeClass.Medium -> 112.dp
    WindowWidthSizeClass.Expanded -> 136.dp
}
