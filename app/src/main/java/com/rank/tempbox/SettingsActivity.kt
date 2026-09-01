package com.rank.tempbox

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import com.rank.tempbox.ui.screens.SettingsScreen
import com.rank.tempbox.ui.theme.TempBoxTheme

class SettingsActivity : ComponentActivity() {

    private val viewModel: MainViewModel by lazy {
        (application as TempBoxApplication).mainViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            val prefs = remember { getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE) }
            var themeMode by remember {
                mutableStateOf(
                    if (prefs.contains("theme_mode")) {
                        prefs.getString("theme_mode", "system")!!
                    } else if (prefs.contains("dark_mode")) {
                        if (prefs.getBoolean("dark_mode", true)) "dark" else "light"
                    } else {
                        "system"
                    }
                )
            }
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }

            TempBoxTheme(isDark = isDark) {
                SettingsScreen(
                    viewModel = viewModel,
                    isDark = isDark,
                    themeMode = themeMode,
                    onThemeModeChanged = { newMode ->
                        themeMode = newMode
                        prefs.edit().putString("theme_mode", newMode).apply()
                    },
                    onBack = { finish() },
                    onFullReset = {
                        getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
                        startActivity(Intent(this, OnboardingActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    },
                    onResetOnboarding = {
                        prefs.edit().putBoolean("onboarding_complete", false).apply()
                        startActivity(Intent(this, OnboardingActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    },
                )
            }
        }
    }
}
