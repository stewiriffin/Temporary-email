package com.rank.tempbox.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.rank.tempbox.ui.theme.TempBoxTheme
import com.startapp.sdk.ads.banner.Banner

/** Full-width banner strip styled to match the app's bottom nav / theme. */
@Composable
fun StartIoBanner(modifier: Modifier = Modifier) {
    val colors = TempBoxTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.navBg)
            .border(0.5.dp, colors.rowBorder),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            factory = { context -> Banner(context) },
        )
    }
}
