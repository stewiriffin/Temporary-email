package com.rank.tempbox.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.rank.tempbox.MessageDetail
import com.rank.tempbox.OtpExtractor
import com.rank.tempbox.RetrofitClient
import com.rank.tempbox.ads.StartIoBanner
import com.rank.tempbox.ui.theme.*
import android.content.ClipData
import android.content.ClipboardManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private val AVATAR_COLORS = listOf(
    0xFFFF6B6B, 0xFF4ECDC4, 0xFF45B7D1, 0xFFFFA726,
    0xFFBA68C8, 0xFF66BB6A, 0xFFF48FB1, 0xFF7986CB,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(
    messageId: String,
    token: String,
    isDark: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit = {},
) {
    val colors = TempBoxTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var detail by remember { mutableStateOf<MessageDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // SAF export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        result.data?.data?.let { uri ->
            detail?.let { writeEmailToUri(context, uri, it) }
        }
    }

    // Load message
    LaunchedEffect(messageId) {
        loading = true
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancelAll()
        } catch (_: Exception) {}
        try {
            detail = RetrofitClient.authenticatedApi.getMessage("Bearer $token", messageId)
        } catch (e: Exception) {
            error = e.message ?: "Failed to load"
        } finally {
            loading = false
        }
    }

    var otpCopied by remember { mutableStateOf(false) }
    val otp = detail?.let { OtpExtractor.extract(it.html?.joinToString(" "), it.text ?: it.intro) }

    Column(
        Modifier.fillMaxSize().background(colors.phoneBg),
    ) {
        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.card)
                .border(width = 0.5.dp, color = colors.cardBorder)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onBack)
                    .padding(8.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.sub,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Message", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.text)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        detail?.let { shareMessage(context, it) }
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text("Share", fontSize = 11.sp, color = colors.accent, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDelete)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text("Delete", fontSize = 11.sp, color = ExpiryRed, fontWeight = FontWeight.SemiBold)
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent, strokeWidth = 2.dp)
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error loading message", fontSize = 14.sp, color = colors.sub)
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, fontSize = 12.sp, color = colors.dim)
                        Spacer(Modifier.height(16.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(2.dp)).background(colors.accent)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onBack)
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                        ) {
                            Text("Go Back", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = accentButtonLabelColor())
                        }
                    }
                }
            }
            detail != null -> {
                val msg = detail!!
                Box(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(scrollState),
                    ) {
                        // Header
                        Column(Modifier.padding(20.dp)) {
                            // Avatar + sender
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val senderDisplay = msg.from?.name?.takeIf { it.isNotBlank() }
                                    ?: msg.from?.address.orEmpty().ifBlank { "Unknown sender" }
                                val initial = senderDisplay.firstOrNull { it.isLetter() } ?: '?'
                                val avatarColor = AVATAR_COLORS[initial.code % AVATAR_COLORS.size]
                                Box(
                                    Modifier.size(44.dp).clip(CircleShape)
                                        .background(ComposeColor(avatarColor)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(initial.uppercaseChar().toString(), fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold, color = ComposeColor.White)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(senderDisplay,
                                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
                                    if (!msg.from?.name.isNullOrBlank()) {
                                        Text(msg.from?.address.orEmpty(), fontSize = 12.sp, color = colors.sub)
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Subject
                            Text(msg.subject.orEmpty().ifBlank { "(No subject)" },
                                fontSize = 16.sp, fontWeight = FontWeight.W800, color = colors.text, lineHeight = 22.sp)

                            Spacer(Modifier.height(6.dp))
                            Text(formatDate(msg.createdAt.orEmpty()), fontSize = 12.sp, color = colors.dim)
                        }

                        HorizontalDivider(color = colors.cardBorder, thickness = 0.5.dp)

                        // Body
                        val htmlList = msg.html?.filter { it.isNotBlank() }
                        val hasHtml = !htmlList.isNullOrEmpty()

                        if (hasHtml) {
                            val htmlContent = htmlList!!.first()
                            // Verification link detection
                            val verifyRegex = Regex("""(?i)\bhref="([^"]*(?:verify|activate|confirm)[^"]*)""")
                            val verifyMatch = verifyRegex.find(htmlContent)
                            val verifyUrl = verifyMatch?.groupValues?.get(1)

                            if (verifyUrl != null) {
                                Box(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.accent.copy(alpha = 0.08f))
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(verifyUrl)))
                                        }
                                        .padding(12.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("\u2709", fontSize = 16.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Verification link detected",
                                            fontSize = 12.sp, color = colors.accent, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.weight(1f))
                                        Text("\u276F", fontSize = 12.sp, color = colors.accent)
                                    }
                                }
                            }

                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT,
                                        )
                                        settings.javaScriptEnabled = false
                                        settings.loadWithOverviewMode = true
                                        settings.useWideViewPort = true
                                        settings.allowFileAccess = false
                                        settings.allowContentAccess = false
                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        isVerticalScrollBarEnabled = false
                                        webViewClient = WebViewClient()
                                        val styled = buildStyledHtml(htmlContent, isDark)
                                        loadDataWithBaseURL(null, styled, "text/html", "UTF-8", null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                update = { webView ->
                                    val styled = buildStyledHtml(htmlContent, isDark)
                                    webView.loadDataWithBaseURL(null, styled, "text/html", "UTF-8", null)
                                },
                            )
                        } else {
                            val bodyText = msg.text?.ifBlank { null } ?: msg.intro?.ifBlank { null } ?: "(No content)"
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surfaceVariant)
                                    .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                            ) {
                                Text(
                                    bodyText,
                                    fontSize = 13.sp,
                                    color = colors.text,
                                    lineHeight = 20.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }

                        if (otp != null) {
                            Spacer(Modifier.height(12.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.accent.copy(alpha = 0.08f))
                                    .border(1.dp, colors.accent.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        "ONE-TIME CODE",
                                        fontSize = 10.sp,
                                        letterSpacing = 1.5.sp,
                                        color = colors.accent.copy(alpha = 0.7f),
                                    )
                                    Text(
                                        otp,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.accent,
                                        letterSpacing = 4.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.accent.copy(alpha = 0.1f))
                                        .border(1.dp, colors.accent.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) {
                                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clip.setPrimaryClip(ClipData.newPlainText("otp", otp))
                                            otpCopied = true
                                            scope.launch { kotlinx.coroutines.delay(2000); otpCopied = false }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        if (otpCopied) "Copied" else "Copy",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.accent,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(80.dp))
                    }

                    // FAB: scroll to top
                    if (scrollState.value > 400) {
                        Box(
                            Modifier.align(Alignment.BottomEnd).padding(16.dp).size(44.dp)
                                .clip(CircleShape)
                                .background(colors.accent)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    scope.launch { scrollState.animateScrollTo(0) }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("\u2191", fontSize = 18.sp, color = if (isDark) ComposeColor.Black else ComposeColor.White,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        }
        StartIoBanner()
    }
}

private fun buildStyledHtml(htmlContent: String, isDark: Boolean): String {
    val bgColor = if (isDark) "#080808" else "#F4F4F2"
    val textColor = if (isDark) "#E6E6E4" else "#0D0D0D"
    val linkColor = if (isDark) "#00FF88" else "#007A3E"

    var cleanHtml = htmlContent
    cleanHtml = cleanHtml.replace(Regex("(?i)color\\s*:\\s*[^;'\"]+"), "")
    cleanHtml = cleanHtml.replace(Regex("(?i)background-color\\s*:\\s*[^;'\"]+"), "")
    cleanHtml = cleanHtml.replace(Regex("(?i)background\\s*:\\s*[^;'\"]+"), "")

    return """<html><head><meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        * { max-width: 100% !important; box-sizing: border-box !important; }
        body { background-color: $bgColor !important; color: $textColor !important; font-family: sans-serif; font-size: 15px; line-height: 1.6; padding: 12px; margin: 0; }
        p, span, div, td, th, h1, h2, h3, h4, h5, h6 { color: inherit !important; background-color: transparent !important; }
        a { color: $linkColor !important; text-decoration: underline; }
        img { max-width: 100% !important; height: auto !important; border-radius: 8px; }
        table { max-width: 100% !important; width: 100% !important; background-color: transparent !important; border-collapse: collapse; }
    </style></head><body>$cleanHtml</body></html>"""
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT)
        val outputFmt = SimpleDateFormat("EEE, MMM d \u2022 HH:mm", Locale.ROOT)
        outputFmt.format(inputFmt.parse(dateString)!!)
    } catch (e: Exception) {
        dateString
    }
}

private fun shareMessage(context: Context, msg: MessageDetail) {
    val body = msg.text ?: msg.html?.firstOrNull() ?: ""
    val shareText = "Subject: ${msg.subject.orEmpty()}\n\n$body"
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
}

private fun writeEmailToUri(context: Context, uri: Uri, msg: MessageDetail) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            val body = msg.text ?: msg.html?.firstOrNull() ?: "(No content)"
            val content = """
                From: ${msg.from?.name.orEmpty()} <${msg.from?.address.orEmpty()}>
                Date: ${formatDate(msg.createdAt.orEmpty())}
                Subject: ${msg.subject.orEmpty()}
                
                --------------------------------------------------
                
                $body
            """.trimIndent()
            outputStream.write(content.toByteArray(Charsets.UTF_8))
        }
    } catch (_: Exception) {}
}
