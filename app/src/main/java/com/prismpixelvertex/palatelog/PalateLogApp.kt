package com.prismpixelvertex.palatelog

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

private const val TAG = "PalateLog_App"

@HiltAndroidApp
class PalateLogApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationManager: com.prismpixelvertex.palatelog.core.notification.PalateNotificationManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        setupMapplsSafetyExceptionHandler()

        try {
            notificationManager.initChannels()
        } catch (e: Exception) {
            Log.e(TAG, "Notification channel initialization failed", e)
        }

        try {
            com.mappls.sdk.maps.Mappls.getInstance(this)
            if (BuildConfig.MAPPLS_REST_KEY.isNotBlank()) {
                com.mappls.sdk.maps.Mappls.setApiKey(BuildConfig.MAPPLS_REST_KEY)
                Log.d(TAG, "Configured Mappls REST API key")
            }
            Log.d(TAG, "Mappls SDK initialized successfully")
            com.prismpixelvertex.palatelog.core.designsystem.MapProviderConfig.notifyMapplsInitialized(true)
        } catch (e: Exception) {
            Log.e(TAG, "Mappls SDK initialization failed", e)
            com.prismpixelvertex.palatelog.core.designsystem.MapProviderConfig.notifyMapplsInitialized(false)
        }
    }

    private fun setupMapplsSafetyExceptionHandler() {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isMapplsException(throwable)) {
                Log.e(TAG, "Caught unhandled Mappls SDK failure on thread '${thread.name}'. Gracefully engaging OSM fallback.", throwable)
                com.prismpixelvertex.palatelog.core.designsystem.MapProviderConfig.notifyMapplsInitialized(false)
                com.prismpixelvertex.palatelog.core.designsystem.MapProviderConfig.notifyFallbackEngaged()
            } else {
                originalHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun isMapplsException(throwable: Throwable?): Boolean {
        var current = throwable
        while (current != null) {
            if (current.stackTrace.any { it.className.startsWith("com.mappls.") }) {
                return true
            }
            if (current.message?.contains("Package Name / App Fingerprint", ignoreCase = true) == true) {
                return true
            }
            current = current.cause
        }
        return false
    }
}
