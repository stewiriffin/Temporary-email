package com.rank.tempbox.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rank.tempbox.ui.theme.TempBoxTheme
import com.rank.tempbox.ui.theme.accentButtonLabelColor

@Composable
fun TmpMailHeader(
    pageTitle: String,
    showBack: Boolean = false,
    isSettingsActive: Boolean = false,
    onBack: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val colors = TempBoxTheme.colors
    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOut), RepeatMode.Reverse),
        label = "glowAlpha",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(colors.phoneBg)
            .border(width = 0.5.dp, color = colors.cardBorder)
            .padding(horizontal = 16.dp)
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            if (showBack) {
                Box(
                    Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBack,
                        )
                        .padding(end = 8.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.sub,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.accent.copy(alpha = glowAlpha)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Email,
                        contentDescription = null,
                        tint = accentButtonLabelColor(),
                        modifier = Modifier.size(12.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
            }

            Text(
                pageTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text,
                letterSpacing = 1.5.sp,
                maxLines = 1,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!isSettingsActive) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.accent.copy(alpha = 0.1f))
                        .border(0.5.dp, colors.accent.copy(alpha = 0.22f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("BETA", fontSize = 9.sp, color = colors.accent, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSettingsActive) colors.accent.copy(alpha = 0.14f)
                        else colors.surfaceVariant,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSettings,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = if (isSettingsActive) colors.accent else colors.sub,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
