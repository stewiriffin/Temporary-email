package com.rank.tempbox.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.rank.tempbox.R
import com.rank.tempbox.ui.theme.TempBoxTheme
import com.rank.tempbox.ui.theme.accentButtonLabelColor

enum class AppLogoSize { Small, Medium, Large }

@Composable
fun AppLogo(
    size: AppLogoSize = AppLogoSize.Medium,
    showWordmark: Boolean = true,
) {
    val colors = TempBoxTheme.colors
    val appName = stringResource(R.string.app_name)
    val glowAlpha by rememberInfiniteTransition(label = "logoGlow").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOut), RepeatMode.Reverse),
        label = "glow",
    )
    val (boxSize, iconSize) = when (size) {
        AppLogoSize.Small -> 24.dp to 12.dp
        AppLogoSize.Medium -> 28.dp to 14.dp
        AppLogoSize.Large -> 48.dp to 24.dp
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(boxSize)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.accent.copy(alpha = glowAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = null,
                tint = accentButtonLabelColor(),
                modifier = Modifier.size(iconSize),
            )
        }
        if (showWordmark) {
            Spacer(Modifier.width(if (size == AppLogoSize.Large) 14.dp else 8.dp))
            Column {
                Text(
                    appName,
                    fontSize = when (size) {
                        AppLogoSize.Small -> 11.sp
                        AppLogoSize.Medium -> 12.sp
                        AppLogoSize.Large -> 18.sp
                    },
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                )
                if (size == AppLogoSize.Large) {
                    Text(
                        "Disposable inbox",
                        fontSize = 11.sp,
                        color = colors.sub,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }
    }
}
