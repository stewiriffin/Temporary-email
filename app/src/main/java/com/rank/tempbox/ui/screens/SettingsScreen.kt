package com.rank.tempbox.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rank.tempbox.MainViewModel
import com.rank.tempbox.PrefKeys
import com.rank.tempbox.R
import com.rank.tempbox.ads.AppWallManager
import com.rank.tempbox.ads.RewardedAdManager
import com.rank.tempbox.ads.StartIoBanner
import com.rank.tempbox.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    isDark: Boolean,
    themeMode: String,
    onThemeModeChanged: (String) -> Unit,
    onBack: () -> Unit,
    onFullReset: () -> Unit,
    onResetOnboarding: () -> Unit,
) {
    val colors = TempBoxTheme.colors
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE) }
    val scrollState = rememberScrollState()

    val rewardedAds = remember { (context as? Activity)?.let { RewardedAdManager(it) } }
    val appWall = remember { (context as? Activity)?.let { AppWallManager(it) } }

    val emailAddress by viewModel.emailAddress.observeAsState("")
    var refreshInterval by remember { mutableIntStateOf(prefs.getInt("auto_refresh_interval", 15)) }
    var autoRefresh by remember { mutableStateOf(prefs.getBoolean("auto_refresh_enabled", true)) }
    val expiryInfo by viewModel.expiryInfo.observeAsState()

    LaunchedEffect(Unit) {
        viewModel.updateExpiryInfo()
        rewardedAds?.load()
    }

    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) { "1.0" }

    val versionLabel = stringResource(
        R.string.version_label,
        stringResource(R.string.app_name),
        versionName ?: "1.0",
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.phoneBg)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
    ) {
        SectionHeader("Appearance", colors.sub)
        SettingsCard(colors) {
            SettingsToggleRow(
                icon = if (isDark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                label = "Dark mode",
                sublabel = if (isDark) "Using dark theme" else "Using light theme",
                checked = isDark,
                colors = colors,
                onCheckedChange = { onThemeModeChanged(if (it) "dark" else "light") },
            )
            SettingsDivider(colors)
            ThemeModeRow(themeMode, colors, onThemeModeChanged)
        }

        SectionHeader("Your address", colors.sub)
        SettingsCard(colors) {
            Column(Modifier.padding(16.dp)) {
                Text(emailAddress.ifEmpty { "Generating…" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(8.dp))
                val expiry = expiryInfo
                Text(
                    when {
                        expiry?.isExpired == true -> "Address expired"
                        expiry != null && expiry.daysRemaining > 0 -> "Expires in ${expiry.daysRemaining}d"
                        expiry != null && expiry.hoursRemaining > 0 -> "Expires in ${expiry.hoursRemaining}h"
                        else -> "Session-scoped address"
                    },
                    fontSize = 12.sp,
                    color = colors.sub,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap to copy",
                    fontSize = 11.sp,
                    color = colors.accent,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (emailAddress.isNotEmpty()) {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clip.setPrimaryClip(ClipData.newPlainText("email", emailAddress))
                            Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
        }

        SectionHeader("Inbox", colors.sub)
        SettingsCard(colors) {
            SettingsToggleRow(
                icon = Icons.Rounded.Refresh,
                label = "Auto-refresh",
                sublabel = "Check for new mail every ${refreshInterval}s",
                checked = autoRefresh,
                colors = colors,
                onCheckedChange = { checked ->
                    autoRefresh = checked
                    prefs.edit().putBoolean("auto_refresh_enabled", checked).apply()
                    viewModel.restartAutoRefresh()
                },
            )
            if (autoRefresh) {
                SettingsDivider(colors)
                IntervalPicker(refreshInterval, colors) { interval ->
                    refreshInterval = interval
                    prefs.edit().putInt("auto_refresh_interval", interval).apply()
                    viewModel.restartAutoRefresh()
                }
            }
        }

        SectionHeader("Data", colors.sub)
        SettingsCard(colors) {
            SettingsActionRow(
                icon = Icons.Rounded.DeleteSweep,
                label = "Clear cache",
                sublabel = "Free up local storage",
                colors = colors,
                onClick = {
                    val deleted = context.cacheDir?.deleteRecursively() ?: false
                    Toast.makeText(
                        context,
                        if (deleted) "Cache cleared" else "Failed to clear cache",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
            )
            SettingsDivider(colors)
            SettingsActionRow(
                icon = Icons.Rounded.RotateRight,
                label = "Reset onboarding",
                sublabel = "View the intro slides again",
                colors = colors,
                onClick = onResetOnboarding,
            )
            SettingsDivider(colors)
            SettingsActionRow(
                icon = Icons.Rounded.Warning,
                label = "Reset all data",
                sublabel = "Delete address and messages",
                colors = colors,
                onClick = onFullReset,
                danger = true,
            )
        }

        SectionHeader("Support", colors.sub)
        SettingsCard(colors) {
            SettingsActionRow(
                icon = Icons.Rounded.Videocam,
                label = "Watch ad for extra address",
                sublabel = "Get one more new address today",
                colors = colors,
                onClick = {
                    val manager = rewardedAds
                    if (manager == null) {
                        Toast.makeText(context, "Not available", Toast.LENGTH_SHORT).show()
                        return@SettingsActionRow
                    }
                    val shown = manager.show(
                        onRewarded = {
                            viewModel.grantExtraGeneration()
                            Toast.makeText(context, "Extra address unlocked!", Toast.LENGTH_SHORT).show()
                        },
                    )
                    if (!shown) {
                        Toast.makeText(context, "Video not ready, try again in a moment", Toast.LENGTH_SHORT).show()
                    }
                },
            )
            SettingsDivider(colors)
            SettingsActionRow(
                icon = Icons.Rounded.GridView,
                label = "Browse offers",
                sublabel = "Explore the app wall",
                colors = colors,
                onClick = {
                    appWall?.show()
                        ?: Toast.makeText(context, "Not available", Toast.LENGTH_SHORT).show()
                },
            )
        }

        SectionHeader("About", colors.sub)
        SettingsCard(colors) {
            SettingsInfoRow(Icons.Rounded.Info, "Version", versionLabel, colors)
            SettingsDivider(colors)
            SettingsInfoRow(Icons.Rounded.Shield, "Privacy", "No personal data stored", colors)
        }

        Spacer(Modifier.height(12.dp))
        StartIoBanner()
    }
}

@Composable
private fun SectionHeader(text: String, sub: Color) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        color = sub,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
private fun SettingsCard(colors: TempBoxColors, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(0.5.dp, colors.cardBorder, RoundedCornerShape(16.dp)),
        content = content,
    )
}

@Composable
private fun SettingsDivider(colors: TempBoxColors) {
    HorizontalDivider(color = colors.rowBorder, thickness = 0.5.dp)
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    sublabel: String,
    checked: Boolean,
    colors: TempBoxColors,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBox(icon, colors)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.text)
            Text(sublabel, fontSize = 12.sp, color = colors.sub)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.accent,
                checkedTrackColor = colors.accent.copy(alpha = 0.3f),
            ),
        )
    }
}

@Composable
private fun ThemeModeRow(themeMode: String, colors: TempBoxColors, onThemeModeChanged: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Theme mode", fontSize = 12.sp, color = colors.sub)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("light" to "Light", "dark" to "Dark", "system" to "System").forEach { (key, label) ->
                val selected = themeMode == key
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) colors.accent else colors.surfaceVariant)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onThemeModeChanged(key) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) accentButtonLabelColor() else colors.sub,
                    )
                }
            }
        }
    }
}

@Composable
private fun IntervalPicker(selected: Int, colors: TempBoxColors, onSelect: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Refresh interval", fontSize = 12.sp, color = colors.sub)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10, 15, 30, 60).forEach { interval ->
                val isSelected = selected == interval
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.accent else colors.surfaceVariant)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(interval) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${interval}s",
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) accentButtonLabelColor() else colors.sub,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    label: String,
    sublabel: String,
    colors: TempBoxColors,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBox(icon, colors, danger)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (danger) ExpiryRed else colors.text,
            )
            Text(sublabel, fontSize = 12.sp, color = colors.sub)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = colors.dim, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsInfoRow(icon: ImageVector, label: String, sublabel: String, colors: TempBoxColors) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBox(icon, colors)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.text)
            Text(sublabel, fontSize = 12.sp, color = colors.sub)
        }
    }
}

@Composable
private fun SettingsIconBox(icon: ImageVector, colors: TempBoxColors, danger: Boolean = false) {
    val tint = if (danger) ExpiryRed else colors.sub
    Box(
        Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (danger) ExpiryRed.copy(alpha = 0.12f) else colors.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}
