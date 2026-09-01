package com.rank.tempbox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class TempBoxColors(
    val outerBg: Color,
    val phoneBg: Color,
    val card: Color,
    val heroBgStart: Color,
    val heroBgEnd: Color,
    val text: Color,
    val sub: Color,
    val dim: Color,
    val rowBorder: Color,
    val tipBg: Color,
    val tipBorder: Color,
    val navBg: Color,
    val navBorder: Color,
    val cardBorder: Color,
    val domainChip: Color,
    val domainChipBorder: Color,
    val newBtnBg: Color,
    val newBtnBorder: Color,
    val homeIndicator: Color,
    val statusText: Color,
    val accent: Color,
    val accentDim: Color,
    val muted: Color,
    val inboxPreviewBg: Color,
    val inboxPreviewBorder: Color,
    val inboxRowBorder: Color,
    val surfaceVariant: Color,
)

private val DarkColors = TempBoxColors(
    outerBg = DarkOuterBg,
    phoneBg = DarkPhoneBg,
    card = DarkCardBg,
    heroBgStart = DarkHeroBgStart,
    heroBgEnd = DarkHeroBgEnd,
    text = DarkText,
    sub = DarkSub,
    dim = DarkDim,
    rowBorder = DarkRowBorder,
    tipBg = DarkTipBg,
    tipBorder = DarkTipBorder,
    navBg = DarkNavBg,
    navBorder = DarkNavBorder,
    cardBorder = DarkCardBorder,
    domainChip = DarkDomainChip,
    domainChipBorder = DarkDomainChipBorder,
    newBtnBg = DarkNewBtnBg,
    newBtnBorder = DarkNewBtnBorder,
    homeIndicator = DarkHomeIndicator,
    statusText = DarkStatusText,
    accent = AccentTeal,
    accentDim = Color(0x2600d4aa),
    muted = DarkMuted,
    inboxPreviewBg = DarkCardBg,
    inboxPreviewBorder = DarkCardBorder,
    inboxRowBorder = DarkRowBorder,
    surfaceVariant = DarkSurfaceVariant,
)

private val LightColors = TempBoxColors(
    outerBg = LightOuterBg,
    phoneBg = LightPhoneBg,
    card = LightCardBg,
    heroBgStart = LightHeroBgStart,
    heroBgEnd = LightHeroBgEnd,
    text = LightText,
    sub = LightSub,
    dim = LightDim,
    rowBorder = LightRowBorder,
    tipBg = LightTipBg,
    tipBorder = LightTipBorder,
    navBg = LightNavBg,
    navBorder = LightNavBorder,
    cardBorder = LightCardBorder,
    domainChip = LightDomainChip,
    domainChipBorder = LightDomainChipBorder,
    newBtnBg = LightNewBtnBg,
    newBtnBorder = LightNewBtnBorder,
    homeIndicator = LightHomeIndicator,
    statusText = LightStatusText,
    accent = LightAccentTeal,
    accentDim = Color(0x260D9488),
    muted = LightMuted,
    inboxPreviewBg = LightCardBg,
    inboxPreviewBorder = LightCardBorder,
    inboxRowBorder = LightRowBorder,
    surfaceVariant = LightSurfaceVariant,
)

val LocalTempBoxColors = compositionLocalOf { DarkColors }

@Composable
fun TempBoxTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (isDark) DarkColors else LightColors
    val scheme = if (isDark) darkColorScheme(
        primary = AccentTeal,
        secondary = AccentTeal,
        surface = DarkPhoneBg,
        surfaceVariant = DarkSurfaceVariant,
        background = DarkOuterBg,
        onPrimary = Color(0xFF080808),
        onSurface = DarkText,
        onBackground = DarkText,
        onSurfaceVariant = DarkSub,
        error = ExpiryRed,
    ) else lightColorScheme(
        primary = LightAccentTeal,
        secondary = LightAccentTeal,
        surface = LightPhoneBg,
        surfaceVariant = LightSurfaceVariant,
        background = LightOuterBg,
        onPrimary = Color.White,
        onSurface = LightText,
        onBackground = LightText,
        onSurfaceVariant = LightSub,
        error = ExpiryRed,
    )

    androidx.compose.material3.MaterialTheme(
        colorScheme = scheme,
        typography = TempBoxTypography,
        content = {
            androidx.compose.runtime.CompositionLocalProvider(LocalTempBoxColors provides colors) {
                content()
            }
        },
    )
}

object TempBoxTheme {
    val colors: TempBoxColors
        @Composable get() = LocalTempBoxColors.current
}
