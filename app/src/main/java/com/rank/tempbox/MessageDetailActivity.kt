package com.rank.tempbox

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import com.rank.tempbox.ads.InterstitialAdManager
import com.rank.tempbox.ui.screens.MessageDetailScreen
import com.rank.tempbox.ui.theme.TempBoxTheme

class MessageDetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MESSAGE_ID = "extra_message_id"
    }

    private val viewModel: MainViewModel by lazy {
        (application as TempBoxApplication).mainViewModel
    }
    private val prefs by lazy { getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE) }
    private val interstitialAds by lazy { InterstitialAdManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID)
            ?: intent.getStringExtra("MESSAGE_ID")
        if (messageId.isNullOrBlank()) {
            Toast.makeText(this, "Invalid message.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val slot = intent.getIntExtra("slot_number", -1)
        if (slot !in 1..3) {
            Toast.makeText(this, "Invalid inbox slot.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val token = prefs.getString("token_$slot", null)
            ?: prefs.getString("auth_token", null)
            ?: run {
                Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

        setContent {
            val themeMode = remember {
                if (prefs.contains("theme_mode")) {
                    prefs.getString("theme_mode", "dark")!!
                } else if (prefs.contains("dark_mode")) {
                    if (prefs.getBoolean("dark_mode", true)) "dark" else "light"
                } else {
                    "dark"
                }
            }
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }

            TempBoxTheme(isDark = isDark) {
                MessageDetailScreen(
                    messageId = messageId,
                    token = token,
                    isDark = isDark,
                    onBack = { finish() },
                    onDelete = {
                        viewModel.deleteMessage(messageId)
                        finish()
                    },
                )
            }
        }

        interstitialAds.load()
        Handler(Looper.getMainLooper()).postDelayed({
            interstitialAds.showIfReady()
        }, 1200)
    }
}
