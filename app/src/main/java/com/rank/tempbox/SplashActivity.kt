package com.rank.tempbox

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.rank.tempbox.ui.components.AppLogo
import com.rank.tempbox.ui.components.AppLogoSize
import com.rank.tempbox.ui.theme.TempBoxTheme
import com.startapp.sdk.adsbase.StartAppAd
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        super.onCreate(savedInstanceState)

        setContent {
            val prefs = remember { getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE) }
            val themeMode = remember { prefs.getString("theme_mode", "dark")!! }
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }

            TempBoxTheme(isDark = isDark) {
                val colors = TempBoxTheme.colors

                // Animation for logo pulse
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "logo_scale"
                )

                var statusText by remember { mutableStateOf("Initializing secured environment...") }

                LaunchedEffect(Unit) {
                    delay(800)
                    statusText = "Securing temporary inbox..."
                    delay(800)
                    statusText = "Loading preferences..."
                    delay(400)

                    val completed = prefs.getBoolean("onboarding_complete", false)
                    val intent = if (completed) {
                        Intent(this@SplashActivity, MainActivity::class.java)
                    } else {
                        Intent(this@SplashActivity, OnboardingActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.phoneBg),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .padding(24.dp)
                        ) {
                            AppLogo(size = AppLogoSize.Medium, showWordmark = true)
                        }

                        Spacer(Modifier.height(48.dp))

                        CircularProgressIndicator(
                            color = colors.accent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp)
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = statusText,
                            color = colors.sub,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        StartAppAd.showSplash(this, savedInstanceState)
    }
}
