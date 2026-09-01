package com.rank.tempbox

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rank.tempbox.ads.StartIoAds
import java.util.concurrent.TimeUnit

class TempBoxApplication : Application(), ViewModelStoreOwner {

    private val appViewModelStore = ViewModelStore()
    private val viewModelFactory by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(this)
    }

    val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        StartIoAds.init(this)
        createNotificationChannels(this)
        Integrity.init(this)
        scheduleMailboxCleanup()
        mainViewModel

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("TempBox", "Uncaught exception on thread: ${thread.name}", throwable)
            originalHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun scheduleMailboxCleanup() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<MailboxCleanupWorker>(
            4, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "mailbox_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }

    private val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
}
