package com.rank.tempbox

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.google.gson.Gson

object WidgetUpdater {
    private val gson = Gson()

    fun updateFromMessages(context: Context, messages: List<EmailMessage>) {
        val prefs = context.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("cached_messages", gson.toJson(messages)).apply()

        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, ActiveInboxWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        val intent = Intent(context, ActiveInboxWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }
}
