package com.rank.tempbox

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.livedata.observeAsState
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.rank.tempbox.R
import com.rank.tempbox.ads.InterstitialAdManager
import com.rank.tempbox.ads.RewardedAdManager
import com.rank.tempbox.ads.StartIoBanner
import com.rank.tempbox.ui.components.TmpMailHeader
import com.rank.tempbox.ui.screens.HomeScreen
import com.rank.tempbox.ui.screens.InboxScreen
import com.rank.tempbox.ui.screens.SettingsScreen
import com.rank.tempbox.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by lazy {
        (application as TempBoxApplication).mainViewModel
    }

    private val interstitialAds by lazy { InterstitialAdManager(this) }
    private val rewardedAds by lazy { RewardedAdManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        super.onCreate(savedInstanceState)
        interstitialAds.load()
        rewardedAds.load()

        setContent {
            val prefs = remember {
                getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
            }
            var themeMode by remember {
                mutableStateOf(
                    if (prefs.contains("theme_mode")) {
                        prefs.getString("theme_mode", "dark")!!
                    } else if (prefs.contains("dark_mode")) {
                        if (prefs.getBoolean("dark_mode", true)) "dark" else "light"
                    } else {
                        "dark"
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
                val colors = TempBoxTheme.colors
                val context = LocalContext.current

                val emailAddress by viewModel.emailAddress.observeAsState("")
                val messages by viewModel.messages.observeAsState(emptyList())
                val isLoading by viewModel.isLoading.observeAsState(false)
                val countdown by viewModel.countdown.observeAsState(0)
                val expiryInfo by viewModel.expiryInfo.observeAsState()
                val generationsRemaining by viewModel.generationsRemaining.observeAsState(MainViewModel.DAILY_GENERATION_LIMIT)

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val refreshInterval = remember(currentRoute) {
                    prefs.getInt("auto_refresh_interval", 15).coerceAtLeast(1)
                }
                val refreshProgress = if (refreshInterval > 0) {
                    ((refreshInterval - countdown).coerceAtLeast(0).toFloat() / refreshInterval).coerceIn(0f, 1f)
                } else 0f

                val mainRoutes = listOf("home", "inbox")
                val headerRoutes = listOf("home", "inbox", "settings")
                val showBottomBar = currentRoute in mainRoutes

                val snackbarHostState = remember { SnackbarHostState() }
                val appTitle = stringResource(R.string.app_title)

                val vmError by viewModel.error.observeAsState(null)
                LaunchedEffect(vmError) {
                    vmError?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearError()
                    }
                }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { /* granted or denied */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted && !prefs.getBoolean("notification_permission_asked", false)) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            prefs.edit().putBoolean("notification_permission_asked", true).apply()
                        }
                    }
                    viewModel.updateExpiryInfo()
                    while (true) {
                        delay(60_000)
                        viewModel.updateExpiryInfo()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.outerBg),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.phoneBg),
                    ) {
                        if (currentRoute in headerRoutes) {
                            val pageTitle = when (currentRoute) {
                                "inbox" -> "Inbox"
                                "settings" -> "Settings"
                                else -> appTitle
                            }
                            TmpMailHeader(
                                pageTitle = pageTitle,
                                showBack = currentRoute == "settings",
                                isSettingsActive = currentRoute == "settings",
                                onBack = { navController.popBackStack() },
                                onSettings = {
                                    if (currentRoute == "settings") {
                                        navController.popBackStack()
                                    } else {
                                        navController.navigate("settings")
                                    }
                                },
                            )
                        }

                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                enterTransition = {
                                    val fromRoute = initialState.destination.route
                                    val toRoute = targetState.destination.route
                                    if (fromRoute in listOf("home", "inbox") && toRoute in listOf("home", "inbox")) {
                                        val fromIndex = listOf("home", "inbox").indexOf(fromRoute)
                                        val toIndex = listOf("home", "inbox").indexOf(toRoute)
                                        val direction = if (toIndex > fromIndex) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right
                                        slideIntoContainer(direction, animationSpec = tween(300, easing = EaseOutCubic)) + fadeIn(animationSpec = tween(200))
                                    } else {
                                        fadeIn(animationSpec = tween(300))
                                    }
                                },
                                exitTransition = {
                                    val fromRoute = initialState.destination.route
                                    val toRoute = targetState.destination.route
                                    if (fromRoute in listOf("home", "inbox") && toRoute in listOf("home", "inbox")) {
                                        val fromIndex = listOf("home", "inbox").indexOf(fromRoute)
                                        val toIndex = listOf("home", "inbox").indexOf(toRoute)
                                        val direction = if (toIndex > fromIndex) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right
                                        slideOutOfContainer(direction, animationSpec = tween(300, easing = EaseOutCubic)) + fadeOut(animationSpec = tween(200))
                                    } else {
                                        fadeOut(animationSpec = tween(300))
                                    }
                                },
                                popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                                popExitTransition = { fadeOut(animationSpec = tween(300)) }
                            ) {
                                composable("home") {
                                    HomeScreen(
                                        emailAddress = emailAddress,
                                        isDark = isDark,
                                        isLoading = isLoading,
                                        refreshCountdown = countdown,
                                        refreshProgress = refreshProgress,
                                        unreadCount = messages.count { !it.seen },
                                        generationsRemaining = generationsRemaining,
                                        onRefreshEmail = {
                                            if (viewModel.generateNewEmail()) {
                                                interstitialAds.onUserAction()
                                            }
                                        },
                                        onCopyEmail = { addr ->
                                            if (addr.isBlank()) {
                                                Toast.makeText(context, "Email not ready yet", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val clip = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clip.setPrimaryClip(ClipData.newPlainText("email", addr))
                                                Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onNavigateToInbox = {
                                            navController.navigate("inbox") {
                                                popUpTo("home") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        onRefreshInbox = {
                                            viewModel.refreshInbox()
                                            interstitialAds.onUserAction()
                                        },
                                        onWatchAdForGeneration = { showRewardedForExtraAddress() },
                                    )
                                }

                                composable("inbox") {
                                    InboxScreen(
                                        viewModel = viewModel,
                                        isDark = isDark,
                                        onRefresh = {
                                            viewModel.refreshInbox()
                                            interstitialAds.onUserAction()
                                        },
                                    )
                                }

                                composable(
                                    route = "settings",
                                    enterTransition = {
                                        slideIntoContainer(
                                            towards = AnimatedContentTransitionScope.SlideDirection.Up,
                                            animationSpec = tween(350, easing = EaseOutCubic)
                                        ) + fadeIn(animationSpec = tween(250))
                                    },
                                    exitTransition = {
                                        slideOutOfContainer(
                                            towards = AnimatedContentTransitionScope.SlideDirection.Down,
                                            animationSpec = tween(350, easing = EaseInCubic)
                                        ) + fadeOut(animationSpec = tween(250))
                                    }
                                ) {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        isDark = isDark,
                                        themeMode = themeMode,
                                        onThemeModeChanged = { newMode ->
                                            themeMode = newMode
                                            prefs.edit().putString("theme_mode", newMode).apply()
                                        },
                                        onBack = { navController.popBackStack() },
                                        onFullReset = {
                                            prefs.edit().clear().apply()
                                            android.os.Process.killProcess(android.os.Process.myPid())
                                        },
                                        onResetOnboarding = {
                                            prefs.edit().putBoolean("onboarding_complete", false).apply()
                                            startActivity(
                                                android.content.Intent(this@MainActivity, OnboardingActivity::class.java).apply {
                                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                },
                                            )
                                            finish()
                                        },
                                    )
                                }
                            }
                        }

                        // Bottom navigation bar
                        if (showBottomBar) {
                            StartIoBanner()
                            BottomNav(
                                currentRoute = currentRoute ?: "home",
                                accent = colors.accent,
                                sub = colors.sub,
                                navBg = colors.navBg,
                                navBorder = colors.navBorder,
                                unreadCount = messages.count { !it.seen },
                                onTabSelected = { route ->
                                    navController.navigate(route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        }

                        // Home indicator
                        Box(
                            Modifier.fillMaxWidth().padding(bottom = 6.dp, top = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier.width(120.dp).height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colors.homeIndicator),
                            )
                        }
                    }

                    // Snackbar host overlay
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
                        snackbar = { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = colors.card,
                                contentColor = colors.text,
                                actionColor = colors.accent,
                                shape = RoundedCornerShape(2.dp),
                            )
                        },
                    )
                }
            }
        }
    }
    private fun showRewardedForExtraAddress() {
        val shown = rewardedAds.show(
            onRewarded = {
                viewModel.grantExtraGeneration()
                viewModel.generateNewEmail()
            },
        )
        if (!shown) {
            Toast.makeText(this, "Video not ready, try again in a moment", Toast.LENGTH_SHORT).show()
        }
    }
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val navTabs = listOf(
    NavTab("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    NavTab("inbox", "Inbox", Icons.Filled.Inbox, Icons.Outlined.Inbox),
)

@Composable
private fun BottomNav(
    currentRoute: String,
    accent: Color,
    sub: Color,
    navBg: Color,
    navBorder: Color,
    unreadCount: Int,
    onTabSelected: (String) -> Unit,
) {
    Surface(
        color = navBg,
        tonalElevation = 0.dp,
        modifier = Modifier.border(width = 0.5.dp, color = navBorder),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navTabs.forEach { tab ->
                val active = currentRoute == tab.route
                Column(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab.route) },
                        )
                        .padding(horizontal = 28.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (tab.route == "inbox" && unreadCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = accent,
                                    contentColor = accentButtonLabelColor(),
                                ) {
                                    Text(
                                        if (unreadCount > 99) "99+" else "$unreadCount",
                                        fontSize = 9.sp,
                                    )
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (active) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                                tint = if (active) accent else sub,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (active) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                            tint = if (active) accent else sub,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        tab.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (active) accent else sub,
                    )
                    if (active) {
                        Box(
                            Modifier
                                .width(16.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(accent),
                        )
                    } else {
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}
