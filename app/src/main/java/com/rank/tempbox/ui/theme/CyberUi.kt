package com.rank.tempbox.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Text on filled accent buttons in the reference UI. */
@Composable
fun accentButtonLabelColor(): Color {
    val accent = TempBoxTheme.colors.accent
    return if (accent == LightPrimary) Color.White else CyberBlack
}

fun Modifier.cyberCardBorder(accentAlpha: Float = 0.15f): Modifier =
    this.border(1.dp, NeonGreen.copy(alpha = accentAlpha), RoundedCornerShape(2.dp))
