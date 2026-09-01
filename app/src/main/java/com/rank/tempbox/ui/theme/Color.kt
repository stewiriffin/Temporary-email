package com.rank.tempbox.ui.theme

import androidx.compose.ui.graphics.Color

// New reference (theme.css) — dark: #00ff88 / #080808, light: #007a3e / #f4f4f2
val CyberBlack = Color(0xFF080808)
val NeonGreen = Color(0xFF00FF88)
val LightPrimary = Color(0xFF007A3E)
val LightBackground = Color(0xFFF4F4F2)
val LightForeground = Color(0xFF0D0D0D)
val LightCard = Color(0xFFFFFFFF)
val LightMutedForeground = Color(0xFF6B6B6B)
val DarkForeground = Color(0xFFE6E6E4)
val DarkCard = Color(0xFF111110)
val DarkMutedForeground = Color(0xFF636360)

// Dark mode palette
val DarkOuterBg = Color(0xFF050505)
val DarkPhoneBg = CyberBlack
val DarkCardBg = DarkCard
val DarkHeroBgStart = Color(0xFF0E0E0E)
val DarkHeroBgEnd = Color(0xFF0A0A0A)
val DarkText = DarkForeground
val DarkSub = DarkMutedForeground
val DarkDim = Color(0xFF3D3D3D)
val DarkRowBorder = Color(0x12FFFFFF)
val DarkTipBg = Color(0x0D00FF88)
val DarkTipBorder = Color(0x3300FF88)
val DarkNavBg = DarkCard
val DarkNavBorder = Color(0x12FFFFFF)
val DarkCardBorder = Color(0x12FFFFFF)
val DarkDomainChip = Color(0x0AFFFFFF)
val DarkDomainChipBorder = Color(0x1FFFFFFF)
val DarkNewBtnBg = Color(0x0DFFFFFF)
val DarkNewBtnBorder = Color(0x24FFFFFF)
val DarkHomeIndicator = Color(0x26FFFFFF)
val DarkStatusText = DarkForeground
val DarkSurfaceVariant = Color(0xFF1C1C1A)
val AccentTeal = NeonGreen

// Light mode palette (reference v2)
val LightOuterBg = LightBackground
val LightPhoneBg = LightBackground
val LightCardBg = LightCard
val LightHeroBgStart = LightBackground
val LightHeroBgEnd = LightCard
val LightText = LightForeground
val LightSub = LightMutedForeground
val LightDim = Color(0xFF9A9A97)
val LightRowBorder = Color(0x17000000)
val LightTipBg = Color(0x12007A3E)
val LightTipBorder = Color(0x33007A3E)
val LightNavBg = LightCard
val LightNavBorder = Color(0x17000000)
val LightCardBorder = Color(0x17000000)
val LightDomainChip = Color(0x12007A3E)
val LightDomainChipBorder = Color(0x26007A3E)
val LightNewBtnBg = Color(0xFFEBEBEA)
val LightNewBtnBorder = Color(0x26000000)
val LightHomeIndicator = Color(0x26007A3E)
val LightStatusText = LightForeground
val LightSurfaceVariant = Color(0xFFEBEBEA)
val LightAccentTeal = LightPrimary

// Shared
val ExpiryRed = Color(0xFFFF3B5C)
val ExpiryOrange = Color(0xFFF59E0B)
val Success = NeonGreen
val Error = ExpiryRed
val BadgeRed = ExpiryRed
val AvatarColors = listOf(
    Color(0xFF24292E), Color(0xFF0070F3), Color(0xFFFF7262),
    Color(0xFF6772E5), Color(0xFF5E6AD2), Color(0xFF00C7B7),
    Color(0xFF10A37F), Color(0xFF333333),
)
val DarkMuted = Color(0xFF161614)
val LightMuted = Color(0xFFEBEBEA)
