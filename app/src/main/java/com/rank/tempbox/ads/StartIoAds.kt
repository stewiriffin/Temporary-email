package com.rank.tempbox.ads

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.rank.tempbox.BuildConfig
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.adlisteners.VideoListener

object StartIoAds {
    private const val TAG = "StartIoAds"
    const val APP_ID = "206653296"

    @Volatile
    private var initialized = false

    fun init(application: Application) {
        if (initialized) return
        StartAppSDK.initParams(application, APP_ID)
            .setCallback {
                initialized = true
                Log.d(TAG, "SDK ready")
            }
            .init()
        StartAppSDK.setTestAdsEnabled(BuildConfig.DEBUG)
    }
}

class InterstitialAdManager(activity: Activity) {

    companion object {
        /** Minimum time between two interstitials anywhere in the app. */
        private const val MIN_INTERVAL_MS = 90_000L

        /** Don't show an interstitial within the first minute after the process starts. */
        private const val MIN_SESSION_AGE_MS = 60_000L

        /** Require at least this many user actions between interstitials. */
        private const val MIN_ACTIONS_BETWEEN_SHOWS = 3

        private val sessionStartedAt = System.currentTimeMillis()
        private var lastShownAt = 0L
    }

    private val startAppAd = StartAppAd(activity)
    private var isLoading = false
    private var isReady = false
    private var actionCount = 0
    private var actionsSinceLastShow = 0

    fun load() {
        if (isLoading || isReady) return
        isLoading = true
        startAppAd.loadAd(object : AdEventListener {
            override fun onReceiveAd(ad: Ad) {
                isLoading = false
                isReady = true
            }

            override fun onFailedToReceiveAd(ad: Ad?) {
                isLoading = false
                isReady = false
            }
        })
    }

    fun showIfReady(onDismissed: (() -> Unit)? = null): Boolean {
        if (!canShow()) return false
        isReady = false
        lastShownAt = System.currentTimeMillis()
        startAppAd.showAd(object : AdDisplayListener {
            override fun adHidden(ad: Ad?) {
                onDismissed?.invoke()
                load()
            }

            override fun adDisplayed(ad: Ad?) = Unit
            override fun adClicked(ad: Ad?) = Unit
            override fun adNotDisplayed(ad: Ad?) {
                onDismissed?.invoke()
                load()
            }
        })
        return true
    }

    private fun canShow(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastShownAt < MIN_INTERVAL_MS) return false
        if (now - sessionStartedAt < MIN_SESSION_AGE_MS) return false
        if (!isReady) {
            load()
            return false
        }
        return true
    }

    /** Show after every [interval] user actions (new address / refresh), paced app-wide. */
    fun onUserAction(interval: Int = 3) {
        actionCount++
        actionsSinceLastShow++
        if (actionCount % interval != 0) return
        if (actionsSinceLastShow < MIN_ACTIONS_BETWEEN_SHOWS) return
        if (showIfReady()) {
            actionsSinceLastShow = 0
        }
    }
}

/** Rewarded video: watch to earn rewards (e.g. an extra daily address generation). */
class RewardedAdManager(activity: Activity) {
    private val rewardedAd = StartAppAd(activity)
    private var isLoading = false
    private var isReady = false

    fun load() {
        if (isLoading || isReady) return
        isLoading = true
        rewardedAd.loadAd(
            StartAppAd.AdMode.REWARDED_VIDEO,
            object : AdEventListener {
                override fun onReceiveAd(ad: Ad) {
                    isLoading = false
                    isReady = true
                }

                override fun onFailedToReceiveAd(ad: Ad?) {
                    isLoading = false
                    isReady = false
                }
            },
        )
    }

    /** Returns true if the ad was shown. [onRewarded] fires only if the user watches to the end. */
    fun show(onRewarded: () -> Unit, onDismissed: (() -> Unit)? = null): Boolean {
        if (!isReady) {
            load()
            return false
        }
        isReady = false
        rewardedAd.setVideoListener(object : VideoListener {
            override fun onVideoCompleted() {
                Handler(Looper.getMainLooper()).post { onRewarded() }
            }
        })
        rewardedAd.showAd(object : AdDisplayListener {
            override fun adHidden(ad: Ad?) {
                onDismissed?.invoke()
                load()
            }

            override fun adDisplayed(ad: Ad?) = Unit
            override fun adClicked(ad: Ad?) = Unit
            override fun adNotDisplayed(ad: Ad?) {
                onDismissed?.invoke()
                load()
            }
        })
        return true
    }
}

/** App wall: full-screen wall of offers / apps, shown on demand from Settings. */
class AppWallManager(activity: Activity) {
    private val wallAd = StartAppAd(activity)
    private var isLoading = false

    fun show() {
        if (isLoading) return
        isLoading = true
        wallAd.loadAd(
            StartAppAd.AdMode.OFFERWALL,
            object : AdEventListener {
                override fun onReceiveAd(ad: Ad) {
                    isLoading = false
                    Handler(Looper.getMainLooper()).post {
                        wallAd.showAd()
                        loadForNext()
                    }
                }

                override fun onFailedToReceiveAd(ad: Ad?) {
                    isLoading = false
                }
            },
        )
    }

    private fun loadForNext() {
        if (isLoading) return
        isLoading = true
        wallAd.loadAd(
            StartAppAd.AdMode.OFFERWALL,
            object : AdEventListener {
                override fun onReceiveAd(ad: Ad) {
                    isLoading = false
                }

                override fun onFailedToReceiveAd(ad: Ad?) {
                    isLoading = false
                }
            },
        )
    }
}
