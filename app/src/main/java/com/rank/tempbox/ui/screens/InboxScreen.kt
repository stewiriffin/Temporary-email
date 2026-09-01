package com.rank.tempbox.ui.screens

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rank.tempbox.EmailMessage
import com.rank.tempbox.MainViewModel
import com.rank.tempbox.PrefKeys
import com.rank.tempbox.senderLabel
import com.rank.tempbox.ui.theme.AvatarColors
import com.rank.tempbox.ui.theme.TempBoxTheme

@Composable
fun InboxScreen(
    viewModel: MainViewModel,
    isDark: Boolean,
    onRefresh: () -> Unit = { viewModel.refreshInbox() },
) {
    val colors = TempBoxTheme.colors
    val context = LocalContext.current
    val messages by viewModel.messages.observeAsState(initial = emptyList())
    val emailAddress by viewModel.emailAddress.observeAsState(initial = "")
    val isLoading by viewModel.isLoading.observeAsState(false)
    var selectedMessageId by remember { mutableStateOf<String?>(null) }
    val pendingMessageId by viewModel.pendingOpenMessageId.observeAsState()

    LaunchedEffect(pendingMessageId) {
        pendingMessageId?.let { id ->
            selectedMessageId = id
            viewModel.clearPendingOpenMessage()
        }
    }

    val selectedMessage = messages.find { it.id == selectedMessageId }

    if (selectedMessage != null) {
        val prefs = context.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
        val slot = prefs.getInt("active_inbox_slot", 1)
        val token = prefs.getString(PrefKeys.token(slot), null) ?: prefs.getString("auth_token", null) ?: ""
        MessageDetailScreen(
            messageId = selectedMessage.id,
            token = token,
            isDark = isDark,
            onBack = { selectedMessageId = null },
            onDelete = {
                viewModel.deleteMessage(selectedMessage.id)
                selectedMessageId = null
            },
        )
    } else {
        Column(Modifier.fillMaxSize().background(colors.phoneBg)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(colors.card)
                    .border(width = 0.5.dp, color = colors.cardBorder)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    emailAddress.ifBlank { "Generating address…" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRefresh,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp,
                            color = colors.accent,
                        )
                    } else {
                        Icon(Icons.Rounded.Refresh, "Refresh", tint = colors.sub, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        if (isLoading) "Checking…" else "Refresh",
                        fontSize = 12.sp,
                        color = colors.sub,
                    )
                }
            }

            if (messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val glowAlpha by rememberInfiniteTransition(label = "empty").animateFloat(
                            0.04f, 0.12f,
                            infiniteRepeatable(tween(2800), RepeatMode.Reverse),
                            label = "glow",
                        )
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.surfaceVariant)
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Inbox,
                                contentDescription = null,
                                tint = colors.sub,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Inbox is empty", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.sub)
                        Text("Messages arrive automatically", fontSize = 12.sp, color = colors.sub.copy(alpha = 0.6f))
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(messages, key = { it.id }) { msg ->
                        EmailListItem(
                            message = msg,
                            colors = colors,
                            onClick = { selectedMessageId = msg.id },
                            onDelete = { viewModel.deleteMessage(msg.id) },
                        )
                    }
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${messages.size} message${if (messages.size != 1) "s" else ""}",
                                fontSize = 10.sp,
                                color = colors.dim,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmailListItem(
    message: EmailMessage,
    colors: com.rank.tempbox.ui.theme.TempBoxColors,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val senderName = message.senderLabel().ifBlank { "Unknown sender" }
    val initials = senderName.take(2).uppercase()
    val avatarColor = AvatarColors[senderName.hashCode().mod(AvatarColors.size).let { if (it < 0) -it else it }]

    val bgColor = when {
        !message.seen -> colors.accent.copy(alpha = 0.05f)
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(avatarColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(initials, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        senderName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (message.seen) colors.sub else colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        formatRelativeTime(message.createdAt),
                        fontSize = 10.sp,
                        color = colors.sub,
                    )
                }
                Text(
                    message.subject.orEmpty(),
                    fontSize = 12.sp,
                    color = if (message.seen) colors.sub.copy(alpha = 0.6f) else colors.text.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (!message.seen) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(colors.accent))
                        Spacer(Modifier.width(4.dp))
                        Text("Unread", fontSize = 10.sp, color = colors.accent, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Icon(
                Icons.Rounded.Delete,
                "Delete",
                tint = colors.sub,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDelete,
                    ),
            )
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.rowBorder))
    }
}

private fun formatRelativeTime(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    return createdAt.take(10)
}
