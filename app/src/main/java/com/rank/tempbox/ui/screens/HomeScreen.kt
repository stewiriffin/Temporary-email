package com.rank.tempbox.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rank.tempbox.ui.theme.TempBoxTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    emailAddress: String,
    isDark: Boolean,
    isLoading: Boolean = false,
    refreshCountdown: Int = 0,
    refreshProgress: Float = 0f,
    unreadCount: Int = 0,
    generationsRemaining: Int = 3,
    onRefreshEmail: () -> Unit,
    onCopyEmail: (String) -> Unit,
    onNavigateToInbox: () -> Unit,
    onRefreshInbox: () -> Unit,
    onWatchAdForGeneration: () -> Unit = {},
) {
    val colors = TempBoxTheme.colors
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    var masked by remember { mutableStateOf(false) }
    val hasEmail = emailAddress.isNotBlank()
    val scrambled = useScrambleText(emailAddress, enabled = hasEmail)

    val displayEmail = when {
        isLoading && !hasEmail -> "Generating address..."
        !hasEmail -> "Waiting for connection..."
        masked && emailAddress.contains("@") -> {
            val parts = emailAddress.split("@")
            val local = parts[0]
            val maskedLocal = if (local.length > 3) local.take(3) + "•".repeat(local.length - 3) else local
            "$maskedLocal@${parts[1]}"
        }
        else -> scrambled
    }

    fun copyAddress() {
        if (!hasEmail) {
            onCopyEmail("")
            return
        }
        onCopyEmail(emailAddress)
        copied = true
        scope.launch { delay(2000); copied = false }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.phoneBg),
    ) {
        if (isDark) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(colors.accent.copy(alpha = 0.06f), Color.Transparent),
                            radius = 900f,
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 32.dp, bottom = 16.dp),
        ) {
            // Status row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiveDot(colors.accent)
                    Text("Active inbox", fontSize = 12.sp, color = colors.sub, fontWeight = FontWeight.Medium)
                }
                Text("Session-scoped", fontSize = 12.sp, color = colors.sub)
            }

            Spacer(Modifier.height(24.dp))

            // Address card
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.card)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp)),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = hasEmail,
                            onClick = ::copyAddress,
                        )
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                ) {
                    Text(
                        "Your address",
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        color = colors.sub,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            displayEmail,
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasEmail) colors.accent else colors.sub,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (hasEmail) BlinkingCursor(colors.accent)
                    }
                    if (copied) {
                        Spacer(Modifier.height(6.dp))
                        Text("Copied to clipboard", fontSize = 12.sp, color = colors.accent, fontWeight = FontWeight.Medium)
                    }
                }

                // Auto-refresh bar
                Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("AUTO-REFRESH", fontSize = 10.sp, letterSpacing = 1.sp, color = colors.sub)
                        Text(
                            if (isLoading) "checking…" else "${refreshCountdown.coerceAtLeast(0)}s",
                            fontSize = 10.sp,
                            color = colors.sub,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(colors.cardBorder),
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(refreshProgress.coerceIn(0f, 1f))
                                .background(colors.accent),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                HorizontalDivider(color = colors.rowBorder, thickness = 0.5.dp)

                Row(Modifier.fillMaxWidth()) {
                    CardAction(
                        icon = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        label = if (copied) "Copied" else "Copy",
                        primary = true,
                        enabled = hasEmail,
                        accent = colors.accent,
                        muted = colors.sub,
                        onClick = ::copyAddress,
                        modifier = Modifier.weight(1f),
                    )
                    VerticalDivider(modifier = Modifier.height(56.dp), color = colors.rowBorder, thickness = 0.5.dp)
                    CardAction(
                        icon = if (masked) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        label = if (masked) "Show" else "Hide",
                        primary = false,
                        enabled = hasEmail,
                        accent = colors.accent,
                        muted = colors.sub,
                        onClick = { if (hasEmail) masked = !masked },
                        modifier = Modifier.weight(1f),
                    )
                    VerticalDivider(modifier = Modifier.height(56.dp), color = colors.rowBorder, thickness = 0.5.dp)
                    CardAction(
                        icon = Icons.Rounded.Refresh,
                        label = "New",
                        primary = false,
                        enabled = !isLoading,
                        accent = colors.accent,
                        muted = colors.sub,
                        onClick = onRefreshEmail,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (generationsRemaining <= 0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.tipBg)
                        .border(1.dp, colors.tipBorder, RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onWatchAdForGeneration,
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Videocam, null, tint = colors.accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Free limit reached", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
                        Text("Watch a short ad to generate another address", fontSize = 12.sp, color = colors.sub)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.sub)
                }
                Spacer(Modifier.height(16.dp))
            }

            // Open inbox
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.card)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onNavigateToInbox,
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Inbox, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Open inbox", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
                    Text(
                        if (unreadCount > 0) "$unreadCount unread message${if (unreadCount == 1) "" else "s"}"
                        else "View incoming messages",
                        fontSize = 12.sp,
                        color = colors.sub,
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.sub)
            }

            Spacer(Modifier.weight(1f))

            Text(
                "No data stored. Inbox vanishes when you leave.",
                fontSize = 12.sp,
                color = colors.sub,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        if (isLoading && !hasEmail) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun LiveDot(accent: Color) {
    val alpha by rememberInfiniteTransition(label = "live").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1700, easing = EaseInOut), RepeatMode.Reverse),
        label = "liveAlpha",
    )
    Box(
        Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = alpha)),
    )
}

@Composable
private fun BlinkingCursor(accent: Color) {
    val visible by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
        label = "blink",
    )
    if (visible > 0.5f) {
        Box(Modifier.width(2.dp).height(20.dp).background(accent))
    }
}

@Composable
private fun CardAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    primary: Boolean,
    enabled: Boolean,
    accent: Color,
    muted: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            icon,
            null,
            tint = when {
                !enabled -> muted.copy(alpha = 0.4f)
                primary -> accent
                else -> muted
            },
            modifier = Modifier.size(16.dp),
        )
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                !enabled -> muted.copy(alpha = 0.4f)
                primary -> accent
                else -> muted
            },
        )
    }
}

@Composable
private fun useScrambleText(target: String, enabled: Boolean = true): String {
    var display by remember(target) { mutableStateOf(target) }
    LaunchedEffect(target, enabled) {
        if (!enabled || target.isBlank()) {
            display = target
            return@LaunchedEffect
        }
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789@.-"
        var step = 0
        while (step < target.length) {
            step++
            display = target.mapIndexed { i, ch ->
                if (i < step) ch else chars.random()
            }.joinToString("")
            delay(32)
        }
        display = target
    }
    return display
}
