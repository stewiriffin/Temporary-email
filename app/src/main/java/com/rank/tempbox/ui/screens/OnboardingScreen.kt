package com.rank.tempbox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rank.tempbox.ui.theme.TempBoxTheme
import com.rank.tempbox.ui.theme.accentButtonLabelColor
import kotlinx.coroutines.launch

private data class OnboardingSlide(
    val icon: @Composable () -> Unit,
    val title: String,
    val body: String,
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val colors = TempBoxTheme.colors
    val scope = rememberCoroutineScope()
    val slides = listOf(
        OnboardingSlide(
            icon = { Icon(Icons.Rounded.Bolt, null, tint = colors.accent, modifier = Modifier.size(24.dp)) },
            title = "Instant disposable inbox",
            body = "A fresh address is generated the moment you open the app. No signup, no password, no account.",
        ),
        OnboardingSlide(
            icon = { Icon(Icons.Rounded.Shield, null, tint = colors.accent, modifier = Modifier.size(24.dp)) },
            title = "Zero logs, zero tracking",
            body = "Messages are never stored on a server. The inbox vanishes when you close the app.",
        ),
        OnboardingSlide(
            icon = { Icon(Icons.Rounded.Schedule, null, tint = colors.accent, modifier = Modifier.size(24.dp)) },
            title = "Auto-refreshes every 30s",
            body = "New mail arrives without lifting a finger. One-time codes are detected and surfaced instantly.",
        ),
        OnboardingSlide(
            icon = { Icon(Icons.Rounded.Lock, null, tint = colors.accent, modifier = Modifier.size(24.dp)) },
            title = "Private by design",
            body = "Use it to test signups, receive OTPs, or keep your real address off mailing lists.",
        ),
    )
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val last = pagerState.currentPage == slides.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.phoneBg)
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 32.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            slides.forEachIndexed { index, _ ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (index <= pagerState.currentPage) colors.accent else colors.cardBorder,
                        ),
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { pageIndex ->
            val slide = slides[pageIndex]
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accent.copy(alpha = 0.12f))
                        .border(1.dp, colors.accent.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    slide.icon()
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    slide.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                    lineHeight = 26.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    slide.body,
                    fontSize = 14.sp,
                    color = colors.sub,
                    lineHeight = 22.sp,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (pagerState.currentPage > 0) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Back", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.sub)
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.accent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (last) {
                            onComplete()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (last) "Get started" else "Next",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentButtonLabelColor(),
                    )
                    if (!last) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = accentButtonLabelColor(),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
