package com.rank.tempbox

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MailboxCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
        Integrity.init(applicationContext)

        val slot = 1
        val createdAt = Integrity.getLong(PrefKeys.createdAt(slot), 0L)
        if (createdAt == 0L) return Result.success()

        val remaining = createdAt + 7L * 24 * 60 * 60 * 1000 - System.currentTimeMillis()
        if (remaining <= 0) {
            android.util.Log.w("TempBox-Worker", "Cleaning up expired slot $slot")
            prefs.edit()
                .remove(PrefKeys.email(slot))
                .remove(PrefKeys.password(slot))
                .remove(PrefKeys.token(slot))
                .apply()
            Integrity.putLong(PrefKeys.createdAt(slot), 0L)
        }
        return Result.success()
    }
}
