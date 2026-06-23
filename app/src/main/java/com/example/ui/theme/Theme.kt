package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BentoPrimary = Color(0xFF6750A4)
val BentoSecondary = Color(0xFFECE6F0)
val BentoBackground = Color(0xFFFEF7FF)
val BentoSurface = Color(0xFFF3EDF7)
val BentoText = Color(0xFF1D1B20)
val BentoAccent = Color(0xFFFFD8E4)
val BentoAccentText = Color(0xFF31111D)
val BentoBorder = Color(0xFFCAC4D0)

private val BentoColorScheme = lightColorScheme(
    primary = BentoPrimary,
    onPrimary = Color.White,
    secondary = BentoSecondary,
    onSecondary = BentoText,
    background = BentoBackground,
    onBackground = BentoText,
    surface = BentoSurface,
    onSurface = BentoText,
    outline = BentoBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Always light theme for the elegant Bento pastel look
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BentoColorScheme,
        typography = Typography,
        content = content
    )
}
