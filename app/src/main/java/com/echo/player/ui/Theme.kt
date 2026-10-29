package com.echo.player.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF121212)
val Surface1 = Color(0xFF181818)
val Surface2 = Color(0xFF282828)
val Accent = Color(0xFF2BD66B)
val TextDim = Color(0xFFB3B3B3)

@Composable
fun EchoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Color.Black,
            background = Bg,
            surface = Bg,
            onSurface = Color.White,
            onBackground = Color.White,
            surfaceVariant = Surface2,
            onSurfaceVariant = TextDim,
        ),
        content = content,
    )
}
