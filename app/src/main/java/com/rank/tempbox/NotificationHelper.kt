package com.rank.tempbox

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val emailChannel = NotificationChannel(
            "new_email_channel",
            "New Emails",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_email_desc)
        }

        val otpChannel = NotificationChannel(
            "otp_channel",
            "Verification Codes",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High-priority alerts for detected verification codes"
            enableVibration(true)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(emailChannel)
        manager.createNotificationChannel(otpChannel)
    }
}

fun showOtpNotification(context: Context, otp: String) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val copyIntent = Intent(context, CopyOtpReceiver::class.java).apply {
        putExtra(CopyOtpReceiver.EXTRA_OTP, otp)
    }
    val copyPendingIntent = PendingIntent.getBroadcast(
        context, 1, copyIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, "otp_channel")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(context.getString(R.string.otp_notification_title))
        .setContentText(context.getString(R.string.otp_notification_text, otp))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .addAction(android.R.drawable.ic_menu_edit, context.getString(R.string.otp_copy_action), copyPendingIntent)
        .build()

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(System.currentTimeMillis().toInt(), notification)
}
