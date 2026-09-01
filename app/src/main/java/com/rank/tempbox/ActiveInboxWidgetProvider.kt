package com.rank.tempbox

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ActiveInboxWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Integrity.init(context)
        val prefs = context.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
        val activeSlot = prefs.getInt("active_inbox_slot", 1)
        val email = prefs.getString(PrefKeys.email(activeSlot), "No address")
        val messagesJson = prefs.getString("cached_messages", null)
        val unreadCount = if (messagesJson != null) {
            try {
                val orgJson = org.json.JSONArray(messagesJson)
                var count = 0
                for (i in 0 until orgJson.length()) {
                    if (!orgJson.getJSONObject(i).optBoolean("seen", false)) count++
                }
                count
            } catch (_: Exception) { 0 }
        } else 0

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_active_inbox)
            views.setTextViewText(R.id.widgetEmailAddress, email)
            views.setTextViewText(
                R.id.widgetUnreadBadge,
                if (unreadCount > 0) "$unreadCount unread" else "Active"
            )

            val copyIntent = Intent(context, ActiveInboxWidgetProvider::class.java).apply {
                action = ACTION_COPY_ADDRESS
                putExtra(EXTRA_EMAIL, email)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetCopyButton, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (ACTION_COPY_ADDRESS == intent.action) {
            val email = intent.getStringExtra(EXTRA_EMAIL) ?: return
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("email", email))
        }
    }

    companion object {
        private const val ACTION_COPY_ADDRESS = "com.rank.tempbox.COPY_ADDRESS"
        private const val EXTRA_EMAIL = "extra_email"
    }
}
